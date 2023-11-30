package com.faforever.client.map;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Vault;
import com.faforever.client.domain.MapBean;
import com.faforever.client.domain.MapBean.MapType;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.MapMapper;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.remote.AssetService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.CompletableTask.Priority;
import com.faforever.client.task.TaskService;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.FileSizeReader;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapVersion;
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
import org.springframework.scheduling.annotation.Async;
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
import java.util.stream.Stream;

import static com.faforever.client.util.LuaUtil.loadFile;
import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static com.google.common.net.UrlEscapers.urlFragmentEscaper;
import static java.lang.String.format;
import static java.nio.file.Files.list;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;


@Slf4j
@Lazy
@Service
@RequiredArgsConstructor
public class MapService implements InitializingBean, DisposableBean {

  public static final String DEBUG = "debug";
  private static final String MAP_VERSION_REGEX = ".*[.v](?<version>\\d{4})$"; // Matches to an string like 'adaptive_twin_rivers.v0031'

  private final NotificationService notificationService;
  private final TaskService taskService;
  private final FafApiAccessor fafApiAccessor;
  private final AssetService assetService;
  private final I18n i18n;
  private final UiService uiService;
  private final ThemeService themeService;
  private final MapGeneratorService mapGeneratorService;
  private final PlayerService playerService;
  private final MapMapper mapMapper;
  private final FileSizeReader fileSizeReader;
  private final ClientProperties clientProperties;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final Preferences preferences;
  private final ObjectFactory<MapUploadTask> mapUploadTaskFactory;
  private final ObjectFactory<DownloadMapTask> downloadMapTaskFactory;
  private final ObjectFactory<UninstallMapTask> uninstallMapTaskFactory;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final ObservableMap<String, MapVersionBean> mapsByFolderName = FXCollections.observableHashMap();
  @Getter
  private final ObservableList<MapVersionBean> installedMaps = JavaFxUtil.attachListToMap(FXCollections.synchronizedObservableList(FXCollections.observableArrayList()), mapsByFolderName);
  private final InvalidationListener mapsDirectoryInvalidationListener = observable -> tryLoadMaps();
  private String mapDownloadUrlFormat;
  private String mapPreviewUrlFormat;
  @VisibleForTesting
  Set<String> officialMaps = Set.of(
      "SCMP_001", "SCMP_002", "SCMP_003", "SCMP_004", "SCMP_005", "SCMP_006", "SCMP_007", "SCMP_008", "SCMP_009", "SCMP_010", "SCMP_011",
      "SCMP_012", "SCMP_013", "SCMP_014", "SCMP_015", "SCMP_016", "SCMP_017", "SCMP_018", "SCMP_019", "SCMP_020", "SCMP_021", "SCMP_022",
      "SCMP_023", "SCMP_024", "SCMP_025", "SCMP_026", "SCMP_027", "SCMP_028", "SCMP_029", "SCMP_030", "SCMP_031", "SCMP_032", "SCMP_033",
      "SCMP_034", "SCMP_035", "SCMP_036", "SCMP_037", "SCMP_038", "SCMP_039", "SCMP_040", "X1MP_001", "X1MP_002", "X1MP_003", "X1MP_004",
      "X1MP_005", "X1MP_006", "X1MP_007", "X1MP_008", "X1MP_009", "X1MP_010", "X1MP_011", "X1MP_012", "X1MP_014", "X1MP_017"
  );
  private Thread directoryWatcherThread;

  private static URL getDownloadUrl(String mapName, String baseUrl) throws MalformedURLException {
    return new URL(format(baseUrl, urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US)));
  }

  private static URL getPreviewUrl(String mapName, String baseUrl,
                                   PreviewSize previewSize) throws MalformedURLException {
    return new URL(format(baseUrl, previewSize.folderName, urlFragmentEscaper().escape(mapName)
        .toLowerCase(Locale.US)));
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
          key.pollEvents().stream()
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
          officialMaps.stream()
              .map(officialMapsPath::resolve)
              .collect(toCollection(() -> mapPaths));

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
    MapVersionBean mapVersion = readMap(mapFolder);
    if (!isInstalled(mapVersion.getFolderName())) {
      fxApplicationThreadExecutor.execute(() -> mapsByFolderName.put(mapVersion.getFolderName()
          .toLowerCase(Locale.ROOT), mapVersion));
      log.debug("Added map from {}", mapFolder);
    }
  }

  @NotNull
  public MapVersionBean readMap(Path mapFolder) throws MapLoadException {
    if (!Files.isDirectory(mapFolder)) {
      throw new MapLoadException("Not a folder: " + mapFolder.toAbsolutePath(), null, "map.load.notAFolder", mapFolder.toAbsolutePath());
    }

    try (Stream<Path> mapFolderFilesStream = list(mapFolder)) {
      Path scenarioLuaPath = mapFolderFilesStream
          .filter(file -> file.getFileName().toString().endsWith("_scenario.lua"))
          .findFirst()
          .orElseThrow(() -> new MapLoadException("Map folder does not contain a *_scenario.lua: " + mapFolder.toAbsolutePath(), null, "map.load.noScenario", mapFolder.toAbsolutePath()));

      LuaValue luaRoot = loadFile(scenarioLuaPath);
      LuaValue scenarioInfo = luaRoot.get("ScenarioInfo");
      LuaValue size = scenarioInfo.get("size");

      MapVersionBean mapVersion = new MapVersionBean();
      MapBean map = new MapBean();
      mapVersion.setFolderName(mapFolder.getFileName().toString());
      map.setDisplayName(scenarioInfo.get("name").toString());
      mapVersion.setDescription(FaStrings.removeLocalizationTag(scenarioInfo.get("description").toString()));
      map.setMapType(MapType.fromString(scenarioInfo.get("type").toString()));
      mapVersion.setSize(MapSize.valueOf(size.get(1).toint(), size.get(2).toint()));
      mapVersion.setMaxPlayers(scenarioInfo.get("Configurations")
          .get("standard")
          .get("teams")
          .get(1)
          .get("armies")
          .length());
      mapVersion.setMap(map);

      LuaValue version = scenarioInfo.get("map_version");
      if (!version.isnil()) {
        mapVersion.setVersion(new ComparableVersion(version.toString()));
      }

      return mapVersion;
    } catch (IOException e) {
      throw new MapLoadException("Could not load map due to IO error" + mapFolder.toAbsolutePath(), e, "map.load.ioError", mapFolder.toAbsolutePath());
    } catch (LuaError e) {
      throw new MapLoadException("Could not load map due to lua error" + mapFolder.toAbsolutePath(), e, "map.load.luaError", mapFolder.toAbsolutePath());
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

  public Optional<MapVersionBean> getMapLocallyFromName(String mapFolderName) {
    log.trace("Looking for map '{}' locally", mapFolderName);
    return Optional.ofNullable(mapsByFolderName.get(mapFolderName.toLowerCase(Locale.ROOT)));
  }

  public boolean isOfficialMap(String mapName) {
    return officialMaps.stream().anyMatch(name -> name.equalsIgnoreCase(mapName));
  }

  public boolean isOfficialMap(MapVersionBean mapVersion) {
    return mapVersion != null && isOfficialMap(mapVersion.getFolderName());
  }

  public boolean isCustomMap(MapVersionBean mapVersion) {
    return !isOfficialMap(mapVersion);
  }

  /**
   * Returns {@code true} if the given map is available locally, {@code false} otherwise.
   */
  public boolean isInstalled(String mapFolderName) {
    return mapsByFolderName.containsKey(mapFolderName.toLowerCase(Locale.ROOT));
  }

  public boolean isInstalled(MapVersionBean mapVersion) {
    return mapVersion != null && isInstalled(mapVersion.getFolderName());
  }

  public BooleanExpression isInstalledBinding(ObservableValue<MapVersionBean> mapVersionObservable) {
    return Bindings.createBooleanBinding(() -> isInstalled(mapVersionObservable.getValue()), mapVersionObservable, installedMaps);
  }

  public BooleanExpression isInstalledBinding(MapVersionBean mapVersion) {
    return Bindings.createBooleanBinding(() -> isInstalled(mapVersion), installedMaps);
  }

  public BooleanExpression isInstalledBinding(String mapFolderName) {
    return Bindings.createBooleanBinding(() -> isInstalled(mapFolderName), installedMaps);
  }

  public CompletableFuture<String> generateIfNotInstalled(String mapName) {
    if (isInstalled(mapName)) {
      return CompletableFuture.completedFuture(mapName);
    }
    return mapGeneratorService.generateMap(mapName);
  }

  public CompletableFuture<Void> download(String technicalMapName) {
    try {
      URL mapUrl = getDownloadUrl(technicalMapName, mapDownloadUrlFormat);
      return downloadAndInstallMap(technicalMapName, mapUrl, null, null);
    } catch (MalformedURLException e) {
      throw new AssetLoadException("Could not download map", e, "map.download.error", technicalMapName);
    }
  }


  public CompletableFuture<Void> downloadAndInstallMap(MapVersionBean mapVersion,
                                                       @Nullable DoubleProperty progressProperty,
                                                       @Nullable StringProperty titleProperty) {
    return downloadAndInstallMap(mapVersion.getFolderName(), mapVersion.getDownloadUrl(), progressProperty, titleProperty);
  }

  /**
   * Loads the preview of a map or returns a "unknown map" image.
   */

  @Cacheable(value = CacheNames.MAP_PREVIEW, unless = "#result.equals(@mapService.getGeneratedMapPreviewImage())")
  public Image loadPreview(MapVersionBean mapVersion, PreviewSize previewSize) {
    URL url = switch (previewSize) {
      case SMALL -> mapVersion.getThumbnailUrlSmall();
      case LARGE -> mapVersion.getThumbnailUrlLarge();
    };

    if (url != null) {
      return loadPreview(url, previewSize);
    } else {
      return loadPreview(mapVersion.getFolderName(), previewSize);
    }
  }

  private Image loadPreview(URL url, PreviewSize previewSize) {
    return assetService.loadAndCacheImage(url, Path.of("maps").resolve(previewSize.folderName),
                                          () -> themeService.getThemeImage(ThemeService.NO_IMAGE_AVAILABLE));
  }


  public CompletableFuture<Void> uninstallMap(MapVersionBean mapVersion) {
    if (isOfficialMap(mapVersion.getFolderName())) {
      throw new IllegalArgumentException("Attempt to uninstall an official map");
    }
    UninstallMapTask task = uninstallMapTaskFactory.getObject();
    task.setMap(mapVersion);
    return taskService.submitTask(task).getFuture();
  }


  public Path getPathForMap(MapVersionBean mapVersion) {
    return getPathForMapCaseInsensitive(mapVersion.getFolderName());
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

  public CompletableFuture<MapVersionBean> updateLatestVersionIfNecessary(MapVersionBean mapVersion) {
    if (isOfficialMap(mapVersion) || !preferences.isMapAndModAutoUpdate()) {
      return CompletableFuture.completedFuture(mapVersion);
    }
    return getMapLatestVersion(mapVersion).thenCompose(latestMap -> {
      CompletableFuture<Void> downloadFuture;
      if (!isInstalled(latestMap.getFolderName())) {
        downloadFuture = downloadAndInstallMap(latestMap, null, null);
      } else {
        downloadFuture = CompletableFuture.completedFuture(null);
      }
      return downloadFuture.thenApply(aVoid -> latestMap);
    }).thenCompose(latestMap -> {
      CompletableFuture<Void> uninstallFuture;
      if (!latestMap.getFolderName().equals(mapVersion.getFolderName())) {
        uninstallFuture = uninstallMap(mapVersion);
      } else {
        uninstallFuture = CompletableFuture.completedFuture(null);
      }
      return uninstallFuture.thenApply(aVoid -> latestMap);
    });
  }

  @Async
  public CompletableFuture<Integer> getFileSize(MapVersionBean mapVersion) {
    return fileSizeReader.getFileSize(mapVersion.getDownloadUrl());
  }

  private CompletableFuture<Void> downloadAndInstallMap(String folderName, URL downloadUrl,
                                                        @Nullable DoubleProperty progressProperty,
                                                        @Nullable StringProperty titleProperty) {
    if (mapGeneratorService.isGeneratedMap(folderName)) {
      return generateIfNotInstalled(folderName).thenRun(() -> {});
    }

    if (isInstalled(folderName)) {
      log.info("Map '{}' exists locally already. Download is not required", folderName);
      return CompletableFuture.completedFuture(null);
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

    return taskService.submitTask(task).getFuture();
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

  public Mono<Void> hideMapVersion(MapVersionBean map) {
    String id = String.valueOf(map.getId());
    MapVersion mapVersion = new MapVersion();
    mapVersion.setHidden(true);
    mapVersion.setId(id);
    ElideNavigatorOnId<MapVersion> navigator = ElideNavigator.of(mapVersion);
    return fafApiAccessor.patch(navigator, mapVersion);
  }

  /**
   * Tries to find a map my its folder name, first locally then on the server.
   */
  public CompletableFuture<Optional<MapVersionBean>> findByMapFolderName(String folderName) {
    Optional<MapVersionBean> installed = getMapLocallyFromName(folderName);
    if (installed.isPresent()) {
      return CompletableFuture.completedFuture(installed);
    }

    ElideNavigatorOnCollection<MapVersion> navigator = ElideNavigator.of(MapVersion.class).collection()
        .setFilter(qBuilder().string("folderName").eq(folderName));
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> mapMapper.map(dto, new CycleAvoidingMappingContext()))
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  @VisibleForTesting
  CompletableFuture<MapVersionBean> getMapLatestVersion(MapVersionBean mapVersion) {
    String folderName = mapVersion.getFolderName();

    if (!containsVersionControl(folderName)) {
      return CompletableFuture.completedFuture(mapVersion);
    }

    ElideNavigatorOnCollection<Map> navigator = ElideNavigator.of(Map.class).collection()
        .setFilter(qBuilder().string("versions.folderName").eq(folderName))
        .pageSize(1);
    return fafApiAccessor.getMany(navigator)
        .next()
        .map(dto -> mapMapper.map(dto, new CycleAvoidingMappingContext()))
        .map(MapBean::getLatestVersion)
        .toFuture()
        .thenApply(Optional::ofNullable)
        .thenApply(latestMap -> latestMap.orElse(mapVersion));

  }

  public CompletableFuture<Void> downloadAllMatchmakerMaps(MatchmakerQueueBean matchmakerQueue) {
    ElideNavigatorOnCollection<MapPoolAssignment> navigator = ElideNavigator.of(MapPoolAssignment.class).collection()
        .setFilter(qBuilder().intNum("mapPool.matchmakerQueueMapPool.matchmakerQueue.id").eq(matchmakerQueue.getId()));
    return fafApiAccessor.getMany(navigator)
        .map(mapPoolAssignment -> mapMapper.mapFromPoolAssignment(mapPoolAssignment, new CycleAvoidingMappingContext()))
        .distinct()
        .filter(mapVersion -> !mapGeneratorService.isGeneratedMap(mapVersion.getFolderName()))
        .flatMap(mapVersion -> Mono.fromCompletionStage(() -> downloadAndInstallMap(mapVersion, null, null))
            .onErrorResume(throwable -> {
              log.warn("Unable to download map `{}`", mapVersion.getFolderName(), throwable);
              notificationService.addPersistentErrorNotification(throwable, "map.download.error", mapVersion.getFolderName());
              return Mono.empty();
            }))
        .then()
        .toFuture();
  }

  @Cacheable(value = CacheNames.MATCHMAKER_POOLS, sync = true)
  public CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getMatchmakerMapsWithPageCount(
      MatchmakerQueueBean matchmakerQueue, int count, int page) {
    PlayerBean player = playerService.getCurrentPlayer();
    float rating = Optional.ofNullable(player.getLeaderboardRatings()
            .get(matchmakerQueue.getLeaderboard().getTechnicalName()))
        .map(ratingBean -> ratingBean.getMean() - 3 * ratingBean.getDeviation()).orElse(0f);
    ElideNavigatorOnCollection<MapPoolAssignment> navigator = ElideNavigator.of(MapPoolAssignment.class).collection();
    List<Condition<?>> conditions = new ArrayList<>();
    conditions.add(qBuilder().intNum("mapPool.matchmakerQueueMapPool.matchmakerQueue.id").eq(matchmakerQueue.getId()));
    conditions.add(qBuilder().floatNum("mapPool.matchmakerQueueMapPool.minRating").lte(rating).or()
        .floatNum("mapPool.matchmakerQueueMapPool.minRating").ne(null));
    // The api doesn't support the ne operation so we manually replace it with isnull which rsql does not support
    String customFilter = ((String) new QBuilder().and(conditions).query(new RSQLVisitor())).replace("ex", "isnull");
    Flux<MapVersionBean> matchmakerMapsFlux = fafApiAccessor.getMany(navigator, customFilter)
        .map(mapPoolAssignment -> mapMapper.mapFromPoolAssignment(mapPoolAssignment, new CycleAvoidingMappingContext()))
        .distinct()
        .sort(Comparator.comparing(MapVersionBean::getSize)
            .thenComparing(mapVersion -> mapVersion.getMap().getDisplayName(), String.CASE_INSENSITIVE_ORDER));
    return Mono.zip(
        matchmakerMapsFlux.skip((long) (page - 1) * count)
            .take(count).collectList(),
        matchmakerMapsFlux.count().map(size -> (int) (size - 1) / count + 1)
    ).toFuture();
  }

  public Mono<Boolean> hasPlayedMap(PlayerBean player, MapVersionBean mapVersion) {
    ElideNavigatorOnCollection<Game> navigator = ElideNavigator.of(Game.class).collection()
        .setFilter(qBuilder()
            .intNum("mapVersion.id").eq(mapVersion.getId()).and()
            .intNum("playerStats.player.id").eq(player.getId()))
        .addSortingRule("endTime", false)
        .pageSize(1);
    return fafApiAccessor.getMany(navigator)
        .hasElements();
  }

  public CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getOwnedMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<MapVersion> navigator = ElideNavigator.of(MapVersion.class).collection()
        .setFilter(qBuilder().string("map.author.id").eq(String.valueOf(playerService.getCurrentPlayer().getId())))
        .pageNumber(page)
        .pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator)
        .map(tuple -> tuple.mapT1(mapVersions ->
            mapMapper.mapVersionDtos(mapVersions, new CycleAvoidingMappingContext())
        )).toFuture();
  }

  @Cacheable(value = CacheNames.MAPS, sync = true)
  public CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> findByQueryWithPageCount(SearchConfig searchConfig,
                                                                                           int count, int page) {
    SortConfig sortConfig = searchConfig.sortConfig();
    ElideNavigatorOnCollection<Map> navigator = ElideNavigator.of(Map.class).collection()
        .addSortingRule(sortConfig.sortProperty(), sortConfig.sortOrder().equals(SortOrder.ASC));
    return getMapPage(navigator, searchConfig.searchQuery(), count, page);
  }

  @Cacheable(value = CacheNames.MAPS, sync = true)
  public CompletableFuture<Integer> getRecommendedMapPageCount(int count) {
    return getRecommendedMapsWithPageCount(count, 1).thenApply(Tuple2::getT2);
  }

  public CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getRecommendedMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Map> navigator = ElideNavigator.of(Map.class).collection()
        .setFilter(qBuilder().bool("recommended").isTrue());
    return getMapPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getHighestRatedMapsWithPageCount(int count,
                                                                                                   int page) {
    ElideNavigatorOnCollection<Map> navigator = ElideNavigator.of(Map.class).collection()
        .addSortingRule("reviewsSummary.lowerBound", false);
    return getMapPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getNewestMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Map> navigator = ElideNavigator.of(Map.class).collection()
        .addSortingRule("latestVersion.createTime", false);
    return getMapPage(navigator, count, page);
  }

  public CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getMostPlayedMapsWithPageCount(int count, int page) {
    ElideNavigatorOnCollection<Map> navigator = ElideNavigator.of(Map.class).collection()
        .addSortingRule("gamesPlayed", false);
    return getMapPage(navigator, count, page);
  }

  private CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getMapPage(ElideNavigatorOnCollection<Map> navigator,
                                                                              int count, int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream()
                .map(Map::getLatestVersion)
                .map(dto -> mapMapper.map(dto, new CycleAvoidingMappingContext()))
                .collect(toList())
        )).toFuture();
  }

  private CompletableFuture<Tuple2<List<MapVersionBean>, Integer>> getMapPage(ElideNavigatorOnCollection<Map> navigator,
                                                                              String customFilter, int count,
                                                                              int page) {
    navigator.pageNumber(page).pageSize(count);
    return fafApiAccessor.getManyWithPageCount(navigator, customFilter)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream()
                .map(dto -> mapMapper.map(dto.getLatestVersion(), new CycleAvoidingMappingContext()))
                .collect(toList())
        )).toFuture();
  }
}
