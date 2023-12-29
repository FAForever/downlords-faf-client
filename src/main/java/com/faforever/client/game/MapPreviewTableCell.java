package com.faforever.client.game;

import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.ImageViewHelper;
import javafx.scene.control.TableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MapPreviewTableCell extends TableCell<GameBean, Image> {

  private final ImageView imageView;
  private final ImageViewHelper imageViewHelper;

  public MapPreviewTableCell(ImageViewHelper imageViewHelper) {
    this.imageViewHelper = imageViewHelper;

    imageView = new ImageView();
    imageView.setFitWidth(36.0);
    imageView.setFitHeight(36.0);
    imageView.setSmooth(true);
    imageView.setPreserveRatio(true);
  }

  @Override
  protected void updateItem(Image item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      imageView.imageProperty().bind(imageViewHelper.createPlaceholderImageOnErrorObservable(item));
      setGraphic(imageView);
    }
  }
}

