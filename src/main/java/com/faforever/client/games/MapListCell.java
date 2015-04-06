package com.faforever.client.games;

import javafx.scene.control.ListCell;

public class MapListCell extends ListCell<MapInfoBean> {

  @Override
  protected void updateItem(MapInfoBean item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setText(item.getName());
    }
  }
}
