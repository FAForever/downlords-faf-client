package com.faforever.client.fx;

import com.faforever.client.theme.UiService;
import com.google.common.base.Strings;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.layout.Region;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

public class IconCell<S, T> extends TableCell<S, T> {

  private final Function<T, String> iconCssClassFunction;
  private final Pos alignment;
  private final String[] containerCssClasses;

  public IconCell(Function<T, String> iconCssClassFunction) {
    this(iconCssClassFunction, Pos.CENTER_LEFT);
  }

  public IconCell(Function<T, String> iconCssClassFunction, Pos alignment, String... containerCssClasses) {
    this.iconCssClassFunction = iconCssClassFunction;
    this.alignment = alignment;
    this.containerCssClasses = containerCssClasses;
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
      setAlignment(alignment);
      getStyleClass().addAll(containerCssClasses);
    }
  }
}
