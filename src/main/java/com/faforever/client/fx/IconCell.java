package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public class IconCell<S, T> extends TableCell<S, T> {

  private final Function<T, String> iconCssClassFunction;
  private final String[] containerCssClasses;

  public IconCell(Function<T, String> iconCssClassFunction) {
    this(iconCssClassFunction, new String[0]);
  }

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
      region.getStyleClass().add(UiService.CSS_CLASS_ICON);
      region.getStyleClass().add(cssClass);
      setGraphic(region);
      getStyleClass().addAll(containerCssClasses);
    }
  }
}
