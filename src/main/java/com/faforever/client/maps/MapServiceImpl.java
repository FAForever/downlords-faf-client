package com.faforever.client.maps;

import com.faforever.client.games.MapInfoBean;
import com.faforever.client.preferences.PreferencesService;
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

public class MapServiceImpl implements MapService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Override
  @Cacheable("mapPreview")
  public Image loadPreview(String mapname) {

    String baseUrl = environment.getProperty("vault.mapPreviewUrl.small");
    String url = StringEscapeUtils.escapeHtml4(String.format(baseUrl, mapname));

    logger.debug("Fetching preview for map {} from {}", mapname, url);

    return new Image(url, true);
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
}
