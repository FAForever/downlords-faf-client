package com.faforever.client.fx;

import com.faforever.client.theme.ThemeService;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import static com.faforever.client.theme.ThemeService.NO_IMAGE_AVAILABLE;

@Component
@Lazy
@RequiredArgsConstructor
public class ImageViewHelper {

  private final ThemeService themeService;

  public ObservableValue<Image> createPlaceholderImageOnErrorObservable(Image image) {
    return image.errorProperty()
                .map(error -> error ? themeService.getThemeImage(ThemeService.NO_IMAGE_AVAILABLE) : image);
  }

  public void setDefaultPlaceholderImage(ImageView imageView) {
    setDefaultPlaceholderImage(imageView, false);
  }

  public void setDefaultPlaceholderImage(ImageView imageView, boolean onlyOnError) {
    setPlaceholderImage(imageView, getDefaultPlaceholderImage(), onlyOnError);
  }

  public void setPlaceholderImage(ImageView imageView, Image placeholderImage, boolean onlyOnError) {
    new ImageListenerImpl().setPlaceholderImage(imageView, placeholderImage, onlyOnError);
  }

  public Image getDefaultPlaceholderImage() {
    return themeService.getThemeImage(NO_IMAGE_AVAILABLE);
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
