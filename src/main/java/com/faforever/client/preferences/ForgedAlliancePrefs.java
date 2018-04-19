package com.faforever.client.preferences;

import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ForgedAlliancePrefs {

  public static final Path GPG_FA_PATH;
  public static final Path STEAM_FA_PATH;
  public static final Path LOCAL_FA_DATA_PATH;
  public static final String INIT_FILE_NAME = "init.lua";

  static {
    if (org.bridj.Platform.isWindows()) {
      GPG_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
      //If steam is every swapped to a 64x client, needs to be updated to proper directory or handling must be put in place.
      STEAM_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PROGRAM_FILESX86), "Steam", "SteamApps", "common", "Supreme Commander Forged Alliance");
      LOCAL_FA_DATA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA), "Gas Powered Games", "Supreme Commander Forged Alliance");
    } else {
      String userHome = System.getProperty("user.home");
      GPG_FA_PATH = Paths.get(userHome, "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
      STEAM_FA_PATH = Paths.get(".");
      LOCAL_FA_DATA_PATH = Paths.get(userHome, ".wine", "drive_c", "users", System.getProperty("user.name"), "Application Data", "Gas Powered Games", "Supreme Commander Forged Alliance");
    }
  }

  private final ObjectProperty<Path> path;
  private final ObjectProperty<Path> customMapsDirectory;
  private final ObjectProperty<Path> preferencesFile;
  private final ObjectProperty<Path> officialMapsDirectory;
  private final ObjectProperty<Path> modsDirectory;
  private final IntegerProperty port;
  private final BooleanProperty autoDownloadMaps;

  /**
   * String format to use when building the launch command. Takes exact one parameter; the executable path. <p>
   * Example:
   * <pre>wine "%s"</pre>
   * Results in:
   * <pre>wine "C:\Game\ForgedAlliance.exe"</pre>
   * </p>
   */
  private final StringProperty executableDecorator;
  private final ObjectProperty<Path> executionDirectory;

  public ForgedAlliancePrefs() {
    port = new SimpleIntegerProperty(6112);
    path = new SimpleObjectProperty<>();
    customMapsDirectory = new SimpleObjectProperty<>(GPG_FA_PATH.resolve("Maps"));
    officialMapsDirectory = new SimpleObjectProperty<>(STEAM_FA_PATH.resolve("Maps"));
    modsDirectory = new SimpleObjectProperty<>(GPG_FA_PATH.resolve("Mods"));
    preferencesFile = new SimpleObjectProperty<>(LOCAL_FA_DATA_PATH.resolve("Game.prefs"));
    autoDownloadMaps = new SimpleBooleanProperty(true);
    executableDecorator = new SimpleStringProperty("\"%s\"");
    executionDirectory = new SimpleObjectProperty<>();
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

  public String getExecutableDecorator() {
    return executableDecorator.get();
  }

  public void setExecutableDecorator(String executableDecorator) {
    this.executableDecorator.set(executableDecorator);
  }

  public StringProperty executableDecoratorProperty() {
    return executableDecorator;
  }

  public Path getExecutionDirectory() {
    return executionDirectory.get();
  }

  public void setExecutionDirectory(Path executionDirectory) {
    this.executionDirectory.set(executionDirectory);
  }

  public ObjectProperty<Path> executionDirectoryProperty() {
    return executionDirectory;
  }
}
