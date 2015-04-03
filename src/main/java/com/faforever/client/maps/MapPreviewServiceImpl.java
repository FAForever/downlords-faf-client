package com.faforever.client.maps;

import javafx.scene.image.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;

public class MapPreviewServiceImpl implements MapPreviewService {

  @Autowired
  Environment environment;

  @Override
  @Cacheable("mapPreview")
  public Image loadPreview(String mapname) {
    String baseUrl = environment.getProperty("vault.mapPreviewUrl.small");
    String url = String.format(baseUrl, mapname);
    return new Image(url, true);
  }
}
