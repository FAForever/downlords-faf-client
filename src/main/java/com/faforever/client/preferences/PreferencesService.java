package com.faforever.client.preferences;

import com.faforever.client.util.OperatingSystem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

public class PreferencesService {

  public static final long STORE_DELAY = 1000;

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String PREFS_FILE_NAME = "client.prefs";
  private static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";
  private static final String USER_HOME_SUB_FOLDER = ".faforever";

  private final Path preferencesFilePath;
  private final Gson gson;
  private Preferences preferences;
  private final Timer timer;
  private TimerTask storeInBackgroundTask;

  public PreferencesService() {
    this.preferencesFilePath = getPreferencesFilePath();
    timer = new Timer("PrefTimer", true);
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();
  }

  @PostConstruct
  void init() {
    if (Files.exists(preferencesFilePath)) {
      readExistingFile(preferencesFilePath);
    } else {
      initDefaultPreferences();
    }
  }

  public Path getPreferencesDirectory() {
    switch (OperatingSystem.current()) {
      case WINDOWS:
        return Paths.get(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER);

      default:
        return Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
    }
  }

  public Path getFafDataDirectory() {
    return Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
  }


  public Path getFafBinDirectory() {
    return getFafDataDirectory().resolve("bin");
  }

  private Path getPreferencesFilePath() {
    return getPreferencesDirectory().resolve(PREFS_FILE_NAME);
  }

  public Path getMapsDirectory() {
    return Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance", "Maps");
  }

  private void initDefaultPreferences() {
    logger.debug("Initializing default user preferences");
    preferences = new Preferences();
  }

  private void readExistingFile(Path path) {
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
      }
    };

    timer.schedule(storeInBackgroundTask, STORE_DELAY);
  }
}
