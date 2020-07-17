package com.faforever.client.fx;

import javafx.scene.control.TableCell;

import java.text.DecimalFormat;
import java.util.function.Function;

public class DecimalCell<S, T extends Number> extends TableCell<S, T> {

  private final DecimalFormat format;
  private final Function<T, T> roundingFunction;
  private final String[] cssClasses;

  public DecimalCell(DecimalFormat format, Function<T, T> roundingFunction, String... cssClasses) {
    this.format = format;
    this.roundingFunction = roundingFunction;
    this.cssClasses = cssClasses;
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setText(format.format(roundingFunction.apply(item)));
      getStyleClass().addAll(cssClasses);
    }
  }
}
