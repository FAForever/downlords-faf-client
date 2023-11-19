package com.faforever.client.fx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

@FunctionalInterface
public interface SimpleChangeListener<T> extends ChangeListener<T> {

  void changed(T newValue);

  @Override
  default void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
    changed(newValue);
  }

}
