package com.faforever.client.io;

import lombok.EqualsAndHashCode;
import lombok.Value;

import java.net.URL;

@EqualsAndHashCode(callSuper = true)
@Value
public class ChecksumMismatchException extends Exception {
  URL item;
  String expected;
  String actual;

  public String toString() {
    return String.format("Checksums did not match for %s. Expected %s got %s", item, expected, actual);
  }
}
