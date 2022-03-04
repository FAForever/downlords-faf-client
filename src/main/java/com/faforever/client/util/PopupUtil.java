package com.faforever.client.util;

import com.faforever.client.ui.StageHolder;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Screen;

public class PopupUtil {

  public static void showImagePopup(Image image) {
    showImagePopup(image, 0.7);
  }

  /**
   * @param imageHeightInPercentage Image height as a percentage of the screen height size
   */
  public static void showImagePopup(Image image, double imageHeightInPercentage) {
    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    double imageSize = screenBounds.getHeight() * imageHeightInPercentage;
    double centerScreenX = screenBounds.getWidth() / 2 - imageSize / 2;
    double centerScreenY = screenBounds.getHeight() / 2 - imageSize / 2;

    ImageView mapImageView = new ImageView(image);
    mapImageView.setFitHeight(imageSize);
    mapImageView.setSmooth(true);
    mapImageView.setPreserveRatio(true);

    Popup popup = new Popup();
    popup.setAutoHide(true);
    popup.setAutoFix(true);
    popup.getScene().setRoot(new StackPane(mapImageView));
    popup.show(StageHolder.getStage(), centerScreenX, centerScreenY);
  }
}
