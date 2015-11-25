package com.faforever.client.game;

import org.jetbrains.annotations.NotNull;

public class MapSize implements Comparable<MapSize> {

  private int width;
  private int height;

  public MapSize(int width, int height) {
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

    if (width != mapSize.width) {
      return false;
    }
    return height == mapSize.height;

  }

  @Override
  public String toString() {
    return String.format("%dx%d", width, height);
  }
}
