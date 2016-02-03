package com.faforever.client.map;

import com.faforever.client.config.CacheNames;
import com.faforever.client.game.MapBean;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MapService {

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  ObservableList<MapBean> getLocalMaps();

  MapBean getMapBeanLocallyFromName(String mapName);

  MapBean findMapByName(String mapId);

  boolean isOfficialMap(String mapName);

  /**
   * Returns {@code true} if the given map is available locally, {@code false} otherwise.
   */
  boolean isAvailable(String mapName);

  CompletableFuture<Void> download(String technicalMapName);

  @Cacheable(CacheNames.MAPS)
  CompletableFuture<List<MapBean>> lookupMap(String string, int maxResults);

  CompletableFuture<List<MapBean>> getMostDownloadedMaps(int topElementCount);

}
