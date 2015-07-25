package com.faforever.client.map;

import com.faforever.client.config.CacheKeys;
import com.faforever.client.game.MapInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.map.Comment;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskGroup;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ThemeUtil;
import com.faforever.client.util.Unzipper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;

import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class MapServiceImpl implements MapService {

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
  I18n i18n;

  public enum officialMaps{
    SCMP_001, SCMP_002, SCMP_003, SCMP_004, SCMP_005, SCMP_006, SCMP_007, SCMP_008, SCMP_009, SCMP_010, SCMP_011, SCMP_012, SCMP_013,
    SCMP_014, SCMP_015, SCMP_016, SCMP_017, SCMP_018, SCMP_019, SCMP_020, SCMP_021, SCMP_022, SCMP_023, SCMP_024, SCMP_025, SCMP_026,
    SCMP_027, SCMP_028, SCMP_029, SCMP_030, SCMP_031, SCMP_032, SCMP_033, SCMP_034, SCMP_035, SCMP_036, SCMP_037, SCMP_038, SCMP_039,
    SCMP_040, X1MP_001, X1MP_002, X1MP_003, X1MP_004, X1MP_005, X1MP_006, X1MP_007, X1MP_008, X1MP_009, X1MP_010, X1MP_011, X1MP_012, X1MP_014, X1MP_017
  }

  @Override
  @Cacheable(CacheKeys.SMALL_MAP_PREVIEW)
  public Image loadSmallPreview(String mapName) {
    String url = getMapUrl(mapName, environment.getProperty("vault.mapPreviewUrl.small"));

    logger.debug("Fetching small preview for map {} from {}", mapName, url);

    return new Image(url, true);
  }

  @Override
  @Cacheable(CacheKeys.LARGE_MAP_PREVIEW)
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
  public void readMapVaultInBackground(int page, int maxEntries, Callback<List<MapInfoBean>> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<List<MapInfoBean>>(i18n.get("readMapVaultTask.title")) {
      @Override
      protected List<MapInfoBean> call() throws Exception {
        return mapVaultParser.parseMapVault(page, maxEntries);
      }
    }, callback);
  }

  @Override
  public ObservableList<MapInfoBean> getLocalMaps() {
    ObservableList<MapInfoBean> maps = FXCollections.observableArrayList();

    Path mapsDirectory = preferencesService.getPreferences().getForgedAlliance().getMapsDirectory();

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
  public MapInfoBean getMapInfoBeanLocallyFromName(String mapName){
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
  public MapInfoBean getMapInfoBeanFromVaultFromName(String mapName) {
    logger.info("Trying to return {} mapInfoBean from vault", mapName);
    //TODO implement official map vault parser
    if (isOfficialMap(mapName)) {
      return null;
    }
    try {
      return mapVaultParser.parseSingleMap(mapName);
    } catch (IOException | IllegalStateException e) {
      logger.error("Error in parsing {} from vault", mapName);
      return new MapInfoBean();
    }
  }

  @Override
  public boolean isOfficialMap(String mapName) {
    for(officialMaps map : officialMaps.values()){
      if(map.name().equals(mapName.toUpperCase())){
        logger.debug("{} is an official map", mapName);
        //return getMapInfoBeanLocallyFromName(mapName);
        return true;
      }
    }
    return false;
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
  public void download(String mapName, Callback<Void> callback) {
    String taskTitle = i18n.get("mapDownloadTask.title", mapName);
    taskService.submitTask(TaskGroup.NET_HEAVY, new PrioritizedTask<Void>(taskTitle) {
      @Override
      protected Void call() throws Exception {
        String mapUrl = getMapUrl(mapName, environment.getProperty("vault.mapDownloadUrl"));

        logger.info("Downloading map {} from {}", mapName, mapUrl);

        HttpURLConnection urlConnection = (HttpURLConnection) new URL(mapUrl).openConnection();
        int bytesToRead = urlConnection.getContentLength();

        Path targetDirectory = preferencesService.getPreferences().getForgedAlliance().getMapsDirectory();

        try (ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(urlConnection.getInputStream()))) {
          Unzipper.from(inputStream)
              .to(targetDirectory)
              .totalBytes(bytesToRead)
              .listener(this::updateProgress)
              .unzip();
        }

        return null;
      }
    }, callback);
  }

  @Override
  public List<Comment> getComments(String mapName) {
    int id = getMapInfoBeanFromVaultFromName(mapName).getId();
    if(id == 0){
      return new ArrayList<>();
    }
    try {
      return mapVaultParser.parseComments(id);
    } catch (IOException e) {
      logger.error("Error in parsing comment for {}", mapName);
    }
    return null;
  }

  private static String getMapUrl(String mapName, String baseUrl) {
    return String.format(baseUrl, mapName.toLowerCase(Locale.US));
  }
}
