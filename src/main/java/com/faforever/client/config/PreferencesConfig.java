package com.faforever.client.config;

import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.DeveloperPrefs;
import com.faforever.client.preferences.FiltersPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.GeneralPrefs;
import com.faforever.client.preferences.GeneratorPrefs;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.LiveReplaySearchPrefs;
import com.faforever.client.preferences.LocalizationPrefs;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.MatchmakerPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.ReplayHistoryPrefs;
import com.faforever.client.preferences.UserPrefs;
import com.faforever.client.preferences.VaultPrefs;
import com.faforever.client.preferences.WindowPrefs;
import com.faforever.client.serialization.ColorMixin;
import com.faforever.client.serialization.FactionMixin;
import com.faforever.client.serialization.PathDeserializer;
import com.faforever.client.serialization.PathSerializer;
import com.faforever.client.serialization.SimpleListPropertyInstantiator;
import com.faforever.client.serialization.SimpleMapPropertyInstantiator;
import com.faforever.client.serialization.SimpleSetPropertyInstantiator;
import com.faforever.commons.api.dto.Faction;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class PreferencesConfig implements DisposableBean {

  private static final String PREFS_FILE_NAME = "client.prefs";

  private final Path preferencesFilePath;
  private final ObjectMapper preferencesObjectMapper;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final Preferences preferences;

  public PreferencesConfig(OperatingSystem operatingSystem, ObjectMapper objectMapper,
                           FxApplicationThreadExecutor fxApplicationThreadExecutor) throws IOException, InterruptedException {
    this.fxApplicationThreadExecutor = fxApplicationThreadExecutor;

    preferencesObjectMapper = configureObjectMapper(objectMapper);
    preferencesFilePath = operatingSystem.getPreferencesDirectory().resolve(PREFS_FILE_NAME);

    preferences = readExistingPreferences(preferencesFilePath, operatingSystem);
  }

  @Bean
  public Preferences preferences() {
    return preferences;
  }

  @Bean
  public GeneralPrefs general() {
    return preferences().getGeneral();
  }

  @Bean
  public DataPrefs data() {
    return preferences().getData();
  }

  @Bean
  public WindowPrefs window() {
    return preferences().getMainWindow();
  }

  @Bean
  public GeneratorPrefs generator() {
    return preferences().getGenerator();
  }

  @Bean
  public ForgedAlliancePrefs forgedAlliance() {
    return preferences().getForgedAlliance();
  }

  @Bean
  public LoginPrefs login() {
    return preferences().getLogin();
  }

  @Bean
  public ChatPrefs chat() {
    return preferences().getChat();
  }

  @Bean
  public NotificationPrefs notification() {
    return preferences().getNotification();
  }

  @Bean
  public LocalizationPrefs localization() {
    return preferences().getLocalization();
  }

  @Bean
  public LastGamePrefs lastGame() {
    return preferences().getLastGame();
  }

  @Bean
  public MatchmakerPrefs matchmaker() {
    return preferences().getMatchmaker();
  }

  @Bean
  public DeveloperPrefs developer() {
    return preferences().getDeveloper();
  }

  @Bean
  public VaultPrefs vault() {
    return preferences().getVault();
  }

  @Bean
  public LiveReplaySearchPrefs liveReplaySearchPrefs() {
    return preferences().getVault().getLiveReplaySearch();
  }

  @Bean
  public UserPrefs user() {
    return preferences().getUser();
  }

  @Bean
  public FiltersPrefs filters() {
    return preferences().getFilters();
  }

  @Bean
  public ReplayHistoryPrefs replayHistory() {return preferences().getReplayHistory();}

  private ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
    ObjectMapper configuredObjectMapper = objectMapper.copy().setSerializationInclusion(Include.NON_EMPTY)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addMixIn(Color.class, ColorMixin.class)
        .addMixIn(Faction.class, FactionMixin.class);

    TypeFactory typeFactory = configuredObjectMapper.getTypeFactory();

    Module preferencesModule = new SimpleModule().addSerializer(Path.class, new PathSerializer())
        .addDeserializer(Path.class, new PathDeserializer())
        .addValueInstantiator(SimpleMapProperty.class, new SimpleMapPropertyInstantiator(configuredObjectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleMapProperty.class)))
        .addValueInstantiator(SimpleListProperty.class, new SimpleListPropertyInstantiator(configuredObjectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleListProperty.class)))
        .addValueInstantiator(SimpleSetProperty.class, new SimpleSetPropertyInstantiator(configuredObjectMapper.getDeserializationConfig(), typeFactory.constructType(SimpleSetProperty.class)))
        .addAbstractTypeMapping(ObservableMap.class, SimpleMapProperty.class)
        .addAbstractTypeMapping(ObservableList.class, SimpleListProperty.class)
        .addAbstractTypeMapping(ObservableSet.class, SimpleSetProperty.class)
        .addAbstractTypeMapping(MapProperty.class, SimpleMapProperty.class)
        .addAbstractTypeMapping(ListProperty.class, SimpleListProperty.class)
        .addAbstractTypeMapping(SetProperty.class, SimpleSetProperty.class);

    configuredObjectMapper.registerModule(preferencesModule);
    return configuredObjectMapper;
  }

  /**
   * It may happen that the file is empty when the process is forcibly killed, so remove the file if that happened.
   *
   * @return true if the file was deleted
   */
  private boolean deleteFileIfEmpty(Path path) throws IOException {
    if (Files.size(path) == 0) {
      Files.delete(path);
      return true;
    }
    return false;
  }

  private Preferences readExistingPreferences(Path path, OperatingSystem operatingSystem) throws IOException {
    if (!Files.exists(path) || deleteFileIfEmpty(path)) {
      return constructDefaultPrefernces(operatingSystem);
    }

    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      log.info("Reading preferences file `{}`", path.toAbsolutePath());
      Preferences preferences = preferencesObjectMapper.readValue(reader, Preferences.class);
      migratePreferences(preferences);
      return preferences;
    } catch (Exception e) {
      log.warn("Preferences file `{}` could not be read", path, e);
      fxApplicationThreadExecutor.executeAndWait(() -> {
        Alert errorReading = new Alert(AlertType.ERROR, "Error reading setting. Reset settings? ", ButtonType.YES, ButtonType.CANCEL);
        errorReading.showAndWait();

        if (errorReading.getResult() == ButtonType.YES) {
          try {
            Files.delete(path);
          } catch (Exception ex) {
            log.error("Error deleting settings file", ex);
            Alert errorDeleting = new Alert(AlertType.ERROR, MessageFormat.format("Error deleting setting. Please delete them yourself. You find them under {} .", path.toAbsolutePath()), ButtonType.OK);
            errorDeleting.showAndWait();
          }
        }
      });

      return constructDefaultPrefernces(operatingSystem);
    }
  }

  @NotNull
  private Preferences constructDefaultPrefernces(OperatingSystem operatingSystem) {
    Preferences preferences = new Preferences();
    preferences.getData().setBaseDataDirectory(operatingSystem.getDefaultDataDirectory());
    ForgedAlliancePrefs forgedAlliance = preferences.getForgedAlliance();
    forgedAlliance.setVaultBaseDirectory(operatingSystem.getDefaultVaultDirectory());
    forgedAlliance.setInstallationPath(operatingSystem.getSteamFaDirectory());
    forgedAlliance.setPreferencesFile(operatingSystem.getLocalFaDataPath().resolve("Game.prefs"));
    return preferences;
  }

  /**
   * Sometimes, old preferences values are renamed or moved. The purpose of this method is to temporarily perform such
   * migrations.
   */
  private void migratePreferences(Preferences preferences) {

  }

  @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
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

    try (Writer writer = Files.newBufferedWriter(preferencesFilePath, StandardCharsets.UTF_8)) {
      log.info("Writing preferences file `{}`", preferencesFilePath.toAbsolutePath());
      preferencesObjectMapper.writeValue(writer, preferences);
    } catch (IOException e) {
      log.error("Preferences file `{}` could not be written", preferencesFilePath.toAbsolutePath(), e);
    }
  }


  @Override
  public void destroy() throws Exception {
    store();
  }
}
