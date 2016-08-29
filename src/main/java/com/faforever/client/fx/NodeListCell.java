package com.faforever.client.fx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ListCell;

import java.util.function.Function;

public class NodeListCell<T> extends ListCell<T> {

  private final Function<T, ? extends Node> function;
  private String[] cssClasses;

  public NodeListCell(Function<T, ? extends Node> function) {
    this(function, new String[0]);
  }

  public NodeListCell(Function<T, ? extends Node> function, String... cssClasses) {
    this.function = function;
    this.cssClasses = cssClasses;
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    Platform.runLater(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        setGraphic(function.apply(item));
        getStyleClass().addAll(cssClasses);
      }
    });
  }
}
