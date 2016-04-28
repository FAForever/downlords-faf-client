package com.faforever.client.fx;

import javafx.geometry.Pos;
import javafx.scene.control.ListCell;

import java.util.function.Function;

public class StringListCell<T> extends ListCell<T> {

  private Function<T, String> function;
  private Pos alignment;
  private String[] cssClasses;

  public StringListCell(Function<T, String> function) {
    this(function, Pos.CENTER_LEFT);
  }

  public StringListCell(Function<T, String> function, Pos alignment, String... cssClasses) {
    this.function = function;
    this.alignment = alignment;
    this.cssClasses = cssClasses;
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setText(function.apply(item));
      setAlignment(alignment);
      getStyleClass().addAll(cssClasses);
    }
  }
}
