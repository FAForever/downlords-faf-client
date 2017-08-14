package com.faforever.client.remote;

import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.util.function.Supplier;

public interface AssetService {
  @Nullable
  Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable Supplier<Image> defaultSupplier);

  @Nullable
  Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable Supplier<Image> defaultSupplier, int width, int height);
}
