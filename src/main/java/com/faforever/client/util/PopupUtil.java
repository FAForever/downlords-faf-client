package com.faforever.client.util;

import com.faforever.client.ui.StageHolder;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Screen;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PopupUtil {

  public static void showImagePopup(Image image) {
    showImagePopup(image, 0.7);
  }

  /**
   * @param imageHeightInPercentage Image height as a percentage of the screen height size
   */
  public static void showImagePopup(Image image, double imageHeightInPercentage) {
    if (image == null) {
      log.warn("No image. Popup would not show");
      return;
    }

    Rectangle2D screenBounds = Screen.getPrimary().getBounds();
    double imageSize = screenBounds.getHeight() * imageHeightInPercentage;
    double centerScreenX = screenBounds.getWidth() / 2 - imageSize / 2;
    double centerScreenY = screenBounds.getHeight() / 2 - imageSize / 2;

    ImageView mapImageView = new ImageView(image);
    Popup popup = new Popup();

    mapImageView.getStyleClass().add("clickable");
    mapImageView.setFitHeight(imageSize);
    mapImageView.setSmooth(true);
    mapImageView.setPreserveRatio(true);
    mapImageView.setOnMouseClicked(event -> popup.hide());

    popup.setAutoHide(true);
    popup.setAutoFix(true);
    popup.getScene().setRoot(new StackPane(mapImageView));
    popup.show(StageHolder.getStage(), centerScreenX, centerScreenY);
  }

  public static Popup createPopup(Node content) {
    return createPopup(null, true, content);
  }

  public static Popup createPopup(AnchorLocation location, Node content) {
    return createPopup(location, true, content);
  }

  public static Popup createPopup(AnchorLocation location, boolean autoHide, Node content) {
    Popup popup = new Popup();
    popup.setAutoFix(true);
    popup.setAutoHide(autoHide);
    if (location != null) {
      popup.setAnchorLocation(location);
    }
    popup.getContent().setAll(content);
    return popup;
  }
}
