package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;

public interface MapService {

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  List<MapInfoBean> getMapsFromVault(int page, int maxEntries);

  ObservableList<MapInfoBean> getLocalMaps();
}
