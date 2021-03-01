package com.faforever.client.fx;

import javafx.scene.Node;
import javafx.scene.control.TableCell;

import java.util.function.Function;

public class NodeTableCell<S, T> extends TableCell<S, T> {

  private final Function<T, ? extends Node> function;
  private final String[] cssClasses;

  public NodeTableCell(Function<T, ? extends Node> function) {
    this(function, new String[0]);
  }

  public NodeTableCell(Function<T, ? extends Node> function, String... cssClasses) {
    this.function = function;
    this.cssClasses = cssClasses;
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
        getStyleClass().addAll(cssClasses);
      }
    });
  }
}
