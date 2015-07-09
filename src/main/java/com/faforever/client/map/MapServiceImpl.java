package com.faforever.client.map;

import com.faforever.client.config.CacheKeys;
import com.faforever.client.game.MapInfoBean;
import com.faforever.client.i18n.I18n;
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
      throw new RuntimeException(e);
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

  @Override
  public MapInfoBean getMapInfoBeanFromString(String mapName){
    logger.debug("Trying to return {} mapInfoBean locally", mapName);
    for (MapInfoBean mapInfoBean : getLocalMaps()) {
      if (mapName.equalsIgnoreCase(mapInfoBean.getName())) {
        logger.debug("Found map {} locally", mapName);
        return mapInfoBean;
      }
    }
    return null;
  }

  @Override
  public boolean isAvailable(String mapName) {
    logger.debug("Trying to find map {} mapName locally", mapName);

    for (MapInfoBean mapInfoBean : getLocalMaps()) {
      if (mapName.equalsIgnoreCase(mapInfoBean.getName())) {
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

  private static String getMapUrl(String mapName, String baseUrl) {
    return String.format(baseUrl, mapName.toLowerCase(Locale.US));
  }
}
