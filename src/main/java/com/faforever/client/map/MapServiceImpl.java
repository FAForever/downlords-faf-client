package com.faforever.client.map;

import com.faforever.client.config.CacheNames;
import com.faforever.client.game.MapInfoBean;
import com.faforever.client.game.MapSize;
import com.faforever.client.legacy.map.Comment;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.util.LuaUtil.stripQuotes;

public class MapServiceImpl implements MapService {

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

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern MAP_SIZE_PATTERN = Pattern.compile("(\\d+),\\s*(\\d+)");

  @Resource
  PreferencesService preferencesService;
  @Resource
  TaskService taskService;
  @Resource
  MapVaultParser mapVaultParser;
  @Resource
  ApplicationContext applicationContext;

  @Value("${vault.mapDownloadUrl}")
  String mapDownloadUrl;
  @Value("${vault.mapPreviewUrl.small}")
  String smallMapPreviewUrl;
  @Value("${vault.mapPreviewUrl.large}")
  String largeMapPreviewUrl;

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
  public CompletableFuture<List<MapInfoBean>> readMapVaultInBackground(int page, int maxEntries) {
    MapVaultParseTask task = applicationContext.getBean(MapVaultParseTask.class);
    task.setMaxEntries(maxEntries);
    task.setPage(page);
    return taskService.submitTask(task);
  }

  @Override
  public ObservableList<MapInfoBean> getLocalMaps() {
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

    ObservableList<MapInfoBean> maps = FXCollections.observableArrayList();
    for (Path mapPath : mapPaths) {
      try {
        MapInfoBean mapInfoBean = readMap(mapPath);
        if (mapInfoBean != null) {
          maps.add(mapInfoBean);
        }
      } catch (IOException e) {
        logger.warn("Map could not be read: " + mapPath, e);
      }
    }

    return maps;
  }

  @Override
  public MapInfoBean getMapInfoBeanLocallyFromName(String mapName) {
    logger.debug("Trying to return {} mapInfoBean locally", mapName);
    for (MapInfoBean mapInfoBean : getLocalMaps()) {
      if (mapName.equalsIgnoreCase(mapInfoBean.getDisplayName())) {
        logger.debug("Found map {} locally", mapName);
        return mapInfoBean;
      }
    }
    return null;
  }

  @Nullable
  private MapInfoBean readMap(Path mapPath) throws IOException {
    if (!Files.isDirectory(mapPath)) {
      logger.warn("Map does not exist: {}", mapPath);
      return null;
    }
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(mapPath, "*_scenario.lua")) {
      Iterator<Path> iterator = directoryStream.iterator();
      if (!iterator.hasNext()) {
        return null;
      }

      try (InputStream inputStream = Files.newInputStream(iterator.next())) {
        Properties properties = new Properties();
        properties.load(inputStream);

        MapInfoBean mapInfoBean = new MapInfoBean(mapPath.getFileName().toString());
        mapInfoBean.setDisplayName(stripQuotes(properties.getProperty("name")));
        mapInfoBean.setDescription(stripQuotes(properties.getProperty("description")));

        Matcher matcher = MAP_SIZE_PATTERN.matcher(properties.getProperty("size"));
        if (matcher.find()) {
          int width = Integer.parseInt(matcher.group(1));
          int height = Integer.parseInt(matcher.group(2));
          mapInfoBean.setSize(new MapSize(width, height));
        }
        return mapInfoBean;
      }
    }
  }

  @Override
  public MapInfoBean getMapInfoBeanFromVaultByName(String mapName) {
    logger.info("Trying to return {} mapInfoBean from vault", mapName);
    //TODO implement official map vault parser
    if (isOfficialMap(mapName)) {
      return null;
    }
    try {
      return mapVaultParser.parseSingleMap(mapName);
    } catch (IOException | IllegalStateException e) {
      logger.error("Error in parsing {} from vault", mapName);
      return null;
    }
  }

  @Override
  public boolean isOfficialMap(String mapName) {
    return OfficialMap.fromMapName(mapName) != null;
  }

  @Override
  public boolean isAvailable(String mapName) {
    logger.debug("Trying to find map {} mapName locally", mapName);

    for (MapInfoBean mapInfoBean : getLocalMaps()) {
      if (mapName.equalsIgnoreCase(mapInfoBean.getDisplayName())) {
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
  public List<Comment> getComments(int mapId) {
    //int mapId = getMapInfoBeanFromVaultByName(mapName).getId();
    if (mapId == 0) {
      return Collections.emptyList();
    }
    try {
      return mapVaultParser.parseComments(mapId);
    } catch (IOException e) {
      logger.error("Error in parsing comment for {}", mapId);
    }
    return null;
  }

  private static String getMapUrl(String mapName, String baseUrl) {
    return String.format(baseUrl, mapName.toLowerCase(Locale.US));
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
