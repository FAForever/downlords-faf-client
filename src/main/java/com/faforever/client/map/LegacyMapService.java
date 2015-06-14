package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.map.MapVaultParser;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class LegacyMapService implements MapService {

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
  @Cacheable("smallMapPreview")
  public Image loadSmallPreview(String mapName) {


    String url = getMapUrl(mapName, environment.getProperty("vault.mapPreviewUrl.small"));

    logger.debug("Fetching small preview for map {} from {}", mapName, url);

    return new Image(url, true);
  }

  @Override
  @Cacheable("largeMapPreview")
  public Image loadLargePreview(String mapName) {
    String url = getMapUrl(mapName, environment.getProperty("vault.mapPreviewUrl.large"));

    logger.debug("Fetching large preview for map {} from {}", mapName, url);

    return new Image(url, true);
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

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(preferencesService.getMapsDirectory())) {
      for (Path path : stream) {
        String mapName = path.getFileName().toString();
        maps.add(new MapInfoBean(mapName));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return maps;
  }

  private static String getMapUrl(String mapName, String baseUrl) {
    return StringEscapeUtils.escapeHtml4(String.format(baseUrl, mapName)).toLowerCase(Locale.US);
  }
}
