package com.faforever.client.fx;

import javafx.scene.control.TableCell;

import java.util.function.BiConsumer;

public class ControllerTableCell<S, T, C extends NodeController<?>> extends TableCell<S, T> {

  private final BiConsumer<C, T> updateConsumer;
  private final C controller;

  public ControllerTableCell(C controller, BiConsumer<C, T> updateConsumer, String... cssClasses) {
    this.updateConsumer = updateConsumer;
    this.controller = controller;
    getStyleClass().addAll(cssClasses);
  }

  @Override
  protected void updateItem(T item, boolean empty) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      setText(null);
      setGraphic(null);
    } else {
      updateConsumer.accept(controller, item);
      setGraphic(controller.getRoot());
    }
  }
}
