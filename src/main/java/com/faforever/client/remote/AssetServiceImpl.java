package com.faforever.client.remote;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.nocatch.NoCatch.noCatch;

public class AssetServiceImpl implements AssetService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  PreferencesService preferencesService;

  @Override
  public Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable URL defaultImage) {
    return loadAndCacheImage(url, cacheSubFolder, defaultImage, 0, 0);
  }

  @Override
  public Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable URL defaultImage, int width, int height) {
    String urlString = url.toString();
    String filename = urlString.substring(urlString.lastIndexOf('/') + 1);
    Path cachePath = preferencesService.getCacheDirectory().resolve(cacheSubFolder).resolve(filename);
    if (Files.exists(cachePath)) {
      return new Image(noCatch(() -> cachePath.toUri().toURL().toExternalForm()), true);
    }
    logger.debug("Fetching image {}", url);

    Image image = defaultImage(url, defaultImage);
    JavaFxUtil.persistImage(image, cachePath, filename.substring(filename.lastIndexOf('.') + 1));
    return image;
  }

  private Image defaultImage(URL url, @Nullable URL defaultImageUrl) {
    try {
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        return new Image(url.toString(), true);
      }
      logger.debug("Image not available: " + url);
    } catch (IOException e) {
      logger.warn("Could not fetch image from: " + url, e);
    }
    if (defaultImageUrl != null) {
      return new Image(defaultImageUrl.toString(), true);
    }
    return null;
  }
}
