package com.faforever.client.fx;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

@FunctionalInterface
public interface SimpleInvalidationListener extends InvalidationListener {

  void invalidated();

  default void invalidated(Observable observable) {
      invalidated();
  }

}
