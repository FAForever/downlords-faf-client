package com.faforever.client.map;

import com.faforever.client.config.CacheNames;
import com.faforever.client.game.MapInfoBean;
import com.faforever.client.legacy.map.Comment;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.ThemeUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

  @Autowired
  Environment environment;
  @Autowired
  PreferencesService preferencesService;
  @Autowired
  TaskService taskService;
  @Autowired
  MapVaultParser mapVaultParser;
  @Autowired
  ApplicationContext applicationContext;

  @Override
  @Cacheable(CacheNames.SMALL_MAP_PREVIEW)
  public Image loadSmallPreview(String mapName) {
    String url = getMapUrl(mapName, environment.getProperty("vault.mapPreviewUrl.small"));

    logger.debug("Fetching small preview for map {} from {}", mapName, url);

    return new Image(url, true);
  }

  @Override
  @Cacheable(CacheNames.LARGE_MAP_PREVIEW)
  public Image loadLargePreview(String mapName) {
    String urlString = getMapUrl(mapName, environment.getProperty("vault.mapPreviewUrl.large"));

    logger.debug("Fetching large preview for map {} from {}", mapName, urlString);

    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        return new Image(urlString, true);
      }

      String theme = preferencesService.getPreferences().getTheme();
      return new Image(ThemeUtil.themeFile(theme, "images/map_background.png"));
    } catch (IOException e) {
      logger.warn("Could not fetch map preview", e);
      return null;
    }
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
    ObservableList<MapInfoBean> maps = FXCollections.observableArrayList();

    Path mapsDirectory = preferencesService.getPreferences().getForgedAlliance().getCustomMapsDirectory();
    if (Files.notExists(mapsDirectory)) {
      logger.warn("Local map directory does not exist: ", mapsDirectory);
      return FXCollections.emptyObservableList();
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(mapsDirectory)) {
      for (Path path : stream) {
        String mapName = path.getFileName().toString();
        maps.add(new MapInfoBean(mapName));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return maps;
  }

  //FIXME implement official map detection
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
  public CompletionStage<Void> download(String technicalMapName) {
    String mapUrl = getMapUrl(technicalMapName, environment.getProperty("vault.mapDownloadUrl"));

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
}
