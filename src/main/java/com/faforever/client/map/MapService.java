package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Vault;
import com.faforever.client.domain.api.Map;
import com.faforever.client.domain.api.MapType;
import com.faforever.client.domain.api.MapVersion;
import com.faforever.client.domain.api.MatchmakerQueueMapPool;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mapstruct.MapMapper;
import com.faforever.client.mapstruct.MatchmakerMapper;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.CompletableTask.Priority;
import com.faforever.client.task.TaskService;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.elide.ElideNavigator;
import com.faforever.commons.api.elide.ElideNavigatorOnCollection;
import com.faforever.commons.api.elide.ElideNavigatorOnId;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.faforever.client.util.LuaUtil.loadFile;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static com.google.common.net.UrlEscapers.urlFragmentEscaper;
import static java.lang.String.format;
import static java.nio.file.Files.list;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.stream.Collectors.toCollection;


@Slf4j
@Lazy
@Service
@RequiredArgsConstructor
public class MapService implements InitializingBean, DisposableBean {

  public static final String DEBUG = "debug";
  private static final String MAP_VERSION_REGEX = ".*[.v](\\d{4})$"; // Matches to an string like 'adaptive_twin_rivers.v0031'

  private final NotificationService notificationService;
  private final TaskService taskService;
  private final FafApiAccessor fafApiAccessor;
  private final AssetService assetService;
  private final I18n i18n;
  private final ThemeService themeService;
  private final MapGeneratorService mapGeneratorService;
  private final PlayerService playerService;
  private final MapMapper mapMapper;
  private final MatchmakerMapper matchmakerMapper;
  private final FileSizeReader fileSizeReader;
  private final ClientProperties clientProperties;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final Preferences preferences;
  private final ObjectFactory<MapUploadTask> mapUploadTaskFactory;
  private final ObjectFactory<DownloadMapTask> downloadMapTaskFactory;
  private final ObjectFactory<UninstallMapTask> uninstallMapTaskFactory;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObservableMap<String, MapVersion> mapsByFolderName = FXCollections.observableHashMap();
  @Getter
  private final ObservableList<MapVersion> installedMaps = JavaFxUtil.attachListToMap(
      FXCollections.synchronizedObservableList(FXCollections.observableArrayList()), mapsByFolderName);
  private final InvalidationListener mapsDirectoryInvalidationListener = observable -> tryLoadMaps();
  private String mapDownloadUrlFormat;
  private String mapPreviewUrlFormat;
  @VisibleForTesting
  Set<String> officialMaps = Set.of("SCMP_001", "SCMP_002", "SCMP_003", "SCMP_004", "SCMP_005", "SCMP_006", "SCMP_007",
                                    "SCMP_008", "SCMP_009", "SCMP_010", "SCMP_011", "SCMP_012", "SCMP_013", "SCMP_014",
                                    "SCMP_015", "SCMP_016", "SCMP_017", "SCMP_018", "SCMP_019", "SCMP_020", "SCMP_021",
                                    "SCMP_022", "SCMP_023", "SCMP_024", "SCMP_025", "SCMP_026", "SCMP_027", "SCMP_028",
                                    "SCMP_029", "SCMP_030", "SCMP_031", "SCMP_032", "SCMP_033", "SCMP_034", "SCMP_035",
                                    "SCMP_036", "SCMP_037", "SCMP_038", "SCMP_039", "SCMP_040", "X1MP_001", "X1MP_002",
                                    "X1MP_003", "X1MP_004", "X1MP_005", "X1MP_006", "X1MP_007", "X1MP_008", "X1MP_009",
                                    "X1MP_010", "X1MP_011", "X1MP_012", "X1MP_014", "X1MP_017");
  private Thread directoryWatcherThread;

  private static URL getDownloadUrl(String mapName, String baseUrl) throws MalformedURLException {
    return new URL(format(baseUrl, urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US)));
  }

  private static URL getPreviewUrl(String mapName, String baseUrl,
                                   PreviewSize previewSize) throws MalformedURLException {
    return new URL(
        format(baseUrl, previewSize.folderName, urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US)));
  }

  @Override
  public void afterPropertiesSet() {
    Vault vault = clientProperties.getVault();
    mapDownloadUrlFormat = vault.getMapDownloadUrlFormat();
    mapPreviewUrlFormat = vault.getMapPreviewUrlFormat();
    WeakInvalidationListener weakDirectoryListener = new WeakInvalidationListener(mapsDirectoryInvalidationListener);
    JavaFxUtil.addListener(forgedAlliancePrefs.installationPathProperty(), weakDirectoryListener);
    JavaFxUtil.addAndTriggerListener(forgedAlliancePrefs.vaultBaseDirectoryProperty(), weakDirectoryListener);
  }

  private void tryLoadMaps() {
    if (forgedAlliancePrefs.getInstallationPath() == null) {
      log.warn("Could not load maps: installation path is not set");
      return;
    }

    Path mapsDirectory = forgedAlliancePrefs.getMapsDirectory();
    if (mapsDirectory == null) {
      log.warn("Could not load maps: custom map directory is not set");
      return;
    }

    try {
      Files.createDirectories(mapsDirectory);
      Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
      directoryWatcherThread = startDirectoryWatcher(mapsDirectory);
    } catch (IOException e) {
      log.warn("Could not start map directory watcher", e);
    }

    mapsByFolderName.clear();
    loadInstalledMaps();
  }

  private Thread startDirectoryWatcher(Path mapsDirectory) {
    Thread thread = new Thread(() -> {
      try (WatchService watcher = mapsDirectory.getFileSystem().newWatchService()) {
        forgedAlliancePrefs.getMapsDirectory().register(watcher, ENTRY_DELETE, ENTRY_CREATE);
        while (!Thread.interrupted()) {
          WatchKey key = watcher.take();
          key.pollEvents()
             .stream()
             .filter(event -> event.kind() == ENTRY_DELETE || event.kind() == ENTRY_CREATE)
             .forEach(event -> {
               Path mapPath = mapsDirectory.resolve((Path) event.context());
               if (event.kind() == ENTRY_DELETE) {
                 removeMap(mapPath);
               } else if (event.kind() == ENTRY_CREATE) {
                 Mono.just(mapPath)
                     .filter(Files::exists)
                     .doOnNext(this::addInstalledMap)
                     .retryWhen(Retry.fixedDelay(30, Duration.ofSeconds(1)).filter(MapLoadException.class::isInstance))
                     .subscribe(null, throwable -> log.error("Map could not be read: `{}`", mapPath, throwable));
               }
             });
          key.reset();
        }
      } catch (IOException e) {
        log.warn("Could not start maps directory watcher for `{}`", mapsDirectory);
      } catch (InterruptedException e) {
        log.info("Watcher terminated ({})", e.getMessage());
      }
    });
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  private void loadInstalledMaps() {
    taskService.submitTask(new CompletableTask<Void>(Priority.LOW) {

      @Override
      protected Void call() {
        updateTitle(i18n.get("mapVault.loadingMaps"));
        Path officialMapsPath = forgedAlliancePrefs.getInstallationPath().resolve("maps");
        try (Stream<Path> customMapsDirectoryStream = list(forgedAlliancePrefs.getMapsDirectory())) {
          List<Path> mapPaths = new ArrayList<>();
          customMapsDirectoryStream.collect(toCollection(() -> mapPaths));
          officialMaps.stream().map(officialMapsPath::resolve).collect(toCollection(() -> mapPaths));

          long totalMaps = mapPaths.size();
          long mapsRead = 0;
          for (Path mapPath : mapPaths) {
            if (mapPath.getFileName().toString().equals(DEBUG)) {
              continue;
            }
            updateProgress(++mapsRead, totalMaps);
            try {
              addInstalledMap(mapPath);
            } catch (MapLoadException exception) {
              log.error("Map could not be read: `{}`", mapPath, exception);
            }
          }
        } catch (IOException e) {
          log.error("Maps could not be read from: `{}`", forgedAlliancePrefs.getMapsDirectory(), e);
        }
        return null;
      }
    });
  }

  private void removeMap(Path mapFolder) {
    mapsByFolderName.remove(mapFolder.getFileName().toString().toLowerCase(Locale.ROOT));
  }

  private void addInstalledMap(Path mapFolder) throws MapLoadException {
    MapVersion mapVersion = readMap(mapFolder);
    if (!isInstalled(mapVersion.folderName())) {
      fxApplicationThreadExecutor.execute(
          () -> mapsByFolderName.put(mapVersion.folderName().toLowerCase(Locale.ROOT), mapVersion));
      log.debug("Added map from {}", mapFolder);
    }
  }

  @NotNull
  public MapVersion readMap(Path mapFolder) throws MapLoadException {
    if (!Files.isDirectory(mapFolder)) {
      throw new MapLoadException("Not a folder: " + mapFolder.toAbsolutePath(), null, "map.load.notAFolder",
                                 mapFolder.toAbsolutePath());
    }

    try (Stream<Path> mapFolderFilesStream = list(mapFolder)) {
      Path scenarioLuaPath = mapFolderFilesStream.filter(
                                                     file -> file.getFileName().toString().endsWith("_scenario.lua"))
                                                 .findFirst()
                                                 .orElseThrow(() -> new MapLoadException(
                                                     "Map folder does not contain a *_scenario.lua: " + mapFolder.toAbsolutePath(),
                                                     null, "map.load.noScenario", mapFolder.toAbsolutePath()));

      LuaValue luaRoot = loadFile(scenarioLuaPath);
      LuaValue scenarioInfo = luaRoot.get("ScenarioInfo");
      LuaValue size = scenarioInfo.get("size");

      Map map = new Map(null, scenarioInfo.get("name").toString(), 0, null, false,
                        MapType.fromValue(scenarioInfo.get("type").toString()), null);
      String folderName = mapFolder.getFileName().toString();
      String description = FaStrings.removeLocalizationTag(scenarioInfo.get("description").toString());
      MapSize mapSize = new MapSize(size.get(1).toint(), size.get(2).toint());
      int maxPlayers = scenarioInfo.get("Configurations").get("standard").get("teams").get(1).get("armies").length();

      ComparableVersion comparableVersion = null;
      LuaValue version = scenarioInfo.get("map_version");
      if (!version.isnil()) {
        comparableVersion = new ComparableVersion(version.toString());
      }

      return new MapVersion(null, folderName, 0, description, maxPlayers, mapSize, comparableVersion, false, false,
                            null, null, null, map, null);
    } catch (IOException e) {
      throw new MapLoadException("Could not load map due to IO error" + mapFolder.toAbsolutePath(), e,
                                 "map.load.ioError", mapFolder.toAbsolutePath());
    } catch (LuaError e) {
      throw new MapLoadException("Could not load map due to lua error" + mapFolder.toAbsolutePath(), e,
                                 "map.load.luaError", mapFolder.toAbsolutePath());
    }
  }

  @Cacheable(value = CacheNames.MAP_PREVIEW, unless = "#result.equals(@mapService.getGeneratedMapPreviewImage())")
  public Image loadPreview(String mapName, PreviewSize previewSize) {
    if (mapGeneratorService.isGeneratedMap(mapName)) {
      return getGeneratedMapPreview(mapName);
    }

    try {
      return loadPreview(getPreviewUrl(mapName, mapPreviewUrlFormat, previewSize), previewSize);
    } catch (MalformedURLException e) {
      log.warn("Could not create url from {}", mapName, e);
      return themeService.getThemeImage(ThemeService.NO_IMAGE_AVAILABLE);
    }
  }

  private Image getGeneratedMapPreview(String mapName) {
    Path previewPath = forgedAlliancePrefs.getMapsDirectory().resolve(mapName).resolve(mapName + "_preview.png");
    if (Files.exists(previewPath)) {
      try (InputStream inputStream = Files.newInputStream(previewPath)) {
        return new Image(inputStream);
      } catch (IOException e) {
        log.warn("Could not load image from {}", previewPath, e);
      }
    }

    return getGeneratedMapPreviewImage();
  }

  public Image getGeneratedMapPreviewImage() {
    return themeService.getThemeImage(ThemeService.GENERATED_MAP_IMAGE);
  }

  public Optional<MapVersion> getMapLocallyFromName(String mapFolderName) {
    log.trace("Looking for map '{}' locally", mapFolderName);
    return Optional.ofNullable(mapsByFolderName.get(mapFolderName.toLowerCase(Locale.ROOT)));
  }

  public boolean isOfficialMap(String mapName) {
    return officialMaps.stream().anyMatch(name -> name.equalsIgnoreCase(mapName));
  }

  public boolean isOfficialMap(MapVersion mapVersion) {
    return mapVersion != null && isOfficialMap(mapVersion.folderName());
  }

  public boolean isCustomMap(MapVersion mapVersion) {
    return !isOfficialMap(mapVersion);
  }

  /**
   * Returns {@code true} if the given map is available locally, {@code false} otherwise.
   */
  public boolean isInstalled(String mapFolderName) {
    return mapsByFolderName.containsKey(mapFolderName.toLowerCase(Locale.ROOT));
  }

  public boolean isInstalled(MapVersion mapVersion) {
    return mapVersion != null && isInstalled(mapVersion.folderName());
  }

  public BooleanExpression isInstalledBinding(ObservableValue<MapVersion> mapVersionObservable) {
    return Bindings.createBooleanBinding(() -> isInstalled(mapVersionObservable.getValue()), mapVersionObservable,
                                         installedMaps);
  }

  public BooleanExpression isInstalledBinding(MapVersion mapVersion) {
    return Bindings.createBooleanBinding(() -> isInstalled(mapVersion), installedMaps);
  }

  public BooleanExpression isInstalledBinding(String mapFolderName) {
    return Bindings.createBooleanBinding(() -> isInstalled(mapFolderName), installedMaps);
  }

  public Mono<String> generateIfNotInstalled(String mapName) {
    if (isInstalled(mapName)) {
      return Mono.just(mapName);
    }
    return mapGeneratorService.generateMap(mapName);
  }

  public Mono<Void> downloadIfNecessary(String technicalMapName) {
    if (isInstalled(technicalMapName)) {
      return Mono.empty();
    }
    try {
      URL mapUrl = getDownloadUrl(technicalMapName, mapDownloadUrlFormat);
      return downloadAndInstallMap(technicalMapName, mapUrl, null, null);
    } catch (MalformedURLException e) {
      throw new AssetLoadException("Could not download map", e, "map.download.error", technicalMapName);
    }
  }


  public Mono<Void> downloadAndInstallMap(MapVersion mapVersion, @Nullable DoubleProperty progressProperty,
                                          @Nullable StringProperty titleProperty) {
    return downloadAndInstallMap(mapVersion.folderName(), mapVersion.downloadUrl(), progressProperty, titleProperty);
  }

  /**
   * Loads the preview of a map or returns a "unknown map" image.
   */

  @Cacheable(value = CacheNames.MAP_PREVIEW, unless = "#result.equals(@mapService.getGeneratedMapPreviewImage())")
  public Image loadPreview(MapVersion mapVersion, PreviewSize previewSize) {
    URL url = switch (previewSize) {
      case SMALL -> mapVersion.thumbnailUrlSmall();
      case LARGE -> mapVersion.thumbnailUrlLarge();
    };

    if (url != null) {
      return loadPreview(url, previewSize);
    } else {
      return loadPreview(mapVersion.folderName(), previewSize);
    }
  }

  private Image loadPreview(URL url, PreviewSize previewSize) {
    return assetService.loadAndCacheImage(url, Path.of("maps").resolve(previewSize.folderName),
                                          () -> themeService.getThemeImage(ThemeService.NO_IMAGE_AVAILABLE));
  }


  public Mono<Void> uninstallMap(MapVersion mapVersion) {
    if (isOfficialMap(mapVersion.folderName())) {
      throw new IllegalArgumentException("Attempt to uninstall an official map");
    }
    UninstallMapTask task = uninstallMapTaskFactory.getObject();
    task.setMap(mapVersion);
    return taskService.submitTask(task).getMono();
  }


  public Path getPathForMap(MapVersion mapVersion) {
    return getPathForMapCaseInsensitive(mapVersion.folderName());
  }

  private Path getMapsDirectory(String technicalName) {
    if (isOfficialMap(technicalName)) {
      return forgedAlliancePrefs.getInstallationPath().resolve("maps");
    }
    return forgedAlliancePrefs.getMapsDirectory();
  }

  public Path getPathForMapCaseInsensitive(String approxName) {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(getMapsDirectory(approxName))) {
      for (Path entry : directoryStream) {
        if (entry.getFileName().toString().equalsIgnoreCase(approxName)) {
          return entry;
        }
      }
    } catch (IOException e) {
      throw new AssetLoadException("Could not open maps directory", e, "map.directory.couldNotOpen");
    }
    return null;
  }

  public CompletableTask<Void> uploadMap(Path mapPath, boolean ranked) {
    MapUploadTask mapUploadTask = mapUploadTaskFactory.getObject();
    mapUploadTask.setMapPath(mapPath);
    mapUploadTask.setRanked(ranked);

    return taskService.submitTask(mapUploadTask);
  }

  @CacheEvict(CacheNames.MAPS)
  public void evictCache() {
    // Nothing to see here
  }

  private boolean containsVersionControl(String mapFolderName) {
    return Pattern.matches(MAP_VERSION_REGEX, mapFolderName);
  }

  public Mono<MapVersion> updateLatestVersionIfNecessary(MapVersion mapVersion) {
    if (isOfficialMap(mapVersion) || !preferences.isMapAndModAutoUpdate()) {
      return Mono.just(mapVersion);
    }
    return getMapLatestVersion(mapVersion).flatMap(latestMap -> {
      Mono<Void> downloadFuture;
      if (!isInstalled(latestMap.folderName())) {
        downloadFuture = downloadAndInstallMap(latestMap, null, null);
      } else {
        downloadFuture = Mono.empty();
      }
      return downloadFuture.thenReturn(latestMap);
    }).flatMap(latestMap -> {
      Mono<Void> uninstallFuture;
      if (!latestMap.folderName().equals(mapVersion.folderName())) {
        uninstallFuture = uninstallMap(mapVersion);
      } else {
        uninstallFuture = Mono.empty();
      }
      return uninstallFuture.thenReturn(latestMap);
    });
  }

  public CompletableFuture<Integer> getFileSize(MapVersion mapVersion) {
    return fileSizeReader.getFileSize(mapVersion.downloadUrl());
  }

  private Mono<Void> downloadAndInstallMap(String folderName, URL downloadUrl,
                                           @Nullable DoubleProperty progressProperty,
                                           @Nullable StringProperty titleProperty) {
    if (mapGeneratorService.isGeneratedMap(folderName)) {
      return generateIfNotInstalled(folderName).then();
    }

    if (isInstalled(folderName)) {
      log.info("Map '{}' exists locally already. Download is not required", folderName);
      return Mono.empty();
    }

    DownloadMapTask task = downloadMapTaskFactory.getObject();
    task.setMapUrl(downloadUrl);
    task.setFolderName(folderName);

    if (progressProperty != null) {
      progressProperty.bind(task.progressProperty());
    }
    if (titleProperty != null) {
      titleProperty.bind(task.titleProperty());
    }

    return taskService.submitTask(task).getMono();
  }

  @Override
  public void destroy() {
    Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
  }

  public String convertMapFolderNameToHumanNameIfPossible(String mapFolderName) {
    // dualgap_adaptive.v0012 -> dualgap adaptive
    return mapFolderName.replace("_", " ").replaceAll(".v\\d+", "");
  }

  public enum PreviewSize {
    // These must match the preview URLs
    SMALL("small"), LARGE("large");

    final String folderName;

    PreviewSize(String folderName) {
      this.folderName = folderName;
    }
  }

  public Mono<MapVersion> hideMapVersion(MapVersion map) {
    String id = String.valueOf(map.id());
    com.faforever.commons.api.dto.MapVersion mapVersion = new com.faforever.commons.api.dto.MapVersion();
    mapVersion.setHidden(true);
    mapVersion.setId(id);
    ElideNavigatorOnId<com.faforever.commons.api.dto.MapVersion> navigator = ElideNavigator.of(mapVersion);
    return fafApiAccessor.patch(navigator, mapVersion).then(fafApiAccessor.getOne(navigator)).map(mapMapper::map);
  }

  /**
   * Tries to find a map my its folder name, first locally then on the server.
   */
  public Mono<MapVersion> findByMapFolderName(String folderName) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.MapVersion> navigator = ElideNavigator.of(
                                                                                                       com.faforever.commons.api.dto.MapVersion.class)
                                                                                                   .collection()
                                                                                                   .setFilter(
                                                                                                       qBuilder().string(
                                                                                                                     "folderName")
                                                                                                                 .eq(folderName));
    Mono<MapVersion> apiMapVersion = fafApiAccessor.getMany(navigator).next().map(mapMapper::map);

    return Mono.justOrEmpty(getMapLocallyFromName(folderName)).switchIfEmpty(apiMapVersion);
  }

  @VisibleForTesting
  Mono<MapVersion> getMapLatestVersion(MapVersion mapVersion) {
    String folderName = mapVersion.folderName();

    if (!containsVersionControl(folderName)) {
      return Mono.just(mapVersion);
    }

    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator = ElideNavigator.of(
                                                                                                com.faforever.commons.api.dto.Map.class)
                                                                                            .collection()
                                                                                            .setFilter(
                                                                                                qBuilder().string(
                                                                                                              "versions.folderName")
                                                                                                          .eq(folderName))
                                                                                            .pageSize(1);
    return fafApiAccessor.getMany(navigator)
                         .next()
                         .map(com.faforever.commons.api.dto.Map::getLatestVersion)
                         .map(mapMapper::map)
                         .defaultIfEmpty(mapVersion);

  }

  public Mono<Void> downloadAllMatchmakerMaps(MatchmakerQueueInfo matchmakerQueue) {
    ElideNavigatorOnCollection<MapPoolAssignment> navigator = ElideNavigator.of(MapPoolAssignment.class)
                                                                            .collection()
                                                                            .setFilter(qBuilder().intNum(
                                                                                                     "mapPool.matchmakerQueueMapPool.matchmakerQueue.id")
                                                                                                 .eq(matchmakerQueue.getId()));
    return fafApiAccessor.getMany(navigator).map(mapMapper::mapFromPoolAssignment)
                         .distinct()
                         .filter(mapVersion -> !mapGeneratorService.isGeneratedMap(mapVersion.folderName()))
                         .flatMap(
                             mapVersion -> downloadAndInstallMap(mapVersion, null, null).onErrorResume(throwable -> {
                               log.warn("Unable to download map `{}`", mapVersion.folderName(), throwable);
                               notificationService.addPersistentErrorNotification("map.download.error",
                                                                                  mapVersion.folderName());
                               return Mono.empty();
                             }))
                         .then();
  }

  @Cacheable(value = CacheNames.MATCHMAKER_POOLS, sync = true)
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Mono <java.util.Map<MatchmakerQueueMapPool, List<MapVersion>>> getMatchmakerBrackets(MatchmakerQueueInfo matchmakerQueue) {
    ElideNavigatorOnCollection<MapPoolAssignment> navigator = ElideNavigator.of(MapPoolAssignment.class).collection();
    List<Condition<?>> conditions = new ArrayList<>();
    conditions.add(qBuilder().intNum("mapPool.matchmakerQueueMapPool.matchmakerQueue.id").eq(matchmakerQueue.getId()));

    String customFilter = ((String) new QBuilder().and(conditions).query(new RSQLVisitor()));

    return fafApiAccessor.getMany(navigator, customFilter)
                         .map(matchmakerMapper::map)
                         .collect(Collectors.groupingBy(assignment -> assignment.mapPool().mapPool(),
                                                        Collectors.mapping(assignment -> assignment.mapVersion(), Collectors.toList())));

  }

  public Mono<Boolean> hasPlayedMap(PlayerInfo player, MapVersion mapVersion) {
    ElideNavigatorOnCollection<Game> navigator = ElideNavigator.of(Game.class)
                                                               .collection()
                                                               .setFilter(qBuilder().intNum("mapVersion.id")
                                                                                    .eq(mapVersion.id())
                                                                                    .and()
                                                                                    .intNum("playerStats.player.id")
                                                                                    .eq(player.getId()))
                                                               .addSortingRule("endTime", false)
                                                               .pageSize(1);
    return fafApiAccessor.getMany(navigator).hasElements();
  }

  public Mono<Tuple2<List<MapVersion>, Integer>> getOwnedMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.MapVersion> navigator = ElideNavigator.of(
                                                                                                       com.faforever.commons.api.dto.MapVersion.class)
                                                                                                   .collection()
                                                                                                   .setFilter(
                                                                                                       qBuilder().string(
                                                                                                                     "map.author.id")
                                                                                                                 .eq(String.valueOf(
                                                                                                                     playerService.getCurrentPlayer()
                                                                                                                                  .getId())))
                                                                                                   .pageNumber(page)
                                                                                                   .pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator).map(tuple -> tuple.mapT1(mapMapper::mapVersionDtos));
  }

  @Cacheable(value = CacheNames.MAPS, sync = true)
  public Mono<Tuple2<List<MapVersion>, Integer>> findByQueryWithPageCount(SearchConfig searchConfig, int count,
                                                                          int page) {
    SortConfig sortConfig = searchConfig.sortConfig();
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator = ElideNavigator.of(
                                                                                                com.faforever.commons.api.dto.Map.class)
                                                                                            .collection()
                                                                                            .addSortingRule(
                                                                                                sortConfig.sortProperty(),
                                                                                                sortConfig.sortOrder()
                                                                                                          .equals(
                                                                                                              SortOrder.ASC));
    return getMapPage(navigator, searchConfig.searchQuery(), count, page);
  }

  public Mono<Integer> getRecommendedMapPageCount(int count) {
    return getRecommendedMapsWithPageCount(count, 1).map(Tuple2::getT2);
  }

  public Mono<Tuple2<List<MapVersion>, Integer>> getRecommendedMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.Map.class).collection().setFilter(qBuilder().bool("recommended").isTrue());
    return getMapPage(navigator, count, page);
  }

  public Mono<Tuple2<List<MapVersion>, Integer>> getHighestRatedMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.Map.class).collection().addSortingRule("reviewsSummary.lowerBound", false);
    return getMapPage(navigator, count, page);
  }

  public Mono<Tuple2<List<MapVersion>, Integer>> getNewestMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.Map.class).collection().addSortingRule("latestVersion.createTime", false);
    return getMapPage(navigator, count, page);
  }

  public Mono<Tuple2<List<MapVersion>, Integer>> getMostPlayedMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator = ElideNavigator.of(
        com.faforever.commons.api.dto.Map.class).collection().addSortingRule("gamesPlayed", false);
    return getMapPage(navigator, count, page);
  }

  private Mono<Tuple2<List<MapVersion>, Integer>> getMapPage(
      ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator, int count, int page) {
    return getMapPage(navigator, "", count, page);
  }

  private Mono<Tuple2<List<MapVersion>, Integer>> getMapPage(
      ElideNavigatorOnCollection<com.faforever.commons.api.dto.Map> navigator, String customFilter, int count,
      int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator, customFilter)
                         .map(tuple -> tuple.mapT1(maps -> maps.stream()
                                                               .map(com.faforever.commons.api.dto.Map::getLatestVersion)
                                                               .map(mapMapper::map)
                                                               .toList()));
  }
}
