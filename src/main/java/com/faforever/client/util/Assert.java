package com.faforever.client.util;

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

  public static void checkNotNullIllegalState(Object object, String message) {
    if (object != null) {
      throw new IllegalStateException(message);
    }
  }
}
