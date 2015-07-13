package com.faforever.client.map;

import com.faforever.client.game.MapInfoBean;
import com.faforever.client.util.Callback;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MapService {

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  void readMapVaultInBackground(int page, int maxEntries, Callback<List<MapInfoBean>> callback);

  ObservableList<MapInfoBean> getLocalMaps();

  MapInfoBean getMapInfoBeanFromString(String mapName);

  /**
   * Returns {@code true} if the given map is available locally, {@code false} otherwise.
   */
  boolean isAvailable(String mapName);

  void download(String mapName, Callback<Void> callback);

  List<Map<String,String>> getComments(String name) throws IOException;
}
