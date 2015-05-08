package com.faforever.client.game;

import javafx.scene.control.ListCell;

public class ModListCell extends ListCell<ModInfoBean> {

  @Override
  protected void updateItem(ModInfoBean item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setText(item.getFullName());
    }
  }
}
