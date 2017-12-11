package com.faforever.client.fx;

public interface Controller<ROOT> {

  ROOT getRoot();

  /** Magic method called by JavaFX after FXML has been loaded. */
  default void initialize() {

  }
}
