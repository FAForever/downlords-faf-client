package com.faforever.client.fx;

import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;
import java.util.function.Function;

public class StringListCell<T> extends ListCell<T> {

  private final Function<T, Image> imageFunction;
  private Function<T, String> function;
  private Pos alignment;
  private String[] cssClasses;

  public StringListCell(Function<T, String> function) {
    this(function, null);
  }

  public StringListCell(Function<T, String> function, Function<T, Image> imageFunction) {
    this(function, imageFunction, Pos.CENTER_LEFT);
  }

  public StringListCell(Function<T, String> function, Function<T, Image> imageFunction, Pos alignment, String... cssClasses) {
    this.function = function;
    this.alignment = alignment;
    this.cssClasses = cssClasses;
    this.imageFunction = imageFunction;
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      setGraphic(new ImageView(imageFunction.apply(item)));
      setText(Objects.toString(function.apply(item), ""));
      setAlignment(alignment);
      getStyleClass().addAll(cssClasses);
    }
  }
}
