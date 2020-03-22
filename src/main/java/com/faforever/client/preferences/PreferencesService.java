package com.faforever.client.preferences;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.Faction;
import com.faforever.client.preferences.gson.ColorTypeAdapter;
import com.faforever.client.preferences.gson.ExcludeFieldsWithExcludeAnnotationStrategy;
import com.faforever.client.preferences.gson.PathTypeAdapter;
import com.faforever.client.preferences.gson.PropertyTypeAdapter;
import com.faforever.client.remote.gson.FactionTypeAdapter;
import com.faforever.client.update.ClientConfiguration;
import com.github.nocatch.NoCatch.NoCatchRunnable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.collections.ObservableMap;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Service
public class PreferencesService implements InitializingBean {

  public static final String SUPREME_COMMANDER_EXE = "SupremeCommander.exe";
  public static final String FORGED_ALLIANCE_EXE = "ForgedAlliance.exe";

  /**
   * Points to the FAF data directory where log files, config files and others are held. The returned value varies
   * depending on the operating system.
   */
  private static final Path FAF_DATA_DIRECTORY;
  private static final Logger logger;
  private static final long STORE_DELAY = 1000;
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String PREFS_FILE_NAME = "client.prefs";
  private static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";
  private static final String USER_HOME_SUB_FOLDER = ".faforever";
  private static final String REPLAYS_SUB_FOLDER = "replays";
  private static final String CORRUPTED_REPLAYS_SUB_FOLDER = "corrupt";
  private static final String CACHE_SUB_FOLDER = "cache";
  private static final String CACHE_STYLESHEETS_SUB_FOLDER = Paths.get(CACHE_SUB_FOLDER, "stylesheets").toString();
  private static final Path CACHE_DIRECTORY;

  static {
    if (org.bridj.Platform.isWindows()) {
      FAF_DATA_DIRECTORY = Paths.get(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
    } else {
      FAF_DATA_DIRECTORY = Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
    }
    CACHE_DIRECTORY = FAF_DATA_DIRECTORY.resolve(CACHE_SUB_FOLDER);

    System.setProperty("logging.file", PreferencesService.FAF_DATA_DIRECTORY
        .resolve("logs")
        .resolve("downlords-faf-client.log")
        .toString());
    // duplicated, see getFafLogDirectory; make getFafLogDirectory or log dir static?
    
    System.setProperty("ICE_ADVANCED_LOG", PreferencesService.FAF_DATA_DIRECTORY
        .resolve("logs/iceAdapterLogs")
        .resolve("advanced-ice-adapter.log")
        .toString());
    // duplicated, see getIceAdapterLogDirectory; make getIceAdapterLogDirectory or ice log dir static?

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
  private final Collection<WeakReference<PreferenceUpdateListener>> updateListeners;
  private final ClientProperties clientProperties;
  private ClientConfiguration clientConfiguration;

  private Preferences preferences;
  private TimerTask storeInBackgroundTask;

  public PreferencesService(ClientProperties clientProperties) {
    this.clientProperties = clientProperties;
    updateListeners = new ArrayList<>();
    this.preferencesFilePath = getPreferencesDirectory().resolve(PREFS_FILE_NAME);
    timer = new Timer("PrefTimer", true);
    gson = new GsonBuilder()
        .setPrettyPrinting()
        .addDeserializationExclusionStrategy(new ExcludeFieldsWithExcludeAnnotationStrategy())
        .addSerializationExclusionStrategy(new ExcludeFieldsWithExcludeAnnotationStrategy())
        .registerTypeHierarchyAdapter(Property.class, PropertyTypeAdapter.INSTANCE)
        .registerTypeHierarchyAdapter(Path.class, PathTypeAdapter.INSTANCE)
        .registerTypeAdapter(Color.class, new ColorTypeAdapter())
        .registerTypeAdapter(Faction.class, FactionTypeAdapter.INSTANCE)
        .registerTypeAdapter(ObservableMap.class, FactionTypeAdapter.INSTANCE)
        .create();
  }

  public Path getPreferencesDirectory() {
    if (org.bridj.Platform.isWindows()) {
      return Paths.get(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER);
    }
    return Paths.get(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
  }

  @Override
  public void afterPropertiesSet() throws IOException {
    if (Files.exists(preferencesFilePath)) {
      if (!deleteFileIfEmpty()) {
        readExistingFile(preferencesFilePath);
      }
    } else {
      preferences = new Preferences();
    }

    Path gamePrefs = preferences.getForgedAlliance().getPreferencesFile();
    if (Files.notExists(gamePrefs)) {
      logger.info("Initializing game preferences file: {}", gamePrefs);
      Files.createDirectories(gamePrefs.getParent());
      Files.copy(getClass().getResourceAsStream("/game.prefs"), gamePrefs);
    }
  }

  /**
   * Sometimes, old preferences values are renamed or moved. The purpose of this method is to temporarily perform such
   * migrations.
   */
  private void migratePreferences(Preferences preferences) {
    if (preferences.getForgedAlliance().getPath() != null) {
      preferences.getForgedAlliance().setInstallationPath(preferences.getForgedAlliance().getPath());
      preferences.getForgedAlliance().setPath(null);
    }
    storeInBackground();
  }

  public static void configureLogging() {
    // Calling this method causes the class to be initialized (static initializers) which in turn causes the logger to initialize.
  }


  /**
   * It may happen that the file is empty when the process is forcibly killed, so remove the file if that happened.
   *
   * @return true if the file was deleted
   */
  private boolean deleteFileIfEmpty() throws IOException {
    if (Files.size(preferencesFilePath) == 0) {
      Files.delete(preferencesFilePath);
      preferences = new Preferences();
      return true;
    }
    return false;
  }

  public Path getFafBinDirectory() {
    return getFafDataDirectory().resolve("bin");
  }

  public Path getFafDataDirectory() {
    return FAF_DATA_DIRECTORY;
  }

  public Path getIceAdapterLogDirectory() {
    return getFafLogDirectory().resolve("iceAdapterLogs");
  }

  /**
   * This is the fall back location for the vault, it is set when for some reasons the game can not find the files in
   * the "My Documents" folder.
   *
   * @see com.faforever.client.vault.VaultFileSystemLocationChecker
   */
  public Path getSecondaryVaultLocation() {
    return Paths.get(FAF_DATA_DIRECTORY.toAbsolutePath().toString(), "user", "My Games", "Gas Powered Games", "Supreme Commander Forged Alliance");
  }

  public Path getPrimaryVaultLocation() {
    return ForgedAlliancePrefs.GPG_FA_PATH;
  }

  public Path getPatchReposDirectory() {
    return getFafDataDirectory().resolve("repos");
  }

  private void readExistingFile(Path path) {
    if (preferences != null) {
      throw new IllegalStateException("Preferences have already been initialized");
    }

    try (Reader reader = Files.newBufferedReader(path, CHARSET)) {
      logger.debug("Reading preferences file {}", preferencesFilePath.toAbsolutePath());
      preferences = gson.fromJson(reader, Preferences.class);
      migratePreferences(preferences);
    } catch (Exception e) {
      logger.warn("Preferences file " + path.toAbsolutePath() + " could not be read", e);
      CountDownLatch waitForUser = new CountDownLatch(1);
      Platform.runLater(() -> {
        Alert errorReading = new Alert(AlertType.ERROR, "Error reading setting. Reset settings? ", ButtonType.YES, ButtonType.CANCEL);
        errorReading.showAndWait();

        if (errorReading.getResult() == ButtonType.YES) {
          try {
            Files.delete(path);
            preferences = new Preferences();
            waitForUser.countDown();
          } catch (Exception ex) {
            logger.error("Error deleting settings file", ex);
            Alert errorDeleting = new Alert(AlertType.ERROR, MessageFormat.format("Error deleting setting. Please delete them yourself. You find them under {} .", preferencesFilePath.toAbsolutePath()), ButtonType.OK);
            errorDeleting.showAndWait();
            preferences = new Preferences();
            waitForUser.countDown();
          }
        }
      });
      noCatch((NoCatchRunnable) waitForUser::await);

    }

    if (preferences != null) {
      preferences.getForgedAlliance().bindVaultPath();
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
        ArrayList<WeakReference<PreferenceUpdateListener>> toBeRemoved = new ArrayList<>();
        for (WeakReference<PreferenceUpdateListener> updateListener : updateListeners) {
          PreferenceUpdateListener preferenceUpdateListener = updateListener.get();
          if (preferenceUpdateListener == null) {
            toBeRemoved.add(updateListener);
            continue;
          }
          preferenceUpdateListener.onPreferencesUpdated(preferences);
        }

        for (WeakReference<PreferenceUpdateListener> preferenceUpdateListenerWeakReference : toBeRemoved) {
          updateListeners.remove(preferenceUpdateListenerWeakReference);
        }
      }
    };

    timer.schedule(storeInBackgroundTask, STORE_DELAY);
  }

  /**
   * Adds a listener to be notified whenever the preferences have been updated (that is, stored to file).
   */
  public void addUpdateListener(WeakReference<PreferenceUpdateListener> listener) {
    updateListeners.add(listener);
  }

  public Path getCorruptedReplaysDirectory() {
    return getReplaysDirectory().resolve(CORRUPTED_REPLAYS_SUB_FOLDER);
  }

  public Path getReplaysDirectory() {
    return getFafDataDirectory().resolve(REPLAYS_SUB_FOLDER);
  }

  public Path getCacheDirectory() {
    return CACHE_DIRECTORY;
  }

  public Path getFafLogDirectory() {
    return getFafDataDirectory().resolve("logs");
  }

  public Path getThemesDirectory() {
    return getFafDataDirectory().resolve("themes");
  }

  @SneakyThrows
  public boolean isGamePathValid() {
    return isGamePathValidWithError(preferences.getForgedAlliance().getInstallationPath()) == null;
  }

  public String isGamePathValidWithError(Path installationPath) throws IOException, NoSuchAlgorithmException {
    boolean valid = installationPath != null && isGamePathValid(installationPath.resolve("bin"));
    if (!valid) {
      return "gamePath.select.noValidExe";
    }
    Path binPath = installationPath.resolve("bin");
    String exeHash;
    if (Files.exists(binPath.resolve(FORGED_ALLIANCE_EXE))) {
      exeHash = sha256OfFile(binPath.resolve(FORGED_ALLIANCE_EXE));
    } else {
      exeHash = sha256OfFile(binPath.resolve(SUPREME_COMMANDER_EXE));
    }
    for (String hash : clientProperties.getVanillaGameHashes()) {
      logger.debug("Hash of Supreme Commander.exe in selected User directory: " + exeHash);
      if (hash.equals(exeHash)) {
        return "gamePath.select.vanillaGameSelected";
      }
    }

    if (binPath.equals(getFafBinDirectory())) {
      return "gamePath.select.fafDataSelected";
    }

    return null;
  }

  private String sha256OfFile(Path path) throws IOException, NoSuchAlgorithmException {
    byte[] buffer = new byte[4096];
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(path.toFile()));
    DigestInputStream digestInputStream = new DigestInputStream(bis, digest);
    while (digestInputStream.read(buffer) > -1) {
    }
    digest = digestInputStream.getMessageDigest();
    digestInputStream.close();
    byte[] sha256 = digest.digest();
    StringBuilder sb = new StringBuilder();
    for (byte b : sha256) {
      sb.append(String.format("%02X", b));
    }
    return sb.toString().toUpperCase();
  }

  public boolean isGamePathValid(Path binPath) {
    return binPath != null
        && (Files.isRegularFile(binPath.resolve(FORGED_ALLIANCE_EXE))
        || Files.isRegularFile(binPath.resolve(SUPREME_COMMANDER_EXE))
    );
  }

  public Path getCacheStylesheetsDirectory() {
    return getFafDataDirectory().resolve(CACHE_STYLESHEETS_SUB_FOLDER);
  }

  public Path getLanguagesDirectory() {
    return getFafDataDirectory().resolve("languages");
  }

  public ClientConfiguration getRemotePreferences() throws IOException {
    if (clientConfiguration != null) {
      return clientConfiguration;
    }

    URL url = new URL(clientProperties.getClientConfigUrl());
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    urlConnection.setConnectTimeout((int) clientProperties.getClientConfigConnectTimeout().toMillis());

    try (Reader reader = new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8)) {
      clientConfiguration = gson.fromJson(reader, ClientConfiguration.class);
      return clientConfiguration;
    }
  }


  public CompletableFuture<ClientConfiguration> getRemotePreferencesAsync() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return getRemotePreferences();
      } catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }
}
