package com.faforever.client.preferences;

public class WindowPrefs {

  private int width;
  private int height;
  private boolean maximized;
  private String lastView;
  private double x;
  private double y;

  public WindowPrefs(int width, int height) {
    this.width = width;
    this.height = height;
    x = -1d;
    y = -1d;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean isMaximized() {
    return maximized;
  }

  public void setMaximized(Boolean maximized) {
    this.maximized = maximized;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public void setLastView(String lastView) {
    this.lastView = lastView;
  }

  public String getLastView() {
    return lastView;
  }

  public void setX(double x) {
    this.x = x;
  }

  public void setY(double y) {
    this.y = y;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }
}
