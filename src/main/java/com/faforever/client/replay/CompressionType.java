package com.faforever.client.replay;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum CompressionType {
  /**
   * Base64 encoded deflate compressed stream
   */
  QTCOMPRESS(null),
  /**
   * Zstandard compressed stream
   */
  ZSTD("zstd"),
  UNKNOWN("unknown");

  @JsonValue
  private final String string;

  private static final Map<String, CompressionType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (CompressionType compressionType : values()) {
      fromString.put(compressionType.getString(), compressionType);
    }
  }

  @JsonCreator
  public static CompressionType fromString(String string) {
    return fromString.getOrDefault(string, CompressionType.UNKNOWN);
  }
}
