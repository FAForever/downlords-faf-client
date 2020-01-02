package com.faforever.client.preferences;

import com.faforever.client.preferences.gson.ExcludeFromGson;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.nio.file.Files;
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
      STEAM_FA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PROGRAM_FILESX86), "Steam", "steamapps", "common", "Supreme Commander Forged Alliance");
      LOCAL_FA_DATA_PATH = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_LOCAL_APPDATA), "Gas Powered Games", "Supreme Commander Forged Alliance");
    } else {
      String userHome = System.getProperty("user.home");
      GPG_FA_PATH = Paths.get(userHome, "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
      STEAM_FA_PATH = Paths.get(".");
      LOCAL_FA_DATA_PATH = Paths.get(userHome, ".wine", "drive_c", "users", System.getProperty("user.name"), "Application Data", "Gas Powered Games", "Supreme Commander Forged Alliance");
    }
  }

  /**
   * @deprecated use {@code installationPath} instead.
   */
  @Deprecated
  private final ObjectProperty<Path> path;
  private final ObjectProperty<Path> installationPath;
  private final ObjectProperty<Path> preferencesFile;
  private final ObjectProperty<Path> vaultBaseDirectory;
  @ExcludeFromGson
  private final ObjectProperty<Path> customMapsDirectory;
  @ExcludeFromGson
  private final ObjectProperty<Path> modsDirectory;
  private final BooleanProperty forceRelay;
  private final BooleanProperty autoDownloadMaps;
  /**
   * Saves if the client checked for special cases in which it needs to set the fallback vault location. See {@link
   * com.faforever.client.vault.VaultFileSystemLocationChecker}
   */
  private final BooleanProperty vaultCheckDone;

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
    forceRelay = new SimpleBooleanProperty(false);
    if (Files.isRegularFile(STEAM_FA_PATH.resolve("bin").resolve(PreferencesService.SUPREME_COMMANDER_EXE))) {
      path = new SimpleObjectProperty<>(STEAM_FA_PATH);
    } else {
      path = new SimpleObjectProperty<>();
    }
    installationPath = new SimpleObjectProperty<>();
    vaultBaseDirectory = new SimpleObjectProperty<>(GPG_FA_PATH);
    customMapsDirectory = new SimpleObjectProperty<>();
    modsDirectory = new SimpleObjectProperty<>();
    preferencesFile = new SimpleObjectProperty<>(LOCAL_FA_DATA_PATH.resolve("Game.prefs"));
    autoDownloadMaps = new SimpleBooleanProperty(true);
    executableDecorator = new SimpleStringProperty();
    executionDirectory = new SimpleObjectProperty<>();
    vaultCheckDone = new SimpleBooleanProperty(false);
    bindVaultPath();
  }

  /**
   * Needs to be called after gson deserialization again. Because otherwise the both are bound to the default vaultBaseDirectory and not the one loaded by Gson.
   */
  void bindVaultPath() {
    customMapsDirectory.unbind();
    modsDirectory.unbind();
    customMapsDirectory.bind(Bindings.createObjectBinding(() -> getVaultBaseDirectory().resolve("maps"), vaultBaseDirectory));
    modsDirectory.bind(Bindings.createObjectBinding(() -> getVaultBaseDirectory().resolve("mods"), vaultBaseDirectory));
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

  /**
   * @deprecated use {@code installationPath} instead.
   */
  @Deprecated
  public Path getPath() {
    return path.get();
  }

  /**
   * @deprecated use {@code installationPath} instead.
   */
  @Deprecated
  public void setPath(Path path) {
    this.path.set(path);
  }

  @Deprecated
  public ObjectProperty<Path> pathProperty() {
    return path;
  }

  public boolean isForceRelay() {
    return forceRelay.get();
  }

  public void setForceRelay(boolean forceRelay) {
    this.forceRelay.set(forceRelay);
  }

  public BooleanProperty forceRelayProperty() {
    return forceRelay;
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

  public Path getInstallationPath() {
    return installationPath.get();
  }

  public void setInstallationPath(Path installationPath) {
    this.installationPath.set(installationPath);
  }

  public ObjectProperty<Path> installationPathProperty() {
    return installationPath;
  }

  public Path getVaultBaseDirectory() {
    return vaultBaseDirectory.get();
  }

  public void setVaultBaseDirectory(Path vaultBaseDirectory) {
    this.vaultBaseDirectory.set(vaultBaseDirectory);
  }

  public ObjectProperty<Path> vaultBaseDirectoryProperty() {
    return vaultBaseDirectory;
  }

  public boolean isVaultCheckDone() {
    return vaultCheckDone.get();
  }

  public void setVaultCheckDone(boolean vaultCheckDone) {
    this.vaultCheckDone.set(vaultCheckDone);
  }

  public BooleanProperty vaultCheckDoneProperty() {
    return vaultCheckDone;
  }
}
