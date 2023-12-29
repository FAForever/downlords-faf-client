package com.faforever.client.fx;

import com.faforever.client.theme.ThemeService;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
@RequiredArgsConstructor
public class ImageViewHelper {

  private final ThemeService themeService;

  public ObservableValue<Image> createPlaceholderImageOnErrorObservable(Image image) {
    return image.errorProperty()
                .map(error -> error ? themeService.getThemeImage(ThemeService.NO_IMAGE_AVAILABLE) : image);
  }
}
