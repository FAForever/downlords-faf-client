package com.faforever.client.preferences;

import com.faforever.client.util.OperatingSystem;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ForgedAlliancePrefs {

  public static final Path GPG_FA_PATH;
  public static final Path FAF_GAME_PATH;

  static {
    switch (OperatingSystem.current()) {
      case WINDOWS:
        GPG_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
        FAF_GAME_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
        break;

      default:
        GPG_FA_PATH = Paths.get(".");
        FAF_GAME_PATH = Paths.get(".");
    }
  }

  private final ObjectProperty<Path> path;
  private final ObjectProperty<Path> mapsDirectory;
  private final ObjectProperty<Path> modsDirectory;
  private final IntegerProperty port;
  private final BooleanProperty autoDownloadMaps;


  public ForgedAlliancePrefs() {
    port = new SimpleIntegerProperty(6112);
    path = new SimpleObjectProperty<>();
    mapsDirectory = new SimpleObjectProperty<>(GPG_FA_PATH.resolve("maps"));
    modsDirectory = new SimpleObjectProperty<>(GPG_FA_PATH.resolve("mods"));
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

  public Path getModsDirectory() {
    return modsDirectory.get();
  }

  public ObjectProperty<Path> modsDirectoryProperty() {
    return modsDirectory;
  }

  public void setModsDirectory(Path modsDirectory) {
    this.modsDirectory.set(modsDirectory);
  }

  public Path getMapsDirectory() {
    return mapsDirectory.get();
  }

  public ObjectProperty<Path> mapsDirectoryProperty() {
    return mapsDirectory;
  }

  public void setMapsDirectory(Path mapsDirectory) {
    this.mapsDirectory.set(mapsDirectory);
  }
}
