package com.faforever.client.fx;

import javafx.scene.Node;
import javafx.scene.control.TableCell;

import java.util.function.Function;

public class NodeTableCell<S, T> extends TableCell<S, T> {

  private final Function<T, ? extends Node> function;

  public NodeTableCell(Function<T, ? extends Node> function, String... cssClasses) {
    this.function = function;
    getStyleClass().addAll(cssClasses);
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    JavaFxUtil.runLater(() -> {
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
      } else {
        setGraphic(function.apply(item));
      }
    });
  }
}
