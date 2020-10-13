package com.faforever.client.fx;

import javafx.scene.control.TableCell;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public class StringCell<S, T> extends TableCell<S, T> {

  private final Function<T, String> function;
  private final String[] cssClasses;

  public StringCell(Function<T, String> function) {
    this(function, new String[0]);
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setText(function.apply(item));
      getStyleClass().addAll(cssClasses);
    }
  }
}
