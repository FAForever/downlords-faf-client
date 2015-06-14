package com.faforever.client.game;

import javafx.scene.control.ListCell;

public class ModListCell extends ListCell<GameTypeBean> {

  @Override
  protected void updateItem(GameTypeBean item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setText(item.getFullName());
    }
  }
}
