package com.faforever.client.map;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
public class MapSize implements Comparable<MapSize> {

  private static final float MAP_SIZE_FACTOR = 51.2f;

  private static Map<String, MapSize> cache = new HashMap<>();
  /**
   * The map width in pixels. One kilometer equals 51.2 pixels.
   */
  private final int widthInPixels;
  /**
   * The map height in pixels. One kilometer equals 51.2 pixels.
   */
  private final int heightInPixels;

  /**
   * @param widthInPixels in kilometers
   * @param heightInPixels in kilometers
   */
  private MapSize(int widthInPixels, int heightInPixels) {
    this.widthInPixels = widthInPixels;
    this.heightInPixels = heightInPixels;
  }

  public static MapSize valueOf(int widthInPixels, int heightInPixels) {
    String cacheKey = String.valueOf(widthInPixels) + String.valueOf(heightInPixels);
    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    }

    MapSize mapSize = new MapSize(widthInPixels, heightInPixels);
    cache.put(cacheKey, mapSize);
    return mapSize;
  }

  @Override
  public int compareTo(@NotNull MapSize o) {
    int dimension = widthInPixels * heightInPixels;
    int otherDimension = o.widthInPixels * o.heightInPixels;

    if (dimension == otherDimension) {
      //noinspection SuspiciousNameCombination
      return Integer.compare(widthInPixels, o.widthInPixels);
    }

    return Integer.compare(dimension, otherDimension);
  }

  @Override
  public int hashCode() {
    int result = widthInPixels;
    result = 31 * result + heightInPixels;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MapSize mapSize = (MapSize) o;

    return widthInPixels == mapSize.widthInPixels && heightInPixels == mapSize.heightInPixels;
  }

  public int getWidthInKm() {
    return (int) (widthInPixels / MAP_SIZE_FACTOR);
  }

  public int getHeightInKm() {
    return (int) (heightInPixels / MAP_SIZE_FACTOR);
  }

  @Override
  public String toString() {
    return String.format("%dx%d", widthInPixels, heightInPixels);
  }
}
