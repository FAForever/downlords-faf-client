package com.faforever.client.fx;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ImageViewHelper {

  public static void setPlaceholderImage(ImageView imageView, Image defaultImage) {
    setPlaceholderImage(imageView, defaultImage, false);
  }

  public static void setPlaceholderImage(ImageView imageView, Image defaultImage, boolean onlyOnError) {
    new ImageListenerImpl().setPlaceholderImage(imageView, defaultImage, onlyOnError);
  }

  private static class ImageListenerImpl {

    private ImageView imageView;
    private Image defaultImage;

    private final InvalidationListener progressListener = new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        Image image = imageView.getImage();
        if (image != null && image.getProgress() == 1.0) {
          if (image.isError()) {
            imageView.setImage(defaultImage);
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
              imageView.setImage(defaultImage);
            }
          } else {
            JavaFxUtil.addAndTriggerListener(image.progressProperty(), progressListener);
          }
        }
      }
    };

    public void setPlaceholderImage(ImageView imageView, Image defaultImage, boolean onlyOnError) {
      if (!onlyOnError) {
        imageView.setImage(defaultImage);
      }

      this.imageView = imageView;
      this.defaultImage = defaultImage;
      JavaFxUtil.addListener(imageView.imageProperty(), imageListener);
    }
  }
}
