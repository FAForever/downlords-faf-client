package com.faforever.client.maps;

import com.faforever.client.games.MapInfoBean;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;

public interface MapService {

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  List<MapInfoBean> getMapsFromVault(int page);

  ObservableList<MapInfoBean> getLocalMaps();
}
