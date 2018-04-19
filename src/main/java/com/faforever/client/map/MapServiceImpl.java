package com.faforever.client.map;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Vault;
import com.faforever.client.fa.FaStrings;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBean.Type;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.CompletableTask.Priority;
import com.faforever.client.task.TaskService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ProgrammingError;
import com.faforever.client.vault.search.SearchController.SortConfig;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.util.LuaUtil.loadFile;
import static com.github.nocatch.NoCatch.noCatch;
import static com.google.common.net.UrlEscapers.urlFragmentEscaper;
import static java.lang.String.format;
import static java.nio.file.Files.list;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.stream.Collectors.toCollection;


@Lazy
@Service
public class MapServiceImpl implements MapService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PreferencesService preferencesService;
  private final TaskService taskService;
  private final ApplicationContext applicationContext;
  private final FafService fafService;
  private final AssetService assetService;
  private final I18n i18n;
  private final UiService uiService;

  private final String mapDownloadUrlFormat;
  private final String mapPreviewUrlFormat;

  private Map<Path, MapBean> pathToMap;
  private Path customMapsDirectory;
  private ObservableList<MapBean> installedSkirmishMaps;
  private Map<String, MapBean> mapsByFolderName;
  private Thread directoryWatcherThread;

  @Inject
  public MapServiceImpl(PreferencesService preferencesService, TaskService taskService,
                        ApplicationContext applicationContext,
                        FafService fafService, AssetService assetService,
                        I18n i18n, UiService uiService, ClientProperties clientProperties) {
    this.preferencesService = preferencesService;
    this.taskService = taskService;
    this.applicationContext = applicationContext;
    this.fafService = fafService;
    this.assetService = assetService;
    this.i18n = i18n;
    this.uiService = uiService;

    Vault vault = clientProperties.getVault();
    this.mapDownloadUrlFormat = vault.getMapDownloadUrlFormat();
    this.mapPreviewUrlFormat = vault.getMapPreviewUrlFormat();

    pathToMap = new HashMap<>();
    installedSkirmishMaps = FXCollections.observableArrayList();
    mapsByFolderName = new HashMap<>();

    installedSkirmishMaps.addListener((ListChangeListener<MapBean>) change -> {
      while (change.next()) {
        for (MapBean mapBean : change.getRemoved()) {
          mapsByFolderName.remove(mapBean.getFolderName().toLowerCase());
        }
        for (MapBean mapBean : change.getAddedSubList()) {
          mapsByFolderName.put(mapBean.getFolderName().toLowerCase(), mapBean);
        }
      }
    });
  }

  private static URL getDownloadUrl(String mapName, String baseUrl) {
    return noCatch(() -> new URL(format(baseUrl, urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US))));
  }

  private static URL getPreviewUrl(String mapName, String baseUrl, PreviewSize previewSize) {
    return noCatch(() -> new URL(format(baseUrl, previewSize.folderName, urlFragmentEscaper().escape(mapName).toLowerCase(Locale.US))));
  }

  @PostConstruct
  void postConstruct() {
    customMapsDirectory = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
    JavaFxUtil.addListener(preferencesService.getPreferences().getForgedAlliance().pathProperty(), observable -> tryLoadMaps());
    JavaFxUtil.addListener(preferencesService.getPreferences().getForgedAlliance().customMapsDirectoryProperty(), observable -> tryLoadMaps());
    tryLoadMaps();
  }

  private void tryLoadMaps() {
    if (preferencesService.getPreferences().getForgedAlliance().getPath() == null
        || preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory() == null) {
      return;
    }
    try {
      Files.createDirectories(customMapsDirectory);
      directoryWatcherThread = startDirectoryWatcher(customMapsDirectory);
    } catch (IOException e) {
      logger.warn("Could not start map directory watcher", e);
      // TODO notify user
    }
    loadInstalledMaps();
  }

  private Thread startDirectoryWatcher(Path mapsDirectory) {
    Thread thread = new Thread(() -> noCatch(() -> {
      WatchService watcher = mapsDirectory.getFileSystem().newWatchService();
      MapServiceImpl.this.customMapsDirectory.register(watcher, ENTRY_DELETE);

      try {
        while (!Thread.interrupted()) {
          WatchKey key = watcher.take();
          key.pollEvents().stream()
              .filter(event -> event.kind() == ENTRY_DELETE)
              .forEach(event -> removeMap(mapsDirectory.resolve((Path) event.context())));
          key.reset();
        }
      } catch (InterruptedException e) {
        logger.debug("Watcher terminated ({})", e.getMessage());
      }
    }));
    thread.start();
    return thread;
  }

  private void loadInstalledMaps() {
    taskService.submitTask(new CompletableTask<Void>(Priority.LOW) {
      @Override
      protected Void call() {
        updateTitle(i18n.get("mapVault.loadingMaps"));
        Path officialMapsPath = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("maps");

        try {
          List<Path> mapPaths = new ArrayList<>();
          Files.list(customMapsDirectory).collect(toCollection(() -> mapPaths));
          Arrays.stream(OfficialMap.values())
              .map(map -> officialMapsPath.resolve(map.name()))
              .collect(toCollection(() -> mapPaths));

          long totalMaps = mapPaths.size();
          long mapsRead = 0;
          for (Path mapPath : mapPaths) {
            updateProgress(++mapsRead, totalMaps);
            addSkirmishMap(mapPath);
          }
        } catch (IOException e) {
          logger.warn("Maps could not be read from: " + customMapsDirectory, e);
        }
        return null;
      }
    });
  }

  private void removeMap(Path path) {
    installedSkirmishMaps.remove(pathToMap.remove(path));
  }

  private void addSkirmishMap(Path path) throws MapLoadException {
    try {
      MapBean mapBean = readMap(path);
      pathToMap.put(path, mapBean);
      if (!installedSkirmishMaps.contains(mapBean) && mapBean.getType() == Type.SKIRMISH) {
        installedSkirmishMaps.add(mapBean);
      }
    } catch (MapLoadException e) {
      logger.warn("Map could not be read: " + path.getFileName(), e);
    }
  }

  @Override
  @NotNull
  public MapBean readMap(Path mapFolder) throws MapLoadException {
    if (!Files.isDirectory(mapFolder)) {
      throw new MapLoadException("Not a folder: " + mapFolder.toAbsolutePath());
    }

    Path scenarioLuaPath = noCatch(() -> list(mapFolder))
        .filter(file -> file.getFileName().toString().endsWith("_scenario.lua"))
        .findFirst()
        .orElseThrow(() -> new MapLoadException("Map folder does not contain a *_scenario.lua: " + mapFolder.toAbsolutePath()));

    try {
      LuaValue luaRoot = noCatch(() -> loadFile(scenarioLuaPath), MapLoadException.class);
      LuaValue scenarioInfo = luaRoot.get("ScenarioInfo");
      LuaValue size = scenarioInfo.get("size");

      MapBean mapBean = new MapBean();
      mapBean.setFolderName(mapFolder.getFileName().toString());
      mapBean.setDisplayName(scenarioInfo.get("name").toString());
      mapBean.setDescription(FaStrings.removeLocalizationTag(scenarioInfo.get("description").toString()));
      mapBean.setType(Type.fromString(scenarioInfo.get("type").toString()));
      mapBean.setSize(MapSize.valueOf(size.get(1).toint(), size.get(2).toint()));
      mapBean.setPlayers(scenarioInfo.get("Configurations").get("standard").get("teams").get(1).get("armies").length());

      LuaValue mapVersion = scenarioInfo.get("map_version");
      if (!mapVersion.isnil()) {
        mapBean.setVersion(new ComparableVersion(mapVersion.toString()));
      }

      return mapBean;
    } catch (LuaError e) {
      throw new MapLoadException(e);
    }
  }

  @NotNull
  @Override
  @Cacheable(value = CacheNames.MAP_PREVIEW, unless = "#result == null")
  public Image loadPreview(String mapName, PreviewSize previewSize) {
    return loadPreview(getPreviewUrl(mapName, mapPreviewUrlFormat, previewSize), previewSize);
  }

  @Override
  public ObservableList<MapBean> getInstalledMaps() {
    return installedSkirmishMaps;
  }

  @Override
  public Optional<MapBean> getMapLocallyFromName(String mapFolderName) {
    logger.debug("Trying to find map '{}' locally", mapFolderName);
    ObservableList<MapBean> installedMaps = getInstalledMaps();
    synchronized (installedMaps) {
      for (MapBean mapBean : installedMaps) {
        if (mapFolderName.equalsIgnoreCase(mapBean.getFolderName())) {
          logger.debug("Found map {} locally", mapFolderName);
          return Optional.of(mapBean);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean isOfficialMap(String mapName) {
    return OfficialMap.fromMapName(mapName) != null;
  }

  @Override
  public boolean isInstalled(String mapFolderName) {
    return mapsByFolderName.containsKey(mapFolderName.toLowerCase());
  }

  @Override
  public CompletableFuture<Void> download(String technicalMapName) {
    URL mapUrl = getDownloadUrl(technicalMapName, mapDownloadUrlFormat);
    return downloadAndInstallMap(technicalMapName, mapUrl, null, null);
  }

  @Override
  public CompletableFuture<Void> downloadAndInstallMap(MapBean map, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
    return downloadAndInstallMap(map.getFolderName(), map.getDownloadUrl(), progressProperty, titleProperty);
  }

  @Override
  public CompletableFuture<List<MapBean>> getHighestRatedMaps(int count, int page) {
    return fafService.getHighestRatedMaps(count, page);
  }

  @Override
  public CompletableFuture<List<MapBean>> getNewestMaps(int count, int page) {
    return fafService.getNewestMaps(count, page);
  }

  @Override
  public CompletableFuture<List<MapBean>> getMostPlayedMaps(int count, int page) {
    return fafService.getMostPlayedMaps(count, page);
  }

  @Override
  @Cacheable(CacheNames.MAP_PREVIEW)
  public Image loadPreview(MapBean map, PreviewSize previewSize) {
    URL url;
    switch (previewSize) {
      case SMALL:
        url = map.getSmallThumbnailUrl();
        break;
      case LARGE:
        url = map.getLargeThumbnailUrl();
        break;
      default:
        throw new ProgrammingError("Uncovered preview size: " + previewSize);
    }
    return loadPreview(url, previewSize);
  }

  private Image loadPreview(URL url, PreviewSize previewSize) {
    return assetService.loadAndCacheImage(url, Paths.get("maps").resolve(previewSize.folderName),
        () -> uiService.getThemeImage(UiService.UNKNOWN_MAP_IMAGE));
  }

  @Override
  public CompletableFuture<Void> uninstallMap(MapBean map) {
    UninstallMapTask task = applicationContext.getBean(com.faforever.client.map.UninstallMapTask.class);
    task.setMap(map);
    return taskService.submitTask(task).getFuture();
  }

  @Override
  public Path getPathForMap(MapBean map) {
    return getPathForMap(map.getFolderName());
  }

  @Override
  public Path getPathForMap(String technicalName) {
    Path path = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory().resolve(technicalName);
    if (Files.notExists(path)) {
      return null;
    }
    return path;
  }

  @Override
  public CompletableTask<Void> uploadMap(Path mapPath, boolean ranked) {
    MapUploadTask mapUploadTask = applicationContext.getBean(MapUploadTask.class);
    mapUploadTask.setMapPath(mapPath);
    mapUploadTask.setRanked(ranked);

    return taskService.submitTask(mapUploadTask);
  }

  @Override
  @CacheEvict(CacheNames.MAPS)
  public void evictCache() {
    // Nothing to see here
  }

  @Override
  public CompletableFuture<Optional<MapBean>> findByMapFolderName(String folderName) {
    Path localMapFolder = getPathForMap(folderName);
    if (localMapFolder != null && Files.exists(localMapFolder)) {
      return CompletableFuture.completedFuture(Optional.of(readMap(localMapFolder)));
    }
    return fafService.findMapByFolderName(folderName);
  }

  @Override
  public CompletableFuture<Boolean> hasPlayedMap(int playerId, String mapVersionId) {
    return fafService.getLastGameOnMap(playerId, mapVersionId)
        .thenApply(Optional::isPresent);
  }

  @Override
  @Async
  public CompletableFuture<Integer> getFileSize(URL downloadUrl) {
    return CompletableFuture.completedFuture(noCatch(() -> downloadUrl
        .openConnection()
        .getContentLength()));
  }

  @Override
  public CompletableFuture<List<MapBean>> findByQuery(String query, int page, int maxSearchResults, SortConfig sortConfig) {
    return fafService.findMapsByQuery(query, page, maxSearchResults, sortConfig);
  }

  @Override
  public Optional<MapBean> findMap(String id) {
    return fafService.findMapById(id);
  }

  @Override
  public CompletableFuture<List<MapBean>> getLadderMaps(int loadMoreCount, int page) {
    return fafService.getLadder1v1Maps(loadMoreCount, page);
  }

  private CompletableFuture<Void> downloadAndInstallMap(String folderName, URL downloadUrl, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
    DownloadMapTask task = applicationContext.getBean(DownloadMapTask.class);
    task.setMapUrl(downloadUrl);
    task.setFolderName(folderName);

    if (progressProperty != null) {
      progressProperty.bind(task.progressProperty());
    }
    if (titleProperty != null) {
      titleProperty.bind(task.titleProperty());
    }

    return taskService.submitTask(task).getFuture()
        .thenAccept(aVoid -> noCatch(() -> addSkirmishMap(getPathForMap(folderName))));
  }

  @PreDestroy
  private void preDestroy() {
    Optional.ofNullable(directoryWatcherThread).ifPresent(Thread::interrupt);
  }

  public enum OfficialMap {
    SCMP_001, SCMP_002, SCMP_003, SCMP_004, SCMP_005, SCMP_006, SCMP_007, SCMP_008, SCMP_009, SCMP_010, SCMP_011,
    SCMP_012, SCMP_013, SCMP_014, SCMP_015, SCMP_016, SCMP_017, SCMP_018, SCMP_019, SCMP_020, SCMP_021, SCMP_022,
    SCMP_023, SCMP_024, SCMP_025, SCMP_026, SCMP_027, SCMP_028, SCMP_029, SCMP_030, SCMP_031, SCMP_032, SCMP_033,
    SCMP_034, SCMP_035, SCMP_036, SCMP_037, SCMP_038, SCMP_039, SCMP_040, X1MP_001, X1MP_002, X1MP_003, X1MP_004,
    X1MP_005, X1MP_006, X1MP_007, X1MP_008, X1MP_009, X1MP_010, X1MP_011, X1MP_012, X1MP_014, X1MP_017;

    private static final Map<String, OfficialMap> fromString;

    static {
      fromString = new HashMap<>();
      for (OfficialMap officialMap : values()) {
        fromString.put(officialMap.name(), officialMap);
      }
    }

    public static OfficialMap fromMapName(String mapName) {
      return fromString.get(mapName.toUpperCase());
    }
  }

  public enum PreviewSize {
    // These must match the preview URLs
    SMALL("small"), LARGE("large");

    String folderName;

    PreviewSize(String folderName) {
      this.folderName = folderName;
    }
  }
}
