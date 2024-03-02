package com.faforever.client.map;

import org.jetbrains.annotations.NotNull;

public record MapSize(int widthInPixels, int heightInPixels) implements Comparable<MapSize> {

  private static final float MAP_SIZE_FACTOR = 51.2f;

  @Override
  public int compareTo(@NotNull MapSize o) {
    int dimension = widthInPixels * heightInPixels;
    int otherDimension = o.widthInPixels * o.heightInPixels;

    if (dimension == otherDimension) {
      return Integer.compare(widthInPixels, o.widthInPixels);
    }

    return Integer.compare(dimension, otherDimension);
  }

  public int widthInKm() {
    return (int) (widthInPixels / MAP_SIZE_FACTOR);
  }

  public int heightInKm() {
    return (int) (heightInPixels / MAP_SIZE_FACTOR);
  }
}
