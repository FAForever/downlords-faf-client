package com.faforever.client.fx;

import com.faforever.client.theme.ThemeService;
import com.google.common.base.Strings;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public class IconCell<S, T> extends TableCell<S, T> {

  private final Function<T, String> iconCssClassFunction;

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);
    setText(null);

    if (empty || item == null) {
      setGraphic(null);
    } else {
      String cssClass = iconCssClassFunction.apply(item);
      if (Strings.isNullOrEmpty(cssClass)) {
        setGraphic(null);
        return;
      }

      Region region = new Region();
      region.getStyleClass().addAll(ThemeService.CSS_CLASS_ICON, cssClass);
      setGraphic(region);
    }
  }
}
