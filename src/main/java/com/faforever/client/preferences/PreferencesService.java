package com.faforever.client.preferences;

import com.faforever.client.game.Faction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.gson.FactionTypeAdapter;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.gson.ColorTypeAdapter;
import com.faforever.client.preferences.gson.PathTypeAdapter;
import com.faforever.client.preferences.gson.PropertyTypeAdapter;
import com.faforever.client.util.OperatingSystem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.property.Property;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class PreferencesService {

  /**
   * Points to the FAF data directory where log files, config files and others are held. The returned value varies
   * depending on the operating system.
   */
  private static final Path FAF_DATA_DIRECTORY;
  private static final long STORE_DELAY = 1000;
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String PREFS_FILE_NAME = "client.prefs";
  private static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";
  private static final String USER_HOME_SUB_FOLDER = ".faforever";
  private static final String REPLAYS_SUB_FOLDER = "replays";
  private static final String CORRUPTED_REPLAYS_SUB_FOLDER = "corrupt";
  private static final String CACHE_SUB_FOLDER = "cache";
  private static final Collection<Path> USUAL_GAME_PATHS = Arrays.asList(
      Paths.get(System.getenv("ProgramFiles") + "\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Paths.get(System.getenv("ProgramFiles") + " (x86)\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Paths.get(System.getenv("ProgramFiles") + "\\Steam\\steamapps\\common\\supreme commander forged alliance"),
      Paths.get(System.getenv("ProgramFiles") + "\\Supreme Commander - Forged Alliance")
  );
  private static final String FORGED_ALLIANCE_EXE = "ForgedAlliance.exe";
  private static final String SUPREME_COMMANDER_EXE = "SupremeCommander.exe";
  private static final Logger logger;

  static {
    switch (OperatingSystem.current()) {
      case WINDOWS:
        FAF_DATA_DIRECTORY = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
        break;

      default:
        FAF_DATA_DIRECTORY = Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
    }
  }

  static {
    System.setProperty("logDirectory", PreferencesService.FAF_DATA_DIRECTORY.resolve("logs").toString());

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    logger.debug("Logger initialized");
  }

  private final Path preferencesFilePath;
  private final Gson gson;

  /**
   * @see #storeInBackground()
   */
  private final Timer timer;
  private final Collection<PreferenceUpdateListener> updateListeners;
  @Resource
  I18n i18n;
  @Resource
  NotificationService notificationService;
  private Preferences preferences;
  private TimerTask storeInBackgroundTask;
  private OnChoseGameDirectoryListener onChoseGameDirectoryListener;

  public PreferencesService() {
    updateListeners = new ArrayList<>();
    this.preferencesFilePath = getPreferencesDirectory().resolve(PREFS_FILE_NAME);
    timer = new Timer("PrefTimer", true);
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(Property.class, PropertyTypeAdapter.INSTANCE)
        .registerTypeHierarchyAdapter(Path.class, PathTypeAdapter.INSTANCE)
        .registerTypeAdapter(Color.class, new ColorTypeAdapter())
        .registerTypeAdapter(Faction.class, FactionTypeAdapter.INSTANCE)
        .create();
  }

  public Path getPreferencesDirectory() {
    switch (OperatingSystem.current()) {
      case WINDOWS:
        return Paths.get(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER);

      default:
        return Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
    }
  }

  @PostConstruct
  void postConstruct() throws IOException {
    if (Files.exists(preferencesFilePath)) {
      deleteFileIfEmpty();
      readExistingFile(preferencesFilePath);
    } else {
      initDefaultPreferences();
    }

    Path gamePrefs = preferences.getForgedAlliance().getPreferencesFile();
    if (Files.notExists(gamePrefs)) {
      logger.info("Initializing game preferences file: {}", gamePrefs);
      Files.createDirectories(gamePrefs.getParent());
      Files.copy(getClass().getResourceAsStream("/game.prefs"), gamePrefs);
    }

    Path path = preferences.getForgedAlliance().getPath();
    if (path == null || Files.notExists(path)) {
      logger.info("Game path is not specified or non-existent, trying to detect");
      detectGamePath();
    }
  }

  private void detectGamePath() {
    for (Path path : USUAL_GAME_PATHS) {
      if (storeGamePathIfValid(path)) {
        return;
      }
    }

    logger.info("Game path could not be detected");
    notifyMissingGamePath();
  }

  private void notifyMissingGamePath() {
    List<Action> actions = Collections.singletonList(
        new Action(i18n.get("missingGamePath.locate"), event -> letUserChoseGameDirectory())
    );

    notificationService.addNotification(new PersistentNotification(i18n.get("missingGamePath.notification"), Severity.WARN, actions));
  }

  public CompletableFuture<Boolean> letUserChoseGameDirectory() {
    if (onChoseGameDirectoryListener == null) {
      throw new IllegalStateException("No listener has been specified");
    }

    return onChoseGameDirectoryListener.onChoseGameDirectory().thenApply(path -> {
      if (path == null) {
        return null;
      }
      boolean isPathValid = storeGamePathIfValid(path);

      if (!isPathValid) {
        logger.info("User specified game path does not seem to be valid: {}", path);
      }
      return isPathValid;
    }).exceptionally(throwable -> {
      logger.warn("Unexpected exception", throwable);
      return null;
    });
  }

  /**
   * Checks whether the specified path contains a ForgedAlliance.exe (either directly if the user selected the "bin"
   * directory, or in the "bin" subfolder). If the path is valid, it is stored in the preferences.
   *
   * @return {@code true} if the game path is valid, {@code false} otherwise.
   */
  private boolean storeGamePathIfValid(Path path) {
    if (path == null || !Files.isDirectory(path)) {
      return false;
    }

    if (!Files.isRegularFile(path.resolve(FORGED_ALLIANCE_EXE)) && !Files.isRegularFile(path.resolve(SUPREME_COMMANDER_EXE))) {
      return storeGamePathIfValid(path.resolve("bin"));
    }

    // At this point, path points to the "bin" directory
    Path gamePath = path.getParent();

    logger.info("Found game path at {}", gamePath);
    preferences.getForgedAlliance().setPath(gamePath);
    storeInBackground();

    Path faPathFile = getFafDataDirectory().resolve("fa_path.lua");
    try {
      Files.createDirectories(faPathFile.getParent());
      Files.write(faPathFile, String.format("fa_path = '%s'\n",
          gamePath.toAbsolutePath().toString().replace("\\", "\\\\")).getBytes(US_ASCII));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return true;
  }

  /**
   * It may happen that the file is empty when the process is forcibly killed, so remove the file if that happened.
   */
  private void deleteFileIfEmpty() throws IOException {
    if (Files.size(preferencesFilePath) == 0) {
      Files.delete(preferencesFilePath);
    }
  }

  public Path getFafBinDirectory() {
    return getFafDataDirectory().resolve("bin");
  }

  public Path getFafDataDirectory() {
    return FAF_DATA_DIRECTORY;
  }

  public Path getFafReposDirectory() {
    return getFafDataDirectory().resolve("repos");
  }

  private void initDefaultPreferences() {
    if (preferences != null) {
      throw new IllegalStateException("Preferences have already been initialized");
    }

    logger.debug("Initializing default user preferences");
    preferences = new Preferences();
  }

  private void readExistingFile(Path path) {
    if (preferences != null) {
      throw new IllegalStateException("Preferences have already been initialized");
    }

    try (Reader reader = Files.newBufferedReader(path, CHARSET)) {
      logger.debug("Reading preferences file {}", preferencesFilePath.toAbsolutePath());
      preferences = gson.fromJson(reader, Preferences.class);
    } catch (IOException e) {
      logger.warn("Preferences file " + path.toAbsolutePath() + " could not be read", e);
    }
  }

  public Preferences getPreferences() {
    return preferences;
  }

  public void store() {
    Path parent = preferencesFilePath.getParent();
    try {
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
    } catch (IOException e) {
      logger.warn("Could not create directory " + parent.toAbsolutePath(), e);
      return;
    }

    try (Writer writer = Files.newBufferedWriter(preferencesFilePath, CHARSET)) {
      logger.debug("Writing preferences file {}", preferencesFilePath.toAbsolutePath());
      gson.toJson(preferences, writer);
    } catch (IOException e) {
      logger.warn("Preferences file " + preferencesFilePath.toAbsolutePath() + " could not be written", e);
    }
  }

  /**
   * Stores the preferences in background, with a delay of {@link #STORE_DELAY}. Each subsequent call to this method
   * during that delay causes the delay to be reset. This ensures that the prefs file is written only once if multiple
   * calls occur within a short time.
   */
  public void storeInBackground() {
    if (storeInBackgroundTask != null) {
      storeInBackgroundTask.cancel();
    }

    storeInBackgroundTask = new TimerTask() {
      @Override
      public void run() {
        store();
        for (PreferenceUpdateListener updateListener : updateListeners) {
          updateListener.onPreferencesUpdated(preferences);
        }
      }
    };

    timer.schedule(storeInBackgroundTask, STORE_DELAY);
  }

  /**
   * Adds a listener to be notified whenever the preferences have been updated (that is, stored to file).
   */
  public void addUpdateListener(PreferenceUpdateListener listener) {
    updateListeners.add(listener);
  }

  public Path getCorruptedReplaysDirectory() {
    return getReplaysDirectory().resolve(CORRUPTED_REPLAYS_SUB_FOLDER);
  }

  public Path getReplaysDirectory() {
    return getFafDataDirectory().resolve(REPLAYS_SUB_FOLDER);
  }

  public void setOnChoseGameDirectoryListener(OnChoseGameDirectoryListener onChoseGameDirectoryListener) {
    this.onChoseGameDirectoryListener = onChoseGameDirectoryListener;
  }

  public Path getCacheDirectory() {
    return getFafDataDirectory().resolve(CACHE_SUB_FOLDER);
  }

  public Path getFafLogDirectory() {
    return getFafDataDirectory().resolve("logs");
  }

  public static void configureLogging() {
    // This method call causes the class to be initialized (static initializers) which in turn causes the logger to initialize.
  }
}
