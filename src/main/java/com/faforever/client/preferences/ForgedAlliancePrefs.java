package com.faforever.client.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.nio.file.Path;

@FieldDefaults(makeFinal=true, level= AccessLevel.PRIVATE)
public class ForgedAlliancePrefs {

  public static final String INIT_FILE_NAME = "init.lua";
  private static final String MAPS_SUB_FOLDER = "maps";
  private static final String MODS_SUB_FOLDER = "mods";

  ObjectProperty<Path> installationPath = new SimpleObjectProperty<>();
  ObjectProperty<Path> preferencesFile = new SimpleObjectProperty<>();
  ObjectProperty<Path> vaultBaseDirectory = new SimpleObjectProperty<>();
  BooleanProperty warnNonAsciiVaultPath = new SimpleBooleanProperty(true);
  BooleanProperty autoDownloadMaps = new SimpleBooleanProperty(true);
  BooleanProperty allowIpv6 = new SimpleBooleanProperty(false);

  /**
   * String format to use when building the launch command. Takes exact one parameter; the executable path. <p>
   * Example:
   * <pre>wine "%s"</pre>
   * Results in:
   * <pre>wine "C:\Game\ForgedAlliance.exe"</pre>
   * </p>
   */
  StringProperty executableDecorator = new SimpleStringProperty();
  ObjectProperty<Path> executionDirectory = new SimpleObjectProperty<>();
  BooleanProperty runFAWithDebugger = new SimpleBooleanProperty(false);
  BooleanProperty showIceAdapterDebugWindow = new SimpleBooleanProperty(false);
  ObservableSet<String> preferredCoturnIds = FXCollections.observableSet();

  /**
   * Whether the game process' priority should be set to high after launch. Enabling this may cause issues with some
   * antivirus programs, see <a href="https://github.com/FAForever/downlords-faf-client/issues/2781">#2781</a>.
   */
  BooleanProperty changeProcessPriority = new SimpleBooleanProperty(false);

  public Path getPreferencesFile() {
    return preferencesFile.get();
  }

  public void setPreferencesFile(Path preferencesFile) {
    this.preferencesFile.set(preferencesFile);
  }

  public ObjectProperty<Path> preferencesFileProperty() {
    return preferencesFile;
  }

  public boolean isAutoDownloadMaps() {
    return autoDownloadMaps.get();
  }

  public void setAutoDownloadMaps(boolean autoDownloadMaps) {
    this.autoDownloadMaps.set(autoDownloadMaps);
  }

  public BooleanProperty autoDownloadMapsProperty() {
    return autoDownloadMaps;
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

  public boolean isAllowIpv6() {
    return allowIpv6.get();
  }

  public BooleanProperty allowIpv6Property() {
    return allowIpv6;
  }

  public void setAllowIpv6(boolean allowIpv6) {
    this.allowIpv6.set(allowIpv6);
  }

  public Path getModsDirectory() {
    return getVaultBaseDirectory().resolve(MODS_SUB_FOLDER);
  }

  public Path getMapsDirectory() {
    return getVaultBaseDirectory().resolve(MAPS_SUB_FOLDER);
  }

  public boolean isRunFAWithDebugger() {
    return runFAWithDebugger.get();
  }

  public BooleanProperty runFAWithDebuggerProperty() {
    return runFAWithDebugger;
  }

  public void setRunFAWithDebugger(boolean runFAWithDebugger) {
    this.runFAWithDebugger.set(runFAWithDebugger);
  }

  public boolean isShowIceAdapterDebugWindow() {
    return showIceAdapterDebugWindow.get();
  }

  public void setShowIceAdapterDebugWindow(boolean showIceAdapterDebugWindow) {
    this.showIceAdapterDebugWindow.set(showIceAdapterDebugWindow);
  }

  public BooleanProperty showIceAdapterDebugWindow() {
    return showIceAdapterDebugWindow;
  }

  public ObservableSet<String> getPreferredCoturnIds() {
    return preferredCoturnIds;
  }

  public boolean getWarnNonAsciiVaultPath() {
    return warnNonAsciiVaultPath.get();
  }

  public void setWarnNonAsciiVaultPath(boolean warnNonAsciiVaultPath) {
    this.warnNonAsciiVaultPath.set(warnNonAsciiVaultPath);
  }

  public BooleanProperty warnNonAsciiVaultPathProperty() {
    return warnNonAsciiVaultPath;
  }

  public boolean isChangeProcessPriority() {
    return changeProcessPriority.get();
  }

  public void setChangeProcessPriority(boolean changeProcessPriority) {
    this.changeProcessPriority.set(changeProcessPriority);
  }
}
