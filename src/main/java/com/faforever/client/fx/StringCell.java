package com.faforever.client.fx;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

import java.util.function.Function;

public class StringCell<S, T> extends TableCell<S, T> {

  private Function<T, String> function;
  private Pos alignment;
  private String[] cssClasses;

  public StringCell(Function<T, String> function) {
    this(function, Pos.TOP_LEFT);
  }

  public StringCell(Function<T, String> function, Pos alignment, String... cssClasses) {
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
