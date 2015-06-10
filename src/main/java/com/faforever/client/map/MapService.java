package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.util.Callback;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;

public interface MapService {

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  void getMapsFromVaultInBackground(int page, int maxEntries, Callback<List<MapInfoBean>> callback);

  ObservableList<MapInfoBean> getLocalMaps();
}
