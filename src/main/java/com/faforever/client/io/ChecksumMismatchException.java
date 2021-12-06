package com.faforever.client.io;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChecksumMismatchException extends Exception {
  private final Object item;
  private final String expected;
  private final String actual;


  public String toString() {
    return String.format("Checksums did not match for %s. Expected %s got %s", item, expected, actual);
  }
}
