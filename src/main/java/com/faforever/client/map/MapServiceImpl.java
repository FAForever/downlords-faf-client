package com.faforever.client.map;

import com.faforever.client.config.CacheNames;
import com.faforever.client.game.MapBean;
import com.faforever.client.game.MapSize;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.LuaUtil;
import com.google.common.net.UrlEscapers;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class MapServiceImpl implements MapService {

  public enum OfficialMap {
    SCMP_001, SCMP_002, SCMP_003, SCMP_004, SCMP_005, SCMP_006, SCMP_007, SCMP_008, SCMP_009, SCMP_010, SCMP_011,
    SCMP_012, SCMP_013, SCMP_014, SCMP_015, SCMP_016, SCMP_017, SCMP_018, SCMP_019, SCMP_020, SCMP_021, SCMP_022,
    SCMP_023, SCMP_024, SCMP_025, SCMP_026, SCMP_027, SCMP_028, SCMP_029, SCMP_030, SCMP_031, SCMP_032, SCMP_033,
    SCMP_034, SCMP_035, SCMP_036, SCMP_037, SCMP_038, SCMP_039, SCMP_040, X1MP_001, X1MP_002, X1MP_003, X1MP_004,
    X1MP_005, X1MP_006, X1MP_007, X1MP_008, X1MP_009, X1MP_010, X1MP_011, X1MP_012, X1MP_014, X1MP_017;

    private static final java.util.Map fromString;

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
  Executor executor;
  @Resource
  FafService fafService;

  @Value("${vault.mapDownloadUrl}")
  String mapDownloadUrl;
  @Value("${vault.mapPreviewUrl.small}")
  String smallMapPreviewUrl;
  @Value("${vault.mapPreviewUrl.large}")
  String largeMapPreviewUrl;

  private java.util.Map pathToMap;
  private AnalyzingInfixSuggester suggester;
  private Path mapsDirectory;
  private ObservableList<MapBean> installedMapBeans;

  public MapServiceImpl() {
    pathToMap = new HashMap<>();
    installedMapBeans = FXCollections.observableArrayList();
  }

  @PostConstruct
  void postConstruct() throws IOException {
    mapsDirectory = preferencesService.getPreferences().getForgedAlliance().getModsDirectory();
    preferencesService.getPreferences().getForgedAlliance().modsDirectoryProperty().addListener((observable, oldValue, newValue) -> {
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
      logger.warn("Could not start mod directory watcher", e);
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

  public void loadInstalledMaps() {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(mapsDirectory)) {
      for (Path path : directoryStream) {
        addMap(path);
      }
    } catch (IOException e) {
      logger.warn("Mods could not be read from: " + mapsDirectory, e);
    }
  }

  private void removeMap(Path path) throws IOException {
    installedMapBeans.remove(pathToMap.remove(path));
  }

  private void addMap(Path path) throws IOException {
    MapBean mapBean = readMap(path);
    if (mapBean == null) {
      return;
    }
    pathToMap.put(path, mapBean);
    if (!installedMapBeans.contains(mapBean)) {
      installedMapBeans.add(mapBean);
    }
  }

  @Nullable
  private MapBean readMap(Path mapPath) throws IOException {
    if (!Files.isDirectory(mapPath)) {
      logger.warn("Map does not exist: {}", mapPath);
      return null;
    }
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(mapPath, "*_scenario.lua")) {
      Iterator<Path> iterator = directoryStream.iterator();
      if (!iterator.hasNext()) {
        return null;
      }

      LuaValue luaValue = LuaUtil.loadFile(iterator.next()).get("ScenarioInfo");
      LuaValue size = luaValue.get("size");

      MapBean mapBean = new MapBean();
      mapBean.setTechnicalName(mapPath.getFileName().toString());
      mapBean.setDisplayName(luaValue.get("name").toString());
      mapBean.setDescription(luaValue.get("description").tojstring().replaceAll("<LOC .*?>", ""));
      mapBean.setSize(new MapSize(
          (int) (size.get(1).toint() / MAP_SIZE_FACTOR),
          (int) (size.get(2).toint() / MAP_SIZE_FACTOR))
      );
      mapBean.setPlayers(luaValue.get("Configurations").get("standard").get("teams").get(1).get("armies").length());
      return mapBean;
    }
  }

  @Override
  @Cacheable(value = CacheNames.SMALL_MAP_PREVIEW, unless = "#result == null")
  public Image loadSmallPreview(String mapName) {
    String url = getMapUrl(mapName, smallMapPreviewUrl);

    logger.debug("Fetching small preview for map {} from {}", mapName, url);

    return fetchImageOrNull(url);
  }

  @Override
  @Cacheable(value = CacheNames.LARGE_MAP_PREVIEW, unless = "#result == null")
  public Image loadLargePreview(String mapName) {
    String urlString = getMapUrl(mapName, largeMapPreviewUrl);

    logger.debug("Fetching large preview for map {} from {}", mapName, urlString);

    return fetchImageOrNull(urlString);
  }

  @Override
  public ObservableList<MapBean> getLocalMaps() {
    Path officialMapsPath = preferencesService.getPreferences().getForgedAlliance().getPath().resolve("maps");

    Collection<Path> mapPaths = new LinkedList<>();

    for (OfficialMap officialMap : OfficialMap.values()) {
      mapPaths.add(officialMapsPath.resolve(officialMap.name()));
    }

    Path customMapsPath = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
    if (Files.notExists(customMapsPath)) {
      logger.warn("Custom map directory does not exist: {}", customMapsPath);
    } else {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(customMapsPath)) {
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
      } catch (IOException e) {
        logger.warn("Map could not be read: " + mapPath, e);
      }
    }

    return mapBeans;
  }

  @Override
  public MapBean getMapBeanLocallyFromName(String mapName) {
    logger.debug("Trying to return {} mapInfoBean locally", mapName);
    for (MapBean mapBean : getLocalMaps()) {
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
  public boolean isAvailable(String mapName) {
    logger.debug("Trying to find map {} mapName locally", mapName);

    for (MapBean mapBean : getLocalMaps()) {
      if (mapName.equalsIgnoreCase(mapBean.getDisplayName())) {
        logger.debug("Found map {} locally", mapName);
        return true;
      }
    }

    logger.debug("Map {} is not available locally", mapName);
    return false;
  }

  @Override
  public CompletableFuture<Void> download(String technicalMapName) {
    String mapUrl = getMapUrl(technicalMapName, mapDownloadUrl);

    DownloadMapTask task = applicationContext.getBean(DownloadMapTask.class);
    task.setMapUrl(mapUrl);
    task.setTechnicalMapName(technicalMapName);
    return taskService.submitTask(task);
  }

  @Override
  @Cacheable(CacheNames.MAPS)
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
    }, executor).exceptionally(throwable -> {
      logger.warn("Lookup failed", throwable);
      return null;
    });
  }

  private static String getMapUrl(String mapName, String baseUrl) {
    return String.format(baseUrl, UrlEscapers.urlFragmentEscaper().escape(mapName.toLowerCase(Locale.US)));
  }

  @Nullable
  private Image fetchImageOrNull(String urlString) {
    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        return new Image(urlString, true);
      }
      logger.debug("Map preview is not available: " + urlString);
      return null;
    } catch (IOException e) {
      logger.warn("Could not fetch map preview", e);
      return null;
    }
  }
}
