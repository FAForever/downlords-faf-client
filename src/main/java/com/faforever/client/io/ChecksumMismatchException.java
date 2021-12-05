package com.faforever.client.io;

import lombok.Getter;

public class ChecksumMismatchException extends Exception {
  @Getter
  private final Object item;
  @Getter
  private final String expected;
  @Getter
  private final String actual;

  public ChecksumMismatchException(Object item, String expected, String actual) {
    this.item = item;
    this.expected = expected;
    this.actual = actual;
  }

  public String toString() {
    return String.format("Checksums did not match for %s. Expected %s got %s", item, expected, actual);
  }
}
