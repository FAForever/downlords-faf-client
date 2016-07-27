package com.faforever.client.map;

import com.faforever.client.config.CacheNames;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ConcurrentUtil;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.faforever.client.util.LuaUtil.loadFile;
import static com.github.nocatch.NoCatch.noCatch;
import static com.google.common.net.UrlEscapers.urlFragmentEscaper;
import static java.lang.String.format;
import static java.nio.file.Files.list;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.util.Locale.US;

public class MapServiceImpl implements MapService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final float MAP_SIZE_FACTOR = 102.4f;
  private static final Lock LOOKUP_LOCK = new ReentrantLock();
  @Resource
  PreferencesService preferencesService;
  @Resource
  TaskService taskService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  Directory directory;
  @Resource
  Analyzer analyzer;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;
  @Resource
  FafService fafService;
  @Value("${vault.mapDownloadUrl}")
  String mapDownloadUrl;
  @Value("${vault.mapPreviewUrl.small}")
  String smallMapPreviewUrl;
  @Value("${vault.mapPreviewUrl.large}")
  String largeMapPreviewUrl;
  private Map<Path, MapBean> pathToMap;
  private AnalyzingInfixSuggester suggester;
  private Path mapsDirectory;
  private ObservableList<MapBean> installedMapBeans;
  private Map<String, MapBean> mapsByTechnicalName;
  public MapServiceImpl() {
    pathToMap = new HashMap<>();
    installedMapBeans = FXCollections.observableArrayList();
    mapsByTechnicalName = new HashMap<>();

    installedMapBeans.addListener((ListChangeListener<MapBean>) change -> {
      while (change.next()) {
        for (MapBean mapBean : change.getRemoved()) {
          mapsByTechnicalName.remove(mapBean.getTechnicalName().toLowerCase());
        }
        for (MapBean mapBean : change.getAddedSubList()) {
          mapsByTechnicalName.put(mapBean.getTechnicalName().toLowerCase(), mapBean);
        }
      }
    });
  }

  private static URL getMapUrl(String mapName, String baseUrl) {
    return noCatch(() -> new URL(format(baseUrl, urlFragmentEscaper().escape(mapName.toLowerCase(US)))));
  }

  @PostConstruct
  void postConstruct() throws IOException {
    mapsDirectory = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
    preferencesService.getPreferences().getForgedAlliance().customMapsDirectoryProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        onMapDirectoryReady();
      }
    });

    if (mapsDirectory != null) {
      onMapDirectoryReady();
    }

    suggester = new AnalyzingInfixSuggester(directory, analyzer);
  }

  private void onMapDirectoryReady() {
    try {
      Files.createDirectories(mapsDirectory);
      startDirectoryWatcher(mapsDirectory);
    } catch (IOException | InterruptedException e) {
      logger.warn("Could not start map directory watcher", e);
      // TODO notify user
    }
    loadInstalledMaps();
  }

  private void startDirectoryWatcher(Path mapsDirectory) throws IOException, InterruptedException {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        WatchService watcher = mapsDirectory.getFileSystem().newWatchService();
        MapServiceImpl.this.mapsDirectory.register(watcher, ENTRY_DELETE);

        while (true) {
          WatchKey key = watcher.take();
          for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == ENTRY_DELETE) {
              removeMap(mapsDirectory.resolve((Path) event.context()));
            }
          }
          key.reset();
        }
      }
    });
  }

  private void loadInstalledMaps() {
    CompletableFuture.runAsync(() -> {
      try (DirectoryStream<Path> directoryStream = newDirectoryStream(mapsDirectory)) {
        for (Path path : directoryStream) {
          try {
            addMap(path);
          } catch (MapLoadException e) {
            logger.warn("Map could not be read: " + mapsDirectory, e);
          }
        }
      } catch (IOException e) {
        logger.warn("Maps could not be read from: " + mapsDirectory, e);
      }
    }, threadPoolExecutor);
  }

  private void removeMap(Path path) {
    installedMapBeans.remove(pathToMap.remove(path));
  }

  private void addMap(Path path) throws MapLoadException {
    MapBean mapBean = readMap(path);
    if (mapBean == null) {
      return;
    }
    pathToMap.put(path, mapBean);
    if (!installedMapBeans.contains(mapBean)) {
      installedMapBeans.add(mapBean);
    }
  }

  @Override
  @Nullable
  public MapBean readMap(Path mapFolder) throws MapLoadException {
    if (!Files.isDirectory(mapFolder)) {
      logger.warn("Map does not exist: {}", mapFolder);
      return null;
    }
    return noCatch(() -> {
      Path scenarioLuaPath = noCatch(() -> list(mapFolder))
          .filter(file -> file.getFileName().toString().endsWith("_scenario.lua"))
          .findFirst()
          .orElseThrow(() -> new MapLoadException("Map folder does not contain a *_scenario.lua: " + mapFolder.toAbsolutePath()));

      LuaValue luaRoot = loadFile(scenarioLuaPath);
      LuaValue scenarioInfo = luaRoot.get("ScenarioInfo");
      LuaValue size = scenarioInfo.get("size");

      MapBean mapBean = new MapBean();
      mapBean.setTechnicalName(mapFolder.getFileName().toString());
      mapBean.setDisplayName(scenarioInfo.get("name").toString());
      mapBean.setDescription(scenarioInfo.get("description").tojstring().replaceAll("<LOC .*?>", ""));
      mapBean.setVersion(new ComparableVersion(luaRoot.get("version").tojstring()));
      mapBean.setSize(new MapSize(
          (int) (size.get(1).toint() / MAP_SIZE_FACTOR),
          (int) (size.get(2).toint() / MAP_SIZE_FACTOR))
      );
      mapBean.setPlayers(scenarioInfo.get("Configurations").get("standard").get("teams").get(1).get("armies").length());
      return mapBean;
    }, MapLoadException.class);
  }

  @Override
  @Cacheable(value = CacheNames.SMALL_MAP_PREVIEW, unless = "#result == null")
  public Image loadSmallPreview(String mapName) {
    URL url = getMapUrl(mapName, smallMapPreviewUrl);

    logger.debug("Fetching small preview for map {} from {}", mapName, url);

    return fetchImageOrNull(url);
  }

  @Override
  @Cacheable(value = CacheNames.LARGE_MAP_PREVIEW, unless = "#result == null")
  public Image loadLargePreview(String mapName) {
    URL urlString = getMapUrl(mapName, largeMapPreviewUrl);

    logger.debug("Fetching large preview for map {} from {}", mapName, urlString);

    return fetchImageOrNull(urlString);
  }

  @Override
  public ObservableList<MapBean> getInstalledMaps() {
    Path officialMapsPath = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("maps");

    Collection<Path> mapPaths = new LinkedList<>();

    for (OfficialMap officialMap : OfficialMap.values()) {
      mapPaths.add(officialMapsPath.resolve(officialMap.name()));
    }

    Path customMapsPath = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
    if (Files.notExists(customMapsPath)) {
      logger.warn("Custom map directory does not exist: {}", customMapsPath);
    } else {
      try (DirectoryStream<Path> stream = newDirectoryStream(customMapsPath)) {
        for (Path mapPath : stream) {
          mapPaths.add(mapPath);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    ObservableList<MapBean> mapBeans = FXCollections.observableArrayList();
    for (Path mapPath : mapPaths) {
      try {
        MapBean mapBean = readMap(mapPath);
        if (mapBean != null) {
          mapBeans.add(mapBean);
        }
      } catch (MapLoadException e) {
        logger.warn("Map could not be read: " + mapPath, e);
      }
    }

    return mapBeans;
  }

  @Override
  public MapBean getMapBeanLocallyFromName(String mapName) {
    logger.debug("Trying to return {} mapInfoBean locally", mapName);
    for (MapBean mapBean : getInstalledMaps()) {
      if (mapName.equalsIgnoreCase(mapBean.getDisplayName())) {
        logger.debug("Found map {} locally", mapName);
        return mapBean;
      }
    }
    return null;
  }

  @Override
  public MapBean findMapByName(String mapName) {
    return fafService.findMapByName(mapName);
  }

  @Override
  public boolean isOfficialMap(String mapName) {
    return OfficialMap.fromMapName(mapName) != null;
  }

  @Override
  public boolean isInstalled(String technicalName) {
    return mapsByTechnicalName.containsKey(technicalName.toLowerCase());
  }

  @Override
  public CompletableFuture<Void> download(String technicalMapName) {
    URL mapUrl = getMapUrl(technicalMapName, mapDownloadUrl);
    return downloadAndInstallMap(technicalMapName, mapUrl, null, null);
  }

  @Override
  public CompletableFuture<Void> downloadAndInstallMap(MapBean map, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
    return downloadAndInstallMap(map.getTechnicalName(), map.getDownloadUrl(), progressProperty, titleProperty);
  }

  @Override
  public CompletableFuture<List<MapBean>> lookupMap(String string, int maxResults) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        LOOKUP_LOCK.lock();
        MapInfoBeanIterator iterator = new MapInfoBeanIterator(fafService.getMaps().iterator());
        suggester.build(iterator);
        return suggester.lookup(string, maxResults, true, false).stream()
            .map(lookupResult -> iterator.deserialize(lookupResult.payload.bytes))
            .collect(Collectors.toList());
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        LOOKUP_LOCK.unlock();
      }
    }, threadPoolExecutor).exceptionally(throwable -> {
      logger.warn("Lookup failed", throwable);
      return null;
    });
  }

  @Override
  public CompletableFuture<List<MapBean>> getMostDownloadedMaps(int count) {
    return fafService.getMostDownloadedMaps(count);
  }

  @Override
  public CompletableFuture<List<MapBean>> getMostLikedMaps(int count) {
    return fafService.getMostLikedMaps(count);
  }

  @Override
  public CompletableFuture<List<MapBean>> getNewestMaps(int count) {
    return fafService.getNewestMaps(count);
  }

  @Override
  public CompletableFuture<List<MapBean>> getMostPlayedMaps(int count) {
    return fafService.getMostPlayedMaps(count);
  }

  @Override
  @Cacheable(CacheNames.SMALL_MAP_PREVIEW)
  public Image loadSmallPreview(MapBean map) {
    return new Image(map.getSmallThumbnailUrl().toString(), true);
  }

  @Override
  @Cacheable(CacheNames.LARGE_MAP_PREVIEW)
  public Image loadLargePreview(MapBean map) {
    return new Image(map.getLargeThumbnailUrl().toString(), true);
  }

  @Override
  public CompletionStage<Void> uninstallMap(MapBean map) {
    com.faforever.client.map.UninstallMapTask task = applicationContext.getBean(com.faforever.client.map.UninstallMapTask.class);
    task.setMap(map);
    return taskService.submitTask(task);
  }

  @Override
  public Path getPathForMap(MapBean map) {
    return getPathForMap(map.getTechnicalName());
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
  public CompletableFuture<Void> uploadMap(Path mapPath, Consumer<Float> progressListener, boolean ranked) {
    UploadMapTask uploadMapTask = applicationContext.getBean(UploadMapTask.class);
    uploadMapTask.setMapPath(mapPath);
    uploadMapTask.setProgressListener(progressListener);
    uploadMapTask.setRanked(ranked);

    CompletableFuture<Void> uploadFuture = taskService.submitTask(uploadMapTask);
    uploadMapTask.setFuture(uploadFuture);

    return uploadFuture;
  }

  private CompletableFuture<Void> downloadAndInstallMap(String technicalName, URL downloadUrl, @Nullable DoubleProperty progressProperty, @Nullable StringProperty titleProperty) {
    DownloadMapTask task = applicationContext.getBean(DownloadMapTask.class);
    task.setMapUrl(downloadUrl);
    task.setTechnicalMapName(technicalName);

    if (progressProperty != null) {
      progressProperty.bind(task.progressProperty());
    }
    if (titleProperty != null) {
      titleProperty.bind(task.titleProperty());
    }

    return taskService.submitTask(task)
        .thenAccept(aVoid -> noCatch(() -> addMap(getPathForMap(technicalName))));
  }

  @Nullable
  private Image fetchImageOrNull(URL url) {
    try {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        return new Image(url.toString(), true);
      }
      logger.debug("Map preview is not available: " + url);
      return null;
    } catch (IOException e) {
      logger.warn("Could not fetch map preview", e);
      return null;
    }
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
}
