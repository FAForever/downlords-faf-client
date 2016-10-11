package com.faforever.client.map;

import com.faforever.client.task.CompletableTask;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;

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
   * @param mapFolderName
   */
  boolean isInstalled(String mapFolderName);

  CompletionStage<Void> download(String technicalMapName);

  CompletionStage<Void> downloadAndInstallMap(MapBean map, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletionStage<List<MapBean>> lookupMap(String string, int maxResults);

  CompletionStage<List<MapBean>> getMostDownloadedMaps(int count);

  CompletionStage<List<MapBean>> getMostLikedMaps(int count);

  CompletionStage<List<MapBean>> getNewestMaps(int count);

  CompletionStage<List<MapBean>> getMostPlayedMaps(int count);

  Image loadSmallPreview(MapBean map);

  Image loadLargePreview(MapBean map);

  CompletionStage<Void> uninstallMap(MapBean map);

  Path getPathForMap(MapBean map);

  Path getPathForMap(String technicalName);

  CompletableTask<Void> uploadMap(Path mapPath, boolean ranked);

  void evictCache();
}
