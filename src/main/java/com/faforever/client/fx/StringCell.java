package com.faforever.client.fx;

import javafx.scene.control.TableCell;

import java.util.function.Function;

public class StringCell<S, T> extends TableCell<S, T> {

  private Function<T, String> function;

  public StringCell(Function<T, String> function) {
    this.function = function;
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setText(function.apply(item));
    }
  }
}
