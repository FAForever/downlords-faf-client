package com.faforever.client.preferences;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.error.GameLaunchException;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.serialization.ColorMixin;
import com.faforever.client.serialization.FactionMixin;
import com.faforever.client.serialization.PathDeserializer;
import com.faforever.client.serialization.PathSerializer;
import com.faforever.client.serialization.SimpleListPropertyInstantiator;
import com.faforever.client.serialization.SimpleMapPropertyInstantiator;
import com.faforever.client.serialization.SimpleSetPropertyInstantiator;
import com.faforever.client.update.ClientConfiguration;
import com.faforever.commons.api.dto.Faction;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;

import static java.nio.charset.StandardCharsets.US_ASCII;

@Slf4j
@Lazy
@Service
public class PreferencesService implements InitializingBean, DisposableBean {

  public static final String SUPREME_COMMANDER_EXE = "SupremeCommander.exe";
  public static final String FORGED_ALLIANCE_EXE = "ForgedAlliance.exe";
  public static final String APP_DATA_SUB_FOLDER = "Forged Alliance Forever";
  public static final String USER_HOME_SUB_FOLDER = ".faforever";

  private static final long STORE_DELAY = 1000;
  private static final Charset CHARSET = StandardCharsets.UTF_8;
  private static final String PREFS_FILE_NAME = "client.prefs";

  private final Path preferencesFilePath;
  private final ObjectReader configurationReader;
  private final ObjectReader preferencesUpdater;
  private final ObjectWriter preferencesWriter;
  private final ClientProperties clientProperties;
  private final Timer storePreferencesTimer = new Timer("PrefTimer", true);
  @Getter
  private final Preferences preferences = new Preferences();
  private final OperatingSystem operatingSystem;

  private ClientConfiguration clientConfiguration;
  private TimerTask storeInBackgroundTask;

  public PreferencesService(OperatingSystem operatingSystem, ClientProperties clientProperties) {
    this.clientProperties = clientProperties;
    this.operatingSystem = operatingSystem;
    this.preferencesFilePath = operatingSystem.getPreferencesDirectory().resolve(PREFS_FILE_NAME);

    ObjectMapper objectMapper = buildObjectMapper();
    preferencesUpdater = objectMapper.readerForUpdating(preferences);
    preferencesWriter = objectMapper.writerFor(Preferences.class);
    configurationReader = objectMapper.readerFor(ClientConfiguration.class);
  }

  private ObjectMapper buildObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(Include.NON_EMPTY)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addMixIn(Color.class, ColorMixin.class)
        .addMixIn(Faction.class, FactionMixin.class);

    TypeFactory typeFactory = objectMapper.getTypeFactory();

    Module preferencesModule = new SimpleModule().addSerializer(Path.class, new PathSerializer())
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
    return objectMapper;
  }

  @Override
  public void afterPropertiesSet() throws IOException, InterruptedException {
    initializePreferences();

    if (Files.exists(preferencesFilePath)) {
      if (!deleteFileIfEmpty()) {
        readExistingFile(preferencesFilePath);
      }
    }

    Path gamePrefs = preferences.getForgedAlliance().getPreferencesFile();
    if (Files.notExists(gamePrefs)) {
      log.info("Initializing game preferences file: `{}`", gamePrefs);
      Files.createDirectories(gamePrefs.getParent());
      Files.copy(getClass().getResourceAsStream("/game.prefs"), gamePrefs);
    }
  }

  @Override
  public void destroy() throws Exception {
    if (storeInBackgroundTask != null) {
      storeInBackgroundTask.cancel();
    }
    store();
  }

  private void initializePreferences() {
    preferences.getData().setBaseDataDirectory(operatingSystem.getDefaultDataDirectory());
    ForgedAlliancePrefs forgedAlliance = preferences.getForgedAlliance();
    forgedAlliance.setVaultBaseDirectory(operatingSystem.getDefaultVaultDirectory());
    forgedAlliance.setInstallationPath(operatingSystem.getSteamFaDirectory());
    forgedAlliance.setPreferencesFile(operatingSystem.getLocalFaDataPath().resolve("Game.prefs"));
  }

  /**
   * Sometimes, old preferences values are renamed or moved. The purpose of this method is to temporarily perform such
   * migrations.
   */
  private void migratePreferences(Preferences preferences) {
    preferences.getLocalization().setDateFormat(preferences.getChat().getDateFormat());
    storeInBackground();
  }


  /**
   * It may happen that the file is empty when the process is forcibly killed, so remove the file if that happened.
   *
   * @return true if the file was deleted
   */
  private boolean deleteFileIfEmpty() throws IOException {
    if (Files.size(preferencesFilePath) == 0) {
      Files.delete(preferencesFilePath);
      return true;
    }
    return false;
  }

  private void readExistingFile(Path path) throws InterruptedException {
    try (Reader reader = Files.newBufferedReader(path, CHARSET)) {
      log.info("Reading preferences file `{}`", preferencesFilePath.toAbsolutePath());
      preferencesUpdater.readValue(reader);
      migratePreferences(preferences);
    } catch (Exception e) {
      log.warn("Preferences file `{}` could not be read", path, e);
      CountDownLatch waitForUser = new CountDownLatch(1);
      JavaFxUtil.runLater(() -> {
        Alert errorReading = new Alert(AlertType.ERROR, "Error reading setting. Reset settings? ", ButtonType.YES, ButtonType.CANCEL);
        errorReading.showAndWait();

        if (errorReading.getResult() == ButtonType.YES) {
          try {
            Files.delete(path);
            waitForUser.countDown();
          } catch (Exception ex) {
            log.error("Error deleting settings file", ex);
            Alert errorDeleting = new Alert(AlertType.ERROR, MessageFormat.format("Error deleting setting. Please delete them yourself. You find them under {} .", preferencesFilePath.toAbsolutePath()), ButtonType.OK);
            errorDeleting.showAndWait();
            waitForUser.countDown();
          }
        }
      });
      waitForUser.await();
    }
  }

  private void store() {
    Path parent = preferencesFilePath.getParent();
    try {
      if (!Files.exists(parent)) {
        Files.createDirectories(parent);
      }
    } catch (IOException e) {
      log.warn("Could not create directory `{}`", parent, e);
      return;
    }

    try (Writer writer = Files.newBufferedWriter(preferencesFilePath, CHARSET)) {
      log.trace("Writing preferences file `{}`", preferencesFilePath.toAbsolutePath());
      preferencesWriter.writeValue(writer, preferences);
    } catch (IOException e) {
      log.error("Preferences file `{}` could not be written", preferencesFilePath.toAbsolutePath(), e);
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

    storePreferencesTimer.schedule(storeInBackgroundTask, STORE_DELAY);
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
      log.info("Hash of Supreme Commander.exe in selected User directory: " + exeHash);
      if (hash.equals(exeHash)) {
        return "gamePath.select.vanillaGameSelected";
      }
    }

    if (binPath.equals(preferences.getData().getBinDirectory())) {
      return "gamePath.select.fafDataSelected";
    }

    return null;
  }

  private String sha256OfFile(Path path) throws IOException, NoSuchAlgorithmException {
    byte[] buffer = new byte[4096];
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    BufferedInputStream bufferedFileStream = new BufferedInputStream(new FileInputStream(path.toFile()));
    DigestInputStream digestInputStream = new DigestInputStream(bufferedFileStream, digest);
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
    return binPath != null && (Files.isRegularFile(binPath.resolve(FORGED_ALLIANCE_EXE)) || Files.isRegularFile(binPath.resolve(SUPREME_COMMANDER_EXE)));
  }

  public boolean isVaultBasePathInvalidForAscii() {
    Path vaultBaseDirectory = preferences.getForgedAlliance().getVaultBaseDirectory();
    return vaultBaseDirectory != null && !US_ASCII.newEncoder().canEncode(vaultBaseDirectory.toString());
  }

  private ClientConfiguration getRemotePreferences() throws IOException {
    if (clientConfiguration != null) {
      return clientConfiguration;
    }

    URL url = new URL(clientProperties.getClientConfigUrl());
    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
    urlConnection.setConnectTimeout((int) clientProperties.getClientConfigConnectTimeout().toMillis());

    try (Reader reader = new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8)) {
      clientConfiguration = configurationReader.readValue(reader);
      return clientConfiguration;
    }
  }


  @Cacheable(value = CacheNames.REMOTE_CONFIG, sync = true)
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
