package com.faforever.client.preferences;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.DirectoryChooserAction;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.util.OperatingSystem;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PreferencesService {

  private static final long STORE_DELAY = 1000;
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String PREFS_FILE_NAME = "client.prefs";
  private static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";
  private static final String USER_HOME_SUB_FOLDER = ".faforever";

  private static final Collection<Path> USUAL_GAME_PATHS = Arrays.asList(
      Paths.get(System.getenv("ProgramFiles") + "\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Paths.get(System.getenv("ProgramFiles") + " (x86)\\THQ\\Gas Powered Games\\Supreme Commander - Forged Alliance"),
      Paths.get(System.getenv("ProgramFiles") + "\\Steam\\steamapps\\common\\supreme commander forged alliance"),
      Paths.get(System.getenv("ProgramFiles") + "\\Supreme Commander - Forged Alliance")

  );

  @Autowired
  NotificationService notificationService;

  @Autowired
  I18n i18n;

  private final Path preferencesFilePath;
  private final Gson gson;
  private Preferences preferences;

  /**
   * @see #storeInBackground()
   */
  private final Timer timer;
  private TimerTask storeInBackgroundTask;
  private Repository fafRepoDirectory;

  public PreferencesService() {
    this.preferencesFilePath = getPreferencesDirectory().resolve(PREFS_FILE_NAME);
    timer = new Timer("PrefTimer", true);
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .create();
  }

  @PostConstruct
  void postConstruct() throws IOException {
    if (Files.exists(preferencesFilePath)) {
      deleteFileIfEmpty();
      readExistingFile(preferencesFilePath);
    } else {
      initDefaultPreferences();
    }

    Path path = preferences.getForgedAlliance().getPath();
    if (path == null || Files.notExists(path)) {
      logger.debug("Game path is not specified, trying to detect");
      detectGamePath();
    }
  }

  private void detectGamePath() {
    for (Path path : USUAL_GAME_PATHS) {
      if (Files.isDirectory(path)) {
        logger.debug("Found game path at {}", path);
        preferences.getForgedAlliance().setPath(path);
        storeInBackground();
        return;
      }
    }

    logger.debug("Game path could not be detected, notifying user");
    List<Action> actions = Collections.singletonList(
        new DirectoryChooserAction(i18n.get("missingGamePath.locateAction"), i18n.get("missingGamePath.chooserTitle"))
    );
    notificationService.addNotification(new PersistentNotification(i18n.get("missingGamePath.notification"), Severity.WARN, actions));
  }

  /**
   * It may happen that the file is empty when the process is forcibly killed, so remove the file if that happened.
   */
  private void deleteFileIfEmpty() throws IOException {
    if (Files.size(preferencesFilePath) == 0) {
      Files.delete(preferencesFilePath);
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

  public Path getFafReposDirectory() {
    return getFafDataDirectory().resolve("repos");
  }

  public Path getFafGameDataDirectory() {
    return getFafDataDirectory().resolve("gamedata");
  }

  public Path getMapsDirectory() {
    return Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_PERSONAL), "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance", "Maps");
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
      }
    };

    timer.schedule(storeInBackgroundTask, STORE_DELAY);
  }
}
