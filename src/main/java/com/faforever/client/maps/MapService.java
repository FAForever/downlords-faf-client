package com.faforever.client.maps;

import com.faforever.client.games.MapInfoBean;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

public interface MapService {

  Image loadSmallPreview(String mapName);

  Image loadLargePreview(String mapName);

  ObservableList<MapInfoBean> getLocalMaps();
}
