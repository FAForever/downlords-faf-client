package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;

public class ForgedAlliancePrefs {

  private final ObjectProperty<Path> path;
  private final IntegerProperty port;
  private final BooleanProperty autoDownloadMaps;


  public ForgedAlliancePrefs() {
    port = new SimpleIntegerProperty(6112);
    path = new SimpleObjectProperty<>();
    autoDownloadMaps = new SimpleBooleanProperty(true);
  }

  public Path getPath() {
    return path.get();
  }

  public ObjectProperty<Path> pathProperty() {
    return path;
  }

  public void setPath(Path path) {
    this.path.set(path);
  }

  public int getPort() {
    return port.get();
  }

  public IntegerProperty portProperty() {
    return port;
  }

  public void setPort(int port) {
    this.port.set(port);
  }

  public boolean getAutoDownloadMaps() {
    return autoDownloadMaps.get();
  }

  public BooleanProperty autoDownloadMapsProperty() {
    return autoDownloadMaps;
  }

  public void setAutoDownloadMaps(boolean autoDownloadMaps) {
    this.autoDownloadMaps.set(autoDownloadMaps);
  }
}
