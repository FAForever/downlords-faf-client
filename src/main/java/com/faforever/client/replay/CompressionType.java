package com.faforever.client.replay;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
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

  private final String string;

  private static final Map<String, CompressionType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (CompressionType compressionType : values()) {
      fromString.put(compressionType.getString(), compressionType);
    }
  }

  public static CompressionType fromString(String string) {
    return fromString.getOrDefault(string, CompressionType.UNKNOWN);
  }

  public static class CompressionTypeAdapter extends TypeAdapter<CompressionType> {

    public static final CompressionTypeAdapter INSTANCE = new CompressionTypeAdapter();

    private CompressionTypeAdapter() {
    }

    @Override
    public void write(JsonWriter out, CompressionType compressionType) throws IOException {
      if (compressionType == null) {
        out.nullValue();
      } else {
        out.value(compressionType.getString());
      }
    }

    @Override
    public CompressionType read(JsonReader in) throws IOException {
      return CompressionType.fromString(in.nextString());
    }
  }
}
