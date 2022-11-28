package com.faforever.client.fx;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageViewHelper {

  public static void setPlaceholderImage(ImageView imageView, Image placeholderImage) {
    setPlaceholderImage(imageView, placeholderImage, false);
  }

  public static void setPlaceholderImage(ImageView imageView, Image placeholderImage, boolean onlyOnError) {
    new ImageListenerImpl().setPlaceholderImage(imageView, placeholderImage, onlyOnError);
  }

  private static class ImageListenerImpl {

    private ImageView imageView;
    private Image placeholderImage;

    private final InvalidationListener progressListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        Image image = imageView.getImage();
        if (image != null && image.getProgress() == 1.0) {
          if (image.isError()) {
            imageView.setImage(placeholderImage);
          }
          JavaFxUtil.removeListener(image.progressProperty(), this);
        }
      }
    };

    private final InvalidationListener imageListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        Image image = imageView.getImage();
        if (image != null && image.getUrl() != null && !image.getUrl().startsWith("file:")) {
          if (image.getProgress() == 1.0) {
            if (image.isError()) {
              imageView.setImage(placeholderImage);
            }
          } else {
            JavaFxUtil.addAndTriggerListener(image.progressProperty(), progressListener);
          }
        }
      }
    };

    public void setPlaceholderImage(ImageView imageView, Image placeholderImage, boolean onlyOnError) {
      if (!onlyOnError) {
        imageView.setImage(placeholderImage);
      }

      this.imageView = imageView;
      this.placeholderImage = placeholderImage;
      JavaFxUtil.addListener(imageView.imageProperty(), imageListener);
    }
  }
}
