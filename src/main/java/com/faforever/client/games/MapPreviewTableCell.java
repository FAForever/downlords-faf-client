package com.faforever.client.games;

import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MapPreviewTableCell extends TableCell<GameInfoBean, Image> {

  private ImageView imageVew;

  public MapPreviewTableCell() {
    this.imageVew = new ImageView();
    setGraphic(imageVew);
  }

  @Override
  protected void updateItem(Image item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      imageVew.setImage(item);
      setGraphic(imageVew);
    }
  }
}

