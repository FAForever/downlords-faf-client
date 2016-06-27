package com.faforever.client.map;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public interface MapService {

  @Nullable
  MapBean readMap(Path mapPath);

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  ObservableList<MapBean> getInstalledMaps();

  MapBean getMapBeanLocallyFromName(String mapName);

  MapBean findMapByName(String mapId);

  boolean isOfficialMap(String mapName);

  /**
   * Returns {@code true} if the given map is available locally, {@code false} otherwise.
   */
  boolean isInstalled(String technicalName);

  CompletableFuture<Void> download(String technicalMapName);

  CompletableFuture<Void> downloadAndInstallMap(MapBean map, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletableFuture<List<MapBean>> lookupMap(String string, int maxResults);

  CompletableFuture<List<MapBean>> getMostDownloadedMaps(int count);

  CompletableFuture<List<MapBean>> getMostLikedMaps(int count);

  CompletableFuture<List<MapBean>> getNewestMaps(int count);

  CompletableFuture<List<MapBean>> getMostPlayedMaps(int count);

  Image loadSmallPreview(MapBean map);

  Image loadLargePreview(MapBean map);

  CompletionStage<Void> uninstallMap(MapBean map);

  Path getPathForMap(MapBean map);

  Path getPathForMap(String technicalName);

  CompletableFuture<Void> uploadMap(Path mapPath, Consumer<Float> progressListener, boolean ranked);
}
