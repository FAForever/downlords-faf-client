package com.faforever.client.fx;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;

import java.util.Objects;
import java.util.function.Function;

public class StringListCell<T> extends ListCell<T> {

  private final Function<T, Node> graphicFunction;
  private final Function<T, String> function;
  private final Pos alignment;
  private final String[] cssClasses;

  public StringListCell(Function<T, String> function) {
    this(function, null);
  }

  public StringListCell(Function<T, String> function, Function<T, Node> graphicFunction) {
    this(function, graphicFunction, Pos.CENTER_LEFT);
  }

  public StringListCell(Function<T, String> function, Function<T, Node> graphicFunction, Pos alignment, String... cssClasses) {
    this.function = function;
    this.alignment = alignment;
    this.cssClasses = cssClasses;
    this.graphicFunction = graphicFunction;
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    JavaFxUtil.runLater(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        if (graphicFunction != null) {
          setGraphic(graphicFunction.apply(item));
        }
        setText(Objects.toString(function.apply(item), ""));
        setAlignment(alignment);
        getStyleClass().addAll(cssClasses);
      }
    });
  }
}
