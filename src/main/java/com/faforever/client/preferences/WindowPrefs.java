package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.nio.file.Path;

public class WindowPrefs {

  private final IntegerProperty width;
  private final IntegerProperty height;
  private final BooleanProperty maximized;
  private final StringProperty lastView;
  private final MapProperty<String, String> lastChildViews;
  private final DoubleProperty x;
  private final DoubleProperty y;
  private final ObjectProperty<Path> backgroundImagePath;

  public WindowPrefs() {
    this.width = new SimpleIntegerProperty(800);
    this.height = new SimpleIntegerProperty(600);
    x = new SimpleDoubleProperty(-1d);
    y = new SimpleDoubleProperty(-1d);
    maximized = new SimpleBooleanProperty();
    lastView = new SimpleStringProperty();
    lastChildViews = new SimpleMapProperty<>(FXCollections.observableHashMap());
    backgroundImagePath = new SimpleObjectProperty<>();
  }

  public int getWidth() {
    return width.get();
  }

  public void setWidth(int width) {
    this.width.set(width);
  }

  public IntegerProperty widthProperty() {
    return width;
  }

  public int getHeight() {
    return height.get();
  }

  public void setHeight(int height) {
    this.height.set(height);
  }

  public IntegerProperty heightProperty() {
    return height;
  }

  public boolean getMaximized() {
    return maximized.get();
  }

  public void setMaximized(boolean maximized) {
    this.maximized.set(maximized);
  }

  public BooleanProperty maximizedProperty() {
    return maximized;
  }

  public String getLastView() {
    return lastView.get();
  }

  public void setLastView(String lastView) {
    this.lastView.set(lastView);
  }

  public StringProperty lastViewProperty() {
    return lastView;
  }

  public double getX() {
    return x.get();
  }

  public void setX(double x) {
    this.x.set(x);
  }

  public DoubleProperty xProperty() {
    return x;
  }

  public double getY() {
    return y.get();
  }

  public void setY(double y) {
    this.y.set(y);
  }

  public DoubleProperty yProperty() {
    return y;
  }

  public ObservableMap<String, String> getLastChildViews() {
    return lastChildViews.get();
  }

  public void setLastChildViews(ObservableMap<String, String> lastChildViews) {
    this.lastChildViews.set(lastChildViews);
  }

  public MapProperty<String, String> lastChildViewsProperty() {
    return lastChildViews;
  }

  public ObjectProperty<Path> backgroundImagePathProperty() {
    return backgroundImagePath;
  }

  public Path getBackgroundImagePath() {
    return backgroundImagePath.getValue();
  }

  public void setBackgroundImagePath(Path path) {
    this.backgroundImagePath.setValue(path);
  }
}
