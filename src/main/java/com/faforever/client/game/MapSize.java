package com.faforever.client.game;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MapSize implements Comparable<MapSize> {

  private static Map<String, MapSize> cache = new HashMap<>();
  private int width;
  private int height;

  private MapSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public int getHeight() {
    return height;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  @Override
  public int compareTo(@NotNull MapSize o) {
    int dimension = width * height;
    int otherDimension = o.width * o.height;

    if (dimension == otherDimension) {
      //noinspection SuspiciousNameCombination
      return Integer.compare(width, o.width);
    }

    return Integer.compare(dimension, otherDimension);
  }

  @Override
  public int hashCode() {
    int result = width;
    result = 31 * result + height;
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

    return width == mapSize.width && height == mapSize.height;
  }

  @Override
  public String toString() {
    return String.format("%dx%d", width, height);
  }

  public static MapSize get(int width, int height) {
    String cacheKey = String.valueOf("width") + String.valueOf("height");
    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    }

    MapSize mapSize = new MapSize(width, height);
    cache.put(cacheKey, mapSize);
    return mapSize;
  }
}
