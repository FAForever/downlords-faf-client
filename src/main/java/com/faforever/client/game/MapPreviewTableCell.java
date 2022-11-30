package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.ImageViewHelper;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MapPreviewTableCell extends TableCell<GameBean, Image> {

  private final ImageView imageVew;
  private final ImageViewHelper imageViewHelper;

  public MapPreviewTableCell(ImageViewHelper imageViewHelper) {
    this.imageViewHelper = imageViewHelper;

    imageVew = new ImageView();
    imageVew.setFitWidth(36.0);
    imageVew.setFitHeight(36.0);
    imageVew.setSmooth(true);
    imageVew.setPreserveRatio(true);
    imageViewHelper.setDefaultPlaceholderImage(imageVew, true);
  }

  @Override
  protected void updateItem(Image item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      imageVew.setImage(!item.isError() ? item : imageViewHelper.getDefaultPlaceholderImage());
      setGraphic(imageVew);
    }
  }
}

