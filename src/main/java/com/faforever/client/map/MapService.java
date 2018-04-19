package com.faforever.client.map;

import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.vault.search.SearchController.SortConfig;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface MapService {

  @Nullable
  MapBean readMap(Path mapPath);

  /**
   * Loads the preview of a map or returns a "unknown map" image.
   */
  @NotNull
  Image loadPreview(String mapName, PreviewSize previewSize);

  ObservableList<MapBean> getInstalledMaps();

  Optional<MapBean> getMapLocallyFromName(String mapName);

  boolean isOfficialMap(String mapName);

  /**
   * Returns {@code true} if the given map is available locally, {@code false} otherwise.
   */
  boolean isInstalled(String mapFolderName);

  CompletableFuture<Void> download(String technicalMapName);

  CompletableFuture<Void> downloadAndInstallMap(MapBean map, DoubleProperty progressProperty, StringProperty titleProperty);

  CompletableFuture<List<MapBean>> getHighestRatedMaps(int count, int page);

  CompletableFuture<List<MapBean>> getNewestMaps(int count, int page);

  CompletableFuture<List<MapBean>> getMostPlayedMaps(int count, int page);

  Image loadPreview(MapBean map, PreviewSize previewSize);

  CompletableFuture<Void> uninstallMap(MapBean map);

  Path getPathForMap(MapBean map);

  Path getPathForMap(String technicalName);

  CompletableTask<Void> uploadMap(Path mapPath, boolean ranked);

  void evictCache();

  /**
   * Tries to find a map my its folder name, first locally then on the server.
   */
  CompletableFuture<Optional<MapBean>> findByMapFolderName(String folderName);

  CompletableFuture<Boolean> hasPlayedMap(int playerId, String mapVersionId);

  CompletableFuture<Integer> getFileSize(URL downloadUrl);

  CompletableFuture<List<MapBean>> findByQuery(String query, int page, int maxSearchResults, SortConfig sortConfig);

  Optional<MapBean> findMap(String id);

  CompletableFuture<List<MapBean>> getLadderMaps(int loadMoreCount, int page);
}
