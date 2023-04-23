package com.faforever.client.fx;

import javafx.util.StringConverter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
public class ToStringOnlyConverter<T> extends StringConverter<T> {

  @NonNull
  private final Function<T, String> toStringFunction;

  @Override
  public String toString(T object) {
    return toStringFunction.apply(object);
  }

  @Override
  public T fromString(String string) {
    throw new UnsupportedOperationException();
  }
}
