package com.faforever.client.preferences;

import com.faforever.client.main.event.NavigationItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;


public class WindowPrefs {

  private final IntegerProperty width = new SimpleIntegerProperty(800);
  private final IntegerProperty height = new SimpleIntegerProperty(600);
  private final BooleanProperty maximized = new SimpleBooleanProperty();
  private final ObjectProperty<NavigationItem> navigationItem = new SimpleObjectProperty<>();
  private final DoubleProperty x = new SimpleDoubleProperty(-1d);
  private final DoubleProperty y = new SimpleDoubleProperty(-1d);
  private final ObjectProperty<Path> backgroundImagePath = new SimpleObjectProperty<>();

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

  public ObjectProperty<Path> backgroundImagePathProperty() {
    return backgroundImagePath;
  }

  public Path getBackgroundImagePath() {
    return backgroundImagePath.getValue();
  }

  public void setBackgroundImagePath(Path path) {
    this.backgroundImagePath.setValue(path);
  }

  public NavigationItem getNavigationItem() {
    return navigationItem.get();
  }

  public void setNavigationItem(NavigationItem navigationItem) {
    this.navigationItem.set(navigationItem);
  }

  public ObjectProperty<NavigationItem> navigationItemProperty() {
    return navigationItem;
  }
}
