package com.faforever.client.util;

import java.util.Objects;
import java.util.function.Supplier;

public final class Assert {

  private Assert() {
    throw new AssertionError("Not instantiatable");
  }

  public static void checkNullArgument(Object object, String message) {
    if (object == null) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void checkNullIllegalState(Object object, String message) {
    if (object == null) {
      throw new IllegalStateException(message);
    }
  }

  public static void checkNullIllegalState(Object object, Supplier<String> messageSupplier) {
    if (object == null) {
      throw new IllegalStateException(messageSupplier.get());
    }
  }

  public static void checkNotNullIllegalState(Object object, String message) {
    if (object != null) {
      throw new IllegalStateException(message);
    }
  }

  public static void checkObjectUnequalsIllegalState(Object a, Object b, String message) {
    if (!Objects.equals(a, b)) {
      throw new IllegalStateException(message);
    }
  }
}
