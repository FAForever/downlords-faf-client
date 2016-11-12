package com.faforever.client.remote;

import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;

public interface AssetService {
  @Nullable
  Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable URL defaultImage);

  @Nullable
  Image loadAndCacheImage(URL url, Path cacheSubFolder, @Nullable URL defaultImage, int width, int height);
}
