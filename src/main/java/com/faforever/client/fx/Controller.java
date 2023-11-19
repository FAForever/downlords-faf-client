package com.faforever.client.fx;

public sealed interface Controller<ROOT> permits MenuItemController, NodeController, TabController {

  ROOT getRoot();

  /** Magic method called by JavaFX after FXML has been loaded. */
  default void initialize() {

  }
}
