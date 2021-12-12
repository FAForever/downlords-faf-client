package com.faforever.client.preferences;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.serialization.ColorMixin;
import com.faforever.client.serialization.FactionMixin;
import com.faforever.client.serialization.PathDeserializer;
import com.faforever.client.serialization.PathSerializer;
import com.faforever.client.serialization.SimpleListPropertyInstantiator;
import com.faforever.client.serialization.SimpleMapPropertyInstantiator;
import com.faforever.client.serialization.SimpleSetPropertyInstantiator;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.client.util.Assert;
import com.faforever.commons.api.dto.Faction;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.sun.jna.platform.win32.Shell32Util;
import com.sun.jna.platform.win32.ShlObj;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
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
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Lazy
@Service
public class PreferencesService implements InitializingBean {

  public static final String SUPREME_COMMANDER_EXE = "SupremeCommander.exe";
  public static final String FORGED_ALLIANCE_EXE = "ForgedAlliance.exe";

  /**
   * Points to the FAF data directory where log files, config files and others are held. The returned value varies
   * depending on the operating system.
   */
  protected static final Path FAF_DATA_DIRECTORY;
  private static final long STORE_DELAY = 1000;
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String PREFS_FILE_NAME = "client.prefs";
  private static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";
  private static final String USER_HOME_SUB_FOLDER = ".faforever";
  private static final String REPLAYS_SUB_FOLDER = "replays";
  private static final String CORRUPTED_REPLAYS_SUB_FOLDER = "corrupt";
  private static final String CACHE_SUB_FOLDER = "cache";
  private static final String FEATURED_MOD_CACHE_SUB_FOLDER = "featured_mod";
  private static final String CACHE_STYLESHEETS_SUB_FOLDER = Path.of(CACHE_SUB_FOLDER, "stylesheets").toString();
  private static final Path CACHE_DIRECTORY;
  private static final Pattern GAME_LOG_PATTERN = Pattern.compile("game(_\\d*)?.log");
  private static final int NUMBER_GAME_LOGS_STORED = 10;
  private static final Path FEATURED_MOD_CACHE_PATH;

  static {
    if (org.bridj.Platform.isWindows()) {
      FAF_DATA_DIRECTORY = Path.of(Shell32Util.getFolderPath(ShlObj.CSIDL_COMMON_APPDATA), "FAForever");
    } else {
      FAF_DATA_DIRECTORY = Path.of(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
    }
    CACHE_DIRECTORY = FAF_DATA_DIRECTORY.resolve(CACHE_SUB_FOLDER);
    FEATURED_MOD_CACHE_PATH = CACHE_DIRECTORY.resolve(FEATURED_MOD_CACHE_SUB_FOLDER);

    System.setProperty("LOG_FILE", PreferencesService.FAF_DATA_DIRECTORY
        .resolve("logs")
        .resolve("client.log")
        .toString());
    // duplicated, see getFafLogDirectory; make getFafLogDirectory or log dir static?

    System.setProperty("ICE_ADVANCED_LOG", PreferencesService.FAF_DATA_DIRECTORY
        .resolve("logs/iceAdapterLogs")
        .resolve("advanced-ice-adapter.log")
        .toString());
    // duplicated, see getIceAdapterLogDirectory; make getIceAdapterLogDirectory or ice log dir static?

    System.setProperty("MAP_GENERATOR_LOG", PreferencesService.FAF_DATA_DIRECTORY
        .resolve("logs")
        .resolve("map-generator.log")
        .toString());
    // duplicated, see getIRCLogDirectory; make getIRCLogDirectory or ice log dir static?

    System.setProperty("IRC_LOG", PreferencesService.FAF_DATA_DIRECTORY
        .resolve("logs")
        .resolve("irc.log")
        .toString());

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private final Path preferencesFilePath;
  private final ObjectMapper objectMapper;
  private final Timer storePreferencesTimer;
  private final Collection<WeakReference<PreferenceUpdateListener>> updateListeners;
  private final ClientProperties clientProperties;
  private ClientConfiguration clientConfiguration;

  private Preferences preferences;
  private TimerTask storeInBackgroundTask;

  public PreferencesService(ClientProperties clientProperties) {
    this.clientProperties = clientProperties;
    updateListeners = new ArrayList<>();
    this.preferencesFilePath = getPreferencesDirectory().resolve(PREFS_FILE_NAME);
    storePreferencesTimer = new Timer("PrefTimer", true);
    objectMapper = new ObjectMapper()
        .setSerializationInclusion(Include.NON_EMPTY)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addMixIn(Color.class, ColorMixin.class)
        .addMixIn(Faction.class, FactionMixin.class);

    TypeFactory typeFactory = objectMapper.getTypeFactory();

    Module preferencesModule = new SimpleModule()
        .addSerializer(Path.class, new PathSerializer())
        .addDeserializer(Path.class, new PathDeserializer())
        .addValueInstantiator(SimpleMapProperty.class, new SimpleMapPropertyInstantiator(objectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleMapProperty.class)))
        .addValueInstantiator(SimpleListProperty.class, new SimpleListPropertyInstantiator(objectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleListProperty.class)))
        .addValueInstantiator(SimpleSetProperty.class, new SimpleSetPropertyInstantiator(objectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleSetProperty.class)))
        .addAbstractTypeMapping(ObservableMap.class, SimpleMapProperty.class)
        .addAbstractTypeMapping(ObservableList.class, SimpleListProperty.class)
        .addAbstractTypeMapping(ObservableSet.class, SimpleSetProperty.class)
        .addAbstractTypeMapping(MapProperty.class, SimpleMapProperty.class)
        .addAbstractTypeMapping(ListProperty.class, SimpleListProperty.class)
        .addAbstractTypeMapping(SetProperty.class, SimpleSetProperty.class);

    objectMapper.registerModule(preferencesModule);
  }

  public Path getPreferencesDirectory() {
    if (org.bridj.Platform.isWindows()) {
      return Path.of(System.getenv("APPDATA")).resolve(APP_DATA_SUB_FOLDER);
    }
    return Path.of(System.getProperty("user.home")).resolve(USER_HOME_SUB_FOLDER);
  }

  @Override
  public void afterPropertiesSet() throws IOException, InterruptedException {
    if (Files.exists(preferencesFilePath)) {
      if (!deleteFileIfEmpty()) {
        readExistingFile(preferencesFilePath);
      }
    } else {
      preferences = new Preferences();
    }

    setLoggingLevel();
    JavaFxUtil.addListener(preferences.debugLogEnabledProperty(), (observable, oldValue, newValue) -> setLoggingLevel());

    Path gamePrefs = preferences.getForgedAlliance().getPreferencesFile();
    if (Files.notExists(gamePrefs)) {
      log.info("Initializing game preferences file: {}", gamePrefs);
      Files.createDirectories(gamePrefs.getParent());
      Files.copy(getClass().getResourceAsStream("/game.prefs"), gamePrefs);
    }
  }

  /**
   * Sometimes, old preferences values are renamed or moved. The purpose of this method is to temporarily perform such
   * migrations.
   */
  private void migratePreferences(Preferences preferences) {
    preferences.getLocalization().setDateFormat(preferences.getChat().getDateFormat());
    storeInBackground();
  }

  public static void configureLogging() {
    // Calling this method causes the class to be initialized (static initializers) which in turn causes the logger to initialize.
    log.debug("Logger initialized");
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
   * This is where FAF stores maps and mods. This avoids writing to the My Documents folder on Windows
   */
  public Path getFAFVaultLocation() {
    return ForgedAlliancePrefs.FAF_VAULT_PATH;
  }

  public Path getGPGVaultLocation() {
    return ForgedAlliancePrefs.GPG_VAULT_PATH;
  }

  public Path getPatchReposDirectory() {
    return getFafDataDirectory().resolve("repos");
  }

  private void readExistingFile(Path path) throws InterruptedException {
    Assert.checkNotNullIllegalState(preferences, "Preferences have already been initialized");

    try (Reader reader = Files.newBufferedReader(path, CHARSET)) {
      log.debug("Reading preferences file {}", preferencesFilePath.toAbsolutePath());
      preferences = objectMapper.readValue(reader, Preferences.class);
      migratePreferences(preferences);
    } catch (Exception e) {
      log.warn("Preferences file " + path.toAbsolutePath() + " could not be read", e);
      CountDownLatch waitForUser = new CountDownLatch(1);
      JavaFxUtil.runLater(() -> {
        Alert errorReading = new Alert(AlertType.ERROR, "Error reading setting. Reset settings? ", ButtonType.YES, ButtonType.CANCEL);
        errorReading.showAndWait();

        if (errorReading.getResult() == ButtonType.YES) {
          try {
            Files.delete(path);
            preferences = new Preferences();
            waitForUser.countDown();
          } catch (Exception ex) {
            log.error("Error deleting settings file", ex);
            Alert errorDeleting = new Alert(AlertType.ERROR, MessageFormat.format("Error deleting setting. Please delete them yourself. You find them under {} .", preferencesFilePath.toAbsolutePath()), ButtonType.OK);
            errorDeleting.showAndWait();
            preferences = new Preferences();
            waitForUser.countDown();
          }
        }
      });
      waitForUser.await();

    }
  }

  public Preferences getPreferences() {
    return preferences;
  }

  private void store() {
    Path parent = preferencesFilePath.getParent();
    try {
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
    } catch (IOException e) {
      log.warn("Could not create directory " + parent.toAbsolutePath(), e);
      return;
    }

    try (Writer writer = Files.newBufferedWriter(preferencesFilePath, CHARSET)) {
      log.debug("Writing preferences file {}", preferencesFilePath.toAbsolutePath());
      objectMapper.writeValue(writer, preferences);
    } catch (IOException e) {
      log.warn("Preferences file " + preferencesFilePath.toAbsolutePath() + " could not be written", e);
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

    storePreferencesTimer.schedule(storeInBackgroundTask, STORE_DELAY);
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

  public Path getFeaturedModCachePath() {
    return FEATURED_MOD_CACHE_PATH;
  }

  public Path getFafLogDirectory() {
    return getFafDataDirectory().resolve("logs");
  }

  public Path getThemesDirectory() {
    return getFafDataDirectory().resolve("themes");
  }

  public Path getNewGameLogFile(int gameUID) {
    try (Stream<Path> listOfLogFiles = Files.list(getFafLogDirectory())) {
      listOfLogFiles
          .filter(p -> GAME_LOG_PATTERN.matcher(p.getFileName().toString()).matches())
          .sorted(Comparator.comparingLong(p -> ((Path) p).toFile().lastModified()).reversed())
          .skip(NUMBER_GAME_LOGS_STORED - 1)
          .forEach(p -> {
            try {
              Files.delete(p);
            } catch (IOException e) {
              log.warn("Could not delete log file {}", p, e);
            }
          });
    } catch (IOException e) {
      log.error("Could not list log directory.", e);
    }
    return getFafLogDirectory().resolve(String.format("game_%d.log", gameUID));
  }

  public Optional<Path> getMostRecentGameLogFile() {
    try (Stream<Path> listOfLogFiles = Files.list(getFafLogDirectory())) {
      return listOfLogFiles
          .filter(p -> GAME_LOG_PATTERN.matcher(p.getFileName().toString()).matches()).max(Comparator.comparingLong(p -> p.toFile().lastModified()));
    } catch (IOException e) {
      log.error("Could not list log directory.", e);
    }
    return Optional.empty();
  }

  public boolean isGamePathValid() {
    Path installationPath = preferences.getForgedAlliance().getInstallationPath();
    try {
      return isGamePathValidWithErrorMessage(installationPath) == null;
    } catch (IOException e) {
      throw new GameLaunchException("Could not load installation directory " + installationPath, e, "gamePath.select.error");
    } catch (NoSuchAlgorithmException e) {
      throw new GameLaunchException("Could not compute hashes of files in installation directory " + installationPath, e, "gamePath.select.error");
    }
  }

  public String isGamePathValidWithErrorMessage(Path installationPath) throws IOException, NoSuchAlgorithmException {
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
      log.debug("Hash of Supreme Commander.exe in selected User directory: " + exeHash);
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
      clientConfiguration = objectMapper.readValue(reader, ClientConfiguration.class);
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

  public void setLoggingLevel() {
    storeInBackground();
    Level targetLogLevel = preferences.isDebugLogEnabled() ? Level.DEBUG : Level.INFO;
    final LoggerContext loggerContext = ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())).getLoggerContext();
    loggerContext.getLoggerList()
        .stream()
        .filter(logger -> logger.getName().startsWith("com.faforever"))
        .forEach(logger -> ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(logger.getName())).setLevel(targetLogLevel));

    log.info("Switching FA Forever logging configuration to {}", targetLogLevel.levelStr);
    if (targetLogLevel == Level.DEBUG) {
      log.debug("Confirming debug logging");
    }
  }
}
