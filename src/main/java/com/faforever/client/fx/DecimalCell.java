package com.faforever.client.fx;

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;

import java.text.DecimalFormat;
import java.util.function.Function;

public class DecimalCell<S, T extends Number> extends TableCell<S, T> {

  private DecimalFormat format;
  private Function<T, T> roundingFunction;
  private Pos alignment;
  private String[] cssClasses;

  public DecimalCell(DecimalFormat format, Function<T, T> roundingFunction) {
    this(format, roundingFunction, Pos.CENTER_RIGHT);
  }

  public DecimalCell(DecimalFormat format, Function<T, T> roundingFunction, Pos alignment, String... cssClasses) {
    this.format = format;
    this.roundingFunction = roundingFunction;
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
      setText(format.format(roundingFunction.apply(item)));
      setAlignment(alignment);
      getStyleClass().addAll(cssClasses);
    }
  }
}
