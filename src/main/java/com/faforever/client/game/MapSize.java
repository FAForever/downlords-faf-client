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
  public String toString() {
    return String.format("%dx%d", width, height);
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
}
