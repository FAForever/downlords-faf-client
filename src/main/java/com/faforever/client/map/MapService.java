package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.concurrent.CompletableFuture;

public interface MapService {

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  ObservableList<MapInfoBean> getLocalMaps();

  MapInfoBean getMapInfoBeanLocallyFromName(String mapName);

  MapInfoBean findMapByName(String mapId);

  boolean isOfficialMap(String mapName);

  /**
   * Returns {@code true} if the given map is available locally, {@code false} otherwise.
   */
  boolean isAvailable(String mapName);

  CompletableFuture<Void> download(String technicalMapName);

}
