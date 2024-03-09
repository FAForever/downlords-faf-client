package com.faforever.client.fx;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class ObservableConstant<T> implements ObservableValue<T> {

  private final T value;

  private ObservableConstant(T value) {
    this.value = value;
  }

  public static <T> ObservableConstant<T> valueOf(T value) {
    return new ObservableConstant<>(value);
  }

  @Override
  public T getValue() {
    return value;
  }

  @Override
  public void addListener(InvalidationListener observer) {}

  @Override
  public void addListener(ChangeListener<? super T> listener) {}

  @Override
  public void removeListener(InvalidationListener observer) {}

  @Override
  public void removeListener(ChangeListener<? super T> listener) {}
}
