package com.faforever.client.fx;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;
import java.util.function.Function;

public class StringListCell<T> extends ListCell<T> {

  private final Function<T, Image> imageFunction;
  private final ImageView imageView;
  private Function<T, String> function;
  private Pos alignment;
  private String[] cssClasses;

  public StringListCell(Function<T, String> function) {
    this(function, null);
  }

  public StringListCell(Function<T, String> function, Function<T, Image> imageFunction) {
    this(function, imageFunction, Pos.CENTER_LEFT, new ImageView());
  }

  public StringListCell(Function<T, String> function, Function<T, Image> imageFunction, Pos alignment, ImageView imageView, String... cssClasses) {
    this.function = function;
    this.alignment = alignment;
    this.cssClasses = cssClasses;
    this.imageFunction = imageFunction;

    if (imageFunction != null) {
      this.imageView = imageView;
    } else {
      this.imageView = null;
    }
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    Platform.runLater(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        if (imageView != null) {
          setGraphic(imageView);
          imageView.setImage(imageFunction.apply(item));
        }
        setText(Objects.toString(function.apply(item), ""));
        setAlignment(alignment);
        getStyleClass().addAll(cssClasses);
      }
    });
  }
}
