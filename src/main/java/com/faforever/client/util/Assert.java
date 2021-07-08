package com.faforever.client.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
public final class Assert {

  private Assert() {
    throw new AssertionError("Not instantiatable");
  }

  public static void checkNullArgument(Object object, String message) {
    if (object == null) {
      IllegalArgumentException exception = new IllegalArgumentException(message);
      log.warn(message, exception);
      throw exception;
    }
  }

  public static void checkNullIllegalState(Object object, String message) {
    if (object == null) {
      IllegalStateException exception = new IllegalStateException(message);
      log.warn(message, exception);
      throw exception;
    }
  }

  public static void checkNullIllegalState(Object object, Supplier<String> messageSupplier) {
    if (object == null) {
      String message = messageSupplier.get();
      IllegalStateException exception = new IllegalStateException(message);
      log.warn(message, exception);
      throw exception;
    }
  }

  public static void checkNotNullIllegalState(Object object, String message) {
    if (object != null) {
      IllegalStateException exception = new IllegalStateException(message);
      log.warn(message, exception);
      throw exception;
    }
  }

  public static void checkObjectUnequalsIllegalState(Object a, Object b, String message) {
    if (!Objects.equals(a, b)) {
      IllegalStateException exception = new IllegalStateException(message);
      log.warn(message, exception);
      throw exception;
    }
  }
}
