package com.faforever.client.remote;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.preferences.PreferencesService;
import javafx.scene.image.Image;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Supplier;


@Lazy
@Service
@Slf4j
public class AssetService {

  private final PreferencesService preferencesService;
  private final UrlValidator urlValidator;

  public AssetService(PreferencesService preferencesService) {
    this.preferencesService = preferencesService;
    urlValidator = new UrlValidator();
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

    try {
      String urlString = url.toString();
      urlString = urlValidator.isValid(urlString) ? urlString : UriUtils.encodePath(urlString, StandardCharsets.UTF_8);
      String filename = urlString.substring(urlString.lastIndexOf('/') + 1);
      Path cachePath = preferencesService.getPreferences().getData().getCacheDirectory().resolve(cacheSubFolder).resolve(filename);
      if (Files.exists(cachePath)) {
        log.debug("Using cached image: {}", cachePath);
        return new Image(cachePath.toUri().toURL().toExternalForm(), width, height, true, true, true);
      }

      log.debug("Fetching image {}", url);
      Image image = new Image(urlString, width, height, true, true, true);
      JavaFxUtil.persistImage(image, cachePath, filename.substring(filename.lastIndexOf('.') + 1));
      return image;
    } catch (InvalidPathException | MalformedURLException e) {
      log.warn("Unable to load image due to invalid fileName {}", url, e);
      if (defaultSupplier == null) {
        return null;
      }
      return defaultSupplier.get();
    }
  }
}
