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
  public static final Path STEAM_FA_PATH;
  public static final Path LOCAL_FA_DATA_PATH;

  static {
    switch (OperatingSystem.current()) {
      case WINDOWS:
        GPG_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
        //If steam is every swapped to a 64x client, needs to be updated to proper directory or handling must be put in place.
        STEAM_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PROGRAM_FILESX86), "Steam", "SteamApps", "common", "Supreme Commander Forged Alliance");
        LOCAL_FA_DATA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA), "Gas Powered Games", "Supreme Commander Forged Alliance");
        break;

      default:
        GPG_FA_PATH = Paths.get(".");
        STEAM_FA_PATH = Paths.get(".");
        LOCAL_FA_DATA_PATH = Paths.get(".");
    }
  }

  private final ObjectProperty<Path> path;
  private final ObjectProperty<Path> customMapsDirectory;
  private final ObjectProperty<Path> preferencesFile;
  private final ObjectProperty<Path> officialMapsDirectory;
  private final ObjectProperty<Path> modsDirectory;
  private final IntegerProperty port;
  private final BooleanProperty autoDownloadMaps;

  public ForgedAlliancePrefs() {
    port = new SimpleIntegerProperty(6112);
    path = new SimpleObjectProperty<>();
    customMapsDirectory = new SimpleObjectProperty<>(GPG_FA_PATH.resolve("maps"));
    officialMapsDirectory = new SimpleObjectProperty<>(STEAM_FA_PATH.resolve("maps"));
    modsDirectory = new SimpleObjectProperty<>(GPG_FA_PATH.resolve("mods"));
    preferencesFile = new SimpleObjectProperty<>(LOCAL_FA_DATA_PATH.resolve("game.prefs"));
    autoDownloadMaps = new SimpleBooleanProperty(true);
  }

  public Path getPreferencesFile() {
    return preferencesFile.get();
  }

  public void setPreferencesFile(Path preferencesFile) {
    this.preferencesFile.set(preferencesFile);
  }

  public ObjectProperty<Path> preferencesFileProperty() {
    return preferencesFile;
  }

  public Path getOfficialMapsDirectory() {
    return officialMapsDirectory.get();
  }

  public void setOfficialMapsDirectory(Path officialMapsDirectory) {
    this.officialMapsDirectory.set(officialMapsDirectory);
  }

  public Path getPath() {
    return path.get();
  }

  public void setPath(Path path) {
    this.path.set(path);
  }

  public ObjectProperty<Path> pathProperty() {
    return path;
  }

  public int getPort() {
    return port.get();
  }

  public void setPort(int port) {
    this.port.set(port);
  }

  public IntegerProperty portProperty() {
    return port;
  }

  public boolean getAutoDownloadMaps() {
    return autoDownloadMaps.get();
  }

  public void setAutoDownloadMaps(boolean autoDownloadMaps) {
    this.autoDownloadMaps.set(autoDownloadMaps);
  }

  public BooleanProperty autoDownloadMapsProperty() {
    return autoDownloadMaps;
  }

  public Path getModsDirectory() {
    return modsDirectory.get();
  }

  public void setModsDirectory(Path modsDirectory) {
    this.modsDirectory.set(modsDirectory);
  }

  public ObjectProperty<Path> modsDirectoryProperty() {
    return modsDirectory;
  }

  public Path getCustomMapsDirectory() {
    return customMapsDirectory.get();
  }

  public void setCustomMapsDirectory(Path customMapsDirectory) {
    this.customMapsDirectory.set(customMapsDirectory);
  }

  public ObjectProperty<Path> customMapsDirectoryProperty() {
    return customMapsDirectory;
  }

  public ObjectProperty<Path> officialMapsDirectoryProperty() {
    return officialMapsDirectory;
  }
}
