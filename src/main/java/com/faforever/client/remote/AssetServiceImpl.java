package com.faforever.client.remote;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import static com.github.nocatch.NoCatch.noCatch;


@Lazy
@Service
public class AssetServiceImpl implements AssetService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final PreferencesService preferencesService;

  @Inject
  public AssetServiceImpl(PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
  }

  @Nullable
  public Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable Supplier<Image> defaultSupplier) {
    return loadAndCacheImage(url, cacheSubFolder, defaultSupplier, 0, 0);
  }

  @Nullable
  public Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable Supplier<Image> defaultSupplier, int width, int height) {
    if (url == null) {
      if (defaultSupplier == null) {
        return null;
      }
      return defaultSupplier.get();
    }

    String urlString = url.toString();
    String filename = urlString.substring(urlString.lastIndexOf('/') + 1);
    Path cachePath = preferencesService.getCacheDirectory().resolve(cacheSubFolder).resolve(filename);
    if (Files.exists(cachePath)) {
      logger.debug("Using cached image: {}", cachePath);
      return new Image(noCatch(() -> cachePath.toUri().toURL().toExternalForm()), true);
    }

    logger.debug("Fetching image {}", url);

    Image image = new Image(url.toString(), true);
    JavaFxUtil.persistImage(image, cachePath, filename.substring(filename.lastIndexOf('.') + 1));
    return image;
  }
}
