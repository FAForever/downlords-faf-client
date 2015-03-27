package com.faforever.client.util;

public interface Callback<T> {

  void success(T result);

  void error(Throwable e);
}
