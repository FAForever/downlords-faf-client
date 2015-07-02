package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WindowPrefs {

  private final IntegerProperty width;
  private final IntegerProperty height;
  private final BooleanProperty maximized;
  private final StringProperty lastView;
  private final DoubleProperty x;
  private final DoubleProperty y;

  public WindowPrefs() {
    this.width = new SimpleIntegerProperty(800);
    this.height = new SimpleIntegerProperty(600);
    x = new SimpleDoubleProperty(-1d);
    y = new SimpleDoubleProperty(-1d);
    maximized = new SimpleBooleanProperty();
    lastView = new SimpleStringProperty();
  }

  public int getWidth() {
    return width.get();
  }

  public IntegerProperty widthProperty() {
    return width;
  }

  public void setWidth(int width) {
    this.width.set(width);
  }

  public int getHeight() {
    return height.get();
  }

  public IntegerProperty heightProperty() {
    return height;
  }

  public void setHeight(int height) {
    this.height.set(height);
  }

  public boolean getMaximized() {
    return maximized.get();
  }

  public BooleanProperty maximizedProperty() {
    return maximized;
  }

  public void setMaximized(boolean maximized) {
    this.maximized.set(maximized);
  }

  public String getLastView() {
    return lastView.get();
  }

  public StringProperty lastViewProperty() {
    return lastView;
  }

  public void setLastView(String lastView) {
    this.lastView.set(lastView);
  }

  public double getX() {
    return x.get();
  }

  public DoubleProperty xProperty() {
    return x;
  }

  public void setX(double x) {
    this.x.set(x);
  }

  public double getY() {
    return y.get();
  }

  public DoubleProperty yProperty() {
    return y;
  }

  public void setY(double y) {
    this.y.set(y);
  }
}
