package com.faforever.client.play;

import com.faforever.client.game.Faction;
import com.faforever.client.parsecom.CloudAccessor;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.games.Games;
import com.google.api.services.games.GamesScopes;
import com.google.api.services.games.model.AchievementDefinition;
import com.google.api.services.games.model.AchievementIncrementResponse;
import com.google.api.services.games.model.AchievementUnlockResponse;
import com.google.api.services.games.model.EventPeriodRange;
import com.google.api.services.games.model.EventPeriodUpdate;
import com.google.api.services.games.model.EventRecordRequest;
import com.google.api.services.games.model.EventUpdateRequest;
import com.google.api.services.games.model.EventUpdateResponse;
import com.google.api.services.games.model.Player;
import com.google.api.services.games.model.PlayerAchievement;
import com.google.common.io.Resources;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GooglePlayServices implements PlayServices {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Lock BATCH_REQUEST_LOCK = new ReentrantLock();

  private static final String ACH_WELCOME_COMMANDER = "CgkI9r3Q7egOEAIQFA";
  private static final String ACH_NOVICE = "CgkI9r3Q7egOEAIQAw";
  private static final String ACH_JUNIOR = "CgkI9r3Q7egOEAIQFQ";
  private static final String ACH_SENIOR = "CgkI9r3Q7egOEAIQBg";
  private static final String ACH_VETERAN = "CgkI9r3Q7egOEAIQFg";
  private static final String ACH_FIRST_SUCCESS = "CgkI9r3Q7egOEAIQFw";
  private static final String ACH_PRIVATE = "CgkI9r3Q7egOEAIQGA";
  private static final String ACH_CORPORAL = "CgkI9r3Q7egOEAIQGQ";
  private static final String ACH_SERGEANT_MAJOR = "CgkI9r3Q7egOEAIQGg";
  private static final String ACH_GETTING_STARTED = "CgkI9r3Q7egOEAIQGw";
  private static final String ACH_GETTING_BETTER = "CgkI9r3Q7egOEAIQHA";
  private static final String ACH_HATTRICK = "CgkI9r3Q7egOEAIQHQ";
  private static final String ACH_THAT_WAS_CLOSE = "CgkI9r3Q7egOEAIQHg";
  private static final String ACH_TOP_SCORE = "CgkI9r3Q7egOEAIQHw";
  private static final String ACH_UNBEATABLE = "CgkI9r3Q7egOEAIQIA";
  private static final String ACH_RUSHER = "CgkI9r3Q7egOEAIQIQ";
  private static final String ACH_MA12_STRIKER = "CgkI9r3Q7egOEAIQIg";
  private static final String ACH_RIPTIDE = "CgkI9r3Q7egOEAIQIw";
  private static final String ACH_DEMOLISHER = "CgkI9r3Q7egOEAIQJA";
  private static final String ACH_MANTIS = "CgkI9r3Q7egOEAIQJQ";
  private static final String ACH_WAGNER = "CgkI9r3Q7egOEAIQJg";
  private static final String ACH_TREBUCHET = "CgkI9r3Q7egOEAIQJw";
  private static final String ACH_AURORA = "CgkI9r3Q7egOEAIQKA";
  private static final String ACH_BLAZE = "CgkI9r3Q7egOEAIQKQ";
  private static final String ACH_SERENITY = "CgkI9r3Q7egOEAIQKg";
  private static final String ACH_THAAM = "CgkI9r3Q7egOEAIQKw";
  private static final String ACH_YENZYNE = "CgkI9r3Q7egOEAIQLA";
  private static final String ACH_SUTHANUS = "CgkI9r3Q7egOEAIQLQ";
  private static final String ACH_LANDLUBBER = "CgkI9r3Q7egOEAIQLg";
  private static final String ACH_SEAMAN = "CgkI9r3Q7egOEAIQLw";
  private static final String ACH_ADMIRAL_OF_THE_FLEET = "CgkI9r3Q7egOEAIQMA";
  private static final String ACH_WRIGHT_BROTHER = "CgkI9r3Q7egOEAIQMQ";
  private static final String ACH_WINGMAN = "CgkI9r3Q7egOEAIQMg";
  private static final String ACH_KING_OF_THE_SKIES = "CgkI9r3Q7egOEAIQMw";
  private static final String ACH_MILITIAMAN = "CgkI9r3Q7egOEAIQNA";
  private static final String ACH_GRENADIER = "CgkI9r3Q7egOEAIQNQ";
  private static final String ACH_FIELD_MARSHAL = "CgkI9r3Q7egOEAIQNg";
  private static final String ACH_TECHIE = "CgkI9r3Q7egOEAIQOg";
  private static final String ACH_I_LOVE_BIG_TOYS = "CgkI9r3Q7egOEAIQOw";
  private static final String ACH_EXPERIMENTALIST = "CgkI9r3Q7egOEAIQPA";
  private static final String ACH_WHAT_A_SWARM = "CgkI9r3Q7egOEAIQPQ";
  private static final String ACH_THE_TRANSPORTER = "CgkI9r3Q7egOEAIQOQ";
  private static final String ACH_WHO_NEEDS_SUPPORT = "CgkI9r3Q7egOEAIQPg";
  private static final String ACH_DEADLY_BUGS = "CgkI9r3Q7egOEAIQPw";
  private static final String ACH_NO_MERCY = "CgkI9r3Q7egOEAIQQA";
  private static final String ACH_FLYING_DEATH = "CgkI9r3Q7egOEAIQQQ";
  private static final String ACH_INCOMING_ROBOTS = "CgkI9r3Q7egOEAIQQg";
  private static final String ACH_ARACHNOLOGIST = "CgkI9r3Q7egOEAIQQw";
  private static final String ACH_FATTER_IS_BETTER = "CgkI9r3Q7egOEAIQRA";
  private static final String ACH_ALIEN_INVASION = "CgkI9r3Q7egOEAIQRQ";
  private static final String ACH_ASS_WASHER = "CgkI9r3Q7egOEAIQRg";
  private static final String ACH_DEATH_FROM_ABOVE = "CgkI9r3Q7egOEAIQRw";
  private static final String ACH_STORMY_SEA = "CgkI9r3Q7egOEAIQSA";
  private static final String ACH_IT_AINT_A_CITY = "CgkI9r3Q7egOEAIQSQ";
  private static final String ACH_RAINMAKER = "CgkI9r3Q7egOEAIQSg";
  private static final String ACH_I_HAVE_A_CANON = "CgkI9r3Q7egOEAIQSw";
  private static final String ACH_MAKE_IT_HAIL = "CgkI9r3Q7egOEAIQTA";
  private static final String ACH_SO_MUCH_RESOURCES = "CgkI9r3Q7egOEAIQTQ";
  private static final String ACH_NUCLEAR_WAR = "CgkI9r3Q7egOEAIQTg";
  private static final String ACH_DR_EVIL = "CgkI9r3Q7egOEAIQTw";
  private static final String ACH_DONT_MESS_WITH_ME = "CgkI9r3Q7egOEAIQZw";
  private static final String ACH_MMMH_COFFEE = "CgkI9r3Q7egOEAIQUA";
  private static final String EVENT_CUSTOM_GAMES_PLAYED = "CgkI9r3Q7egOEAIQAQ";
  private static final String EVENT_RANKED_1V1_GAMES_PLAYED = "CgkI9r3Q7egOEAIQAg";
  private static final String EVENT_FALLEN_ACUS = "CgkI9r3Q7egOEAIQCA";
  private static final String EVENT_BUILT_AIR_UNITS = "CgkI9r3Q7egOEAIQXQ";
  private static final String EVENT_FALLEN_AIR_UNITS = "CgkI9r3Q7egOEAIQCQ";
  private static final String EVENT_BUILT_LAND_UNITS = "CgkI9r3Q7egOEAIQXg";
  private static final String EVENT_FALLEN_LAND_UNITS = "CgkI9r3Q7egOEAIQCg";
  private static final String EVENT_BUILT_NAVAL_UNITS = "CgkI9r3Q7egOEAIQXw";
  private static final String EVENT_FALLEN_NAVAL_UNITS = "CgkI9r3Q7egOEAIQCw";
  private static final String EVENT_HOURS_PLAYED = "CgkI9r3Q7egOEAIQEg";
  private static final String EVENT_BUILT_TECH_1_UNITS = "CgkI9r3Q7egOEAIQUQ";
  private static final String EVENT_FALLEN_TECH_1_UNITS = "CgkI9r3Q7egOEAIQYA";
  private static final String EVENT_BUILT_TECH_2_UNITS = "CgkI9r3Q7egOEAIQUg";
  private static final String EVENT_FALLEN_TECH_2_UNITS = "CgkI9r3Q7egOEAIQYQ";
  private static final String EVENT_BUILT_TECH_3_UNITS = "CgkI9r3Q7egOEAIQUw";
  private static final String EVENT_FALLEN_TECH_3_UNITS = "CgkI9r3Q7egOEAIQYg";
  private static final String EVENT_BUILT_EXPERIMENTALS = "CgkI9r3Q7egOEAIQVA";
  private static final String EVENT_FALLEN_EXPERIMENTALS = "CgkI9r3Q7egOEAIQYw";
  private static final String EVENT_BUILT_ENGINEERS = "CgkI9r3Q7egOEAIQZA";
  private static final String EVENT_FALLEN_ENGINEERS = "CgkI9r3Q7egOEAIQZQ";
  private static final String EVENT_AEON_PLAYS = "CgkI9r3Q7egOEAIQVQ";
  private static final String EVENT_CYBRAN_PLAYS = "CgkI9r3Q7egOEAIQVg";
  private static final String EVENT_UEF_PLAYS = "CgkI9r3Q7egOEAIQVw";
  private static final String EVENT_SERAPHIM_PLAYS = "CgkI9r3Q7egOEAIQWA";
  private static final String EVENT_AEON_WINS = "CgkI9r3Q7egOEAIQWQ";
  private static final String EVENT_CYBRAN_WINS = "CgkI9r3Q7egOEAIQWg";
  private static final String EVENT_UEF_WINS = "CgkI9r3Q7egOEAIQWw";
  private static final String EVENT_SERAPHIM_WINS = "CgkI9r3Q7egOEAIQXA";
  private static final String EVENT_BUILT_PARAGONS = "CgkI9r3Q7egOEAIQaA";


  /**
   * Be sure to specify the name of your application. If the application name is {@code null} or blank, the application
   * will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
  private static final String APPLICATION_NAME = "downlords-faf-client";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final Pattern AUTHORIZATION_CODE_PATTERN = Pattern.compile("\\?code=(.*?)&");
  private static final JsonBatchCallback<AchievementUnlockResponse> UNLOCK_CALLBACK = batchCallback();
  private static final JsonBatchCallback<AchievementIncrementResponse> INCREMENT_CALLBACK = batchCallback();
  private static final JsonBatchCallback<EventUpdateResponse> RECORD_CALLBACK = batchCallback();
  private final ObservableList<PlayerAchievement> readOnlyPlayerAchievements;
  private final ObservableList<PlayerAchievement> playerAchievements;

  @Resource
  PreferencesService preferencesService;
  @Resource
  ExecutorService executorService;
  @Resource
  UserService userService;
  @Resource
  CloudAccessor cloudAccessor;

  private FileDataStoreFactory dataStoreFactory;
  private Games games;
  private NetHttpTransport httpTransport;
  private ServerSocket verificationCodeServerSocket;
  private BatchRequest batchRequest;

  public GooglePlayServices() {
    playerAchievements = FXCollections.observableArrayList();
    readOnlyPlayerAchievements = FXCollections.unmodifiableObservableList(playerAchievements);
  }

  @PostConstruct
  void postConstruct() throws GeneralSecurityException, IOException {
    Path playServicesDirectory = preferencesService.getPreferencesDirectory().resolve("play-services");

    httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    dataStoreFactory = new FileDataStoreFactory(playServicesDirectory.toFile());
  }

  @PreDestroy
  void shutDown() {
    IOUtils.closeQuietly(verificationCodeServerSocket);
  }

  @NotNull
  private static <T> JsonBatchCallback<T> batchCallback() {
    return new JsonBatchCallback<T>() {
      @Override
      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
        logger.warn("Event could not be updated: {}", e);
      }

      @Override
      public void onSuccess(T response, HttpHeaders responseHeaders) throws IOException {
      }
    };
  }

  @Override
  public CompletableFuture<Void> authorize() {
    Runnable runnable = () -> {
      GoogleClientSecrets clientSecrets = null;
      try {
        clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
            new InputStreamReader(getClass().getResourceAsStream("/client_secrets.json")));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, GamesScopes.all())
            .setDataStoreFactory(dataStoreFactory)
            .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, verificationCodeReceiver()).authorize(String.valueOf(userService.getUid()));

        games = new Games.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();

        // FIXME call things only once
        Player currentPlayer = games.players().get("me").execute();
        cloudAccessor.setPlayerId(currentPlayer.getPlayerId()).get();

        games.achievements().unlock(ACH_WELCOME_COMMANDER).execute();

        loadCurrentPlayerAchievements();
      } catch (IOException | InterruptedException | ExecutionException e) {
        throw new RuntimeException(e);
      }
    };

    return CompletableFuture.runAsync(runnable, executorService);
  }

  private void loadCurrentPlayerAchievements() {
    cloudAccessor.getPlayerIdForUsername(userService.getUsername()).thenAccept(playerId -> {
      if (StringUtils.isEmpty(playerId)) {
        playerAchievements.clear();
      }

      try {
        playerAchievements.setAll(games.achievements().list(playerId).execute().getItems());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public void startBatchUpdate() {
    BATCH_REQUEST_LOCK.lock();
    try {
      authorize().get();
      if (batchRequest != null) {
        throw new IllegalStateException("Batch update has already been started");
      }
      logger.debug("Starting batch update");
      batchRequest = games.batch();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    } finally {
      BATCH_REQUEST_LOCK.unlock();
    }
  }

  @Override
  public void executeBatchUpdate() throws IOException {
    BATCH_REQUEST_LOCK.lock();
    try {
      checkBatchRequest();
      logger.debug("Executing {} updates", batchRequest.size());
      batchRequest.execute();
      resetBatchUpdate();

      loadCurrentPlayerAchievements();
    } finally {
      BATCH_REQUEST_LOCK.unlock();
    }
  }

  @Override
  public void customGamePlayed() throws IOException {
    checkBatchRequest();
    incrementAchievement(ACH_NOVICE, 1);
    incrementAchievement(ACH_JUNIOR, 1);
    incrementAchievement(ACH_SENIOR, 1);
    incrementAchievement(ACH_VETERAN, 1);
    recordEvent(EVENT_CUSTOM_GAMES_PLAYED, 1L);
  }

  @Override
  public void ranked1v1GamePlayed() throws IOException {
    checkBatchRequest();
    incrementAchievement(ACH_NOVICE, 1);
    incrementAchievement(ACH_JUNIOR, 1);
    incrementAchievement(ACH_SENIOR, 1);
    incrementAchievement(ACH_VETERAN, 1);
    recordEvent(EVENT_RANKED_1V1_GAMES_PLAYED, 1L);
  }

  @Override
  public void ranked1v1GameWon() throws IOException {
    checkBatchRequest();
    unlockAchievement(ACH_FIRST_SUCCESS);
  }

  @Override
  public void killedCommanders(int count, boolean survived) throws IOException {
    checkBatchRequest();
    if (count >= 3 && survived) {
      unlockAchievement(ACH_HATTRICK);
    }
    incrementAchievement(ACH_DONT_MESS_WITH_ME, count);
    recordEvent(EVENT_FALLEN_ACUS, 1L);
  }

  @Override
  public void acuDamageReceived(double damage, boolean survived) throws IOException {
    if (damage < 12_000 || !survived) {
      return;
    }
    checkBatchRequest();
    unlockAchievement(ACH_THAT_WAS_CLOSE);
  }

  @Override
  public void resetBatchUpdate() {
    batchRequest = null;
  }

  @Override
  public void topScoringPlayer(int totalPlayers) throws IOException {
    if (totalPlayers < 8) {
      return;
    }
    checkBatchRequest();
    unlockAchievement(ACH_TOP_SCORE);
    incrementAchievement(ACH_UNBEATABLE, 1);
  }

  @Override
  public ObservableList<PlayerAchievement> getPlayerAchievements(String username) {
    if (userService.getUsername().equals(username)) {
      return readOnlyPlayerAchievements;
    }

    ObservableList<PlayerAchievement> playerAchievements = FXCollections.observableArrayList();

    cloudAccessor.getPlayerIdForUsername(username).thenAccept(playerId -> {
      try {
        if (StringUtils.isEmpty(playerId)) {
          return;
        }
        playerAchievements.setAll(games.achievements().list(playerId).execute().getItems());
      } catch (IOException e) {
        logger.warn("Could not load achievements", e);
      }
    });

    return playerAchievements;
  }

  @Override
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    Supplier<List<AchievementDefinition>> supplier = () -> {
      try {
        return games.achievementDefinitions().list().execute().getItems();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
    return CompletableFuture.supplyAsync(supplier, executorService);
  }

  @Override
  public void playerRating1v1(int rating) throws IOException {
    checkBatchRequest();
    if (rating > 500) {
      unlockAchievement(ACH_PRIVATE);
    }
    if (rating > 1000) {
      unlockAchievement(ACH_CORPORAL);
    }
    if (rating > 1500) {
      unlockAchievement(ACH_SERGEANT_MAJOR);
    }
  }

  @Override
  public void playerRatingGlobal(int rating) throws IOException {
    checkBatchRequest();
    if (rating > 500) {
      unlockAchievement(ACH_GETTING_STARTED);
    }
    if (rating > 1000) {
      unlockAchievement(ACH_GETTING_BETTER);
    }
  }

  @Override
  public void wonWithinDuration(Duration duration) throws IOException {
    if (duration.compareTo(Duration.ofMinutes(15)) > 0) {
      return;
    }
    checkBatchRequest();
    unlockAchievement(ACH_RUSHER);
  }

  @Override
  public void factionPlayed(Faction faction, boolean survived) throws IOException {
    checkBatchRequest();
    switch (faction) {
      case AEON:
        recordEvent(EVENT_AEON_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_AEON_WINS, 1);
        }
        incrementAchievement(ACH_AURORA, 1);
        incrementAchievement(ACH_BLAZE, 1);
        incrementAchievement(ACH_SERENITY, 1);
        break;
      case CYBRAN:
        recordEvent(EVENT_CYBRAN_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_CYBRAN_WINS, 1);
        }
        incrementAchievement(ACH_MANTIS, 1);
        incrementAchievement(ACH_WAGNER, 1);
        incrementAchievement(ACH_TREBUCHET, 1);
        break;
      case UEF:
        recordEvent(EVENT_UEF_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_UEF_WINS, 1);
        }
        incrementAchievement(ACH_MA12_STRIKER, 1);
        incrementAchievement(ACH_RIPTIDE, 1);
        incrementAchievement(ACH_DEMOLISHER, 1);
        break;
      case SERAPHIM:
        recordEvent(EVENT_SERAPHIM_PLAYS, 1);
        if (survived) {
          recordEvent(EVENT_SERAPHIM_WINS, 1);
        }
        incrementAchievement(ACH_THAAM, 1);
        incrementAchievement(ACH_YENZYNE, 1);
        incrementAchievement(ACH_SUTHANUS, 1);
        break;
    }
  }

  @Override
  public void unitStats(int airBuilt, int airKilled, int landBuilt, int landKilled, int navalBuilt, int navalKilled,
                        int tech1Built, int tech1Killed, int tech2Built, int tech2Killed, int tech3Built,
                        int tech3Killed, int experimentalsBuilt, int experimentalsKilled, int engineersBuilt,
                        int engineersKilled, boolean survived) throws IOException {
    checkBatchRequest();

    recordEvent(EVENT_BUILT_AIR_UNITS, airBuilt);
    recordEvent(EVENT_BUILT_LAND_UNITS, landBuilt);
    recordEvent(EVENT_BUILT_NAVAL_UNITS, navalBuilt);
    recordEvent(EVENT_BUILT_TECH_1_UNITS, tech1Built);
    recordEvent(EVENT_BUILT_TECH_2_UNITS, tech2Built);
    recordEvent(EVENT_BUILT_TECH_3_UNITS, tech3Built);
    recordEvent(EVENT_BUILT_EXPERIMENTALS, experimentalsBuilt);
    recordEvent(EVENT_BUILT_ENGINEERS, engineersBuilt);

    recordEvent(EVENT_FALLEN_AIR_UNITS, airKilled);
    recordEvent(EVENT_FALLEN_LAND_UNITS, landKilled);
    recordEvent(EVENT_FALLEN_NAVAL_UNITS, navalKilled);
    recordEvent(EVENT_FALLEN_TECH_1_UNITS, tech1Killed);
    recordEvent(EVENT_FALLEN_TECH_2_UNITS, tech2Killed);
    recordEvent(EVENT_FALLEN_TECH_3_UNITS, tech3Killed);
    recordEvent(EVENT_FALLEN_EXPERIMENTALS, experimentalsKilled);
    recordEvent(EVENT_FALLEN_ENGINEERS, engineersKilled);

    if (survived) {
      if (airBuilt > landBuilt && airBuilt > navalBuilt) {
        incrementAchievement(ACH_WRIGHT_BROTHER, 1);
        incrementAchievement(ACH_WINGMAN, 1);
        incrementAchievement(ACH_KING_OF_THE_SKIES, 1);
      } else if (landBuilt > airBuilt && landBuilt > navalBuilt) {
        incrementAchievement(ACH_MILITIAMAN, 1);
        incrementAchievement(ACH_GRENADIER, 1);
        incrementAchievement(ACH_FIELD_MARSHAL, 1);
      } else if (navalBuilt > airBuilt && navalBuilt > landBuilt) {
        incrementAchievement(ACH_LANDLUBBER, 1);
        incrementAchievement(ACH_SEAMAN, 1);
        incrementAchievement(ACH_ADMIRAL_OF_THE_FLEET, 1);
      }
      if (experimentalsBuilt >= 3) {
        incrementAchievement(ACH_TECHIE, 1);
        incrementAchievement(ACH_I_LOVE_BIG_TOYS, 1);
        incrementAchievement(ACH_EXPERIMENTALIST, 1);
      }
      incrementAchievement(ACH_DR_EVIL, experimentalsBuilt);
    }
  }

  @Override
  public void timePlayed(Duration duration, boolean survived) throws IOException {
    recordEvent(EVENT_HOURS_PLAYED, duration.getSeconds());
  }

  @Override
  public void asfBuilt(int count) throws IOException {
    if (count > 150) {
      unlockAchievement(ACH_WHAT_A_SWARM);
    }
  }

  @Override
  public void builtTransports(int count) throws IOException {
    incrementAchievement(ACH_THE_TRANSPORTER, count);
  }

  @Override
  public void builtParagons(int count, boolean survived) throws IOException {
    if (survived && count > 0) {
      unlockAchievement(ACH_SO_MUCH_RESOURCES);
    }
    recordEvent(EVENT_BUILT_PARAGONS, 1);
  }

  @Override
  public void builtYolonaOss(int count, boolean survived) throws IOException {
    if (survived && count > 0) {
      unlockAchievement(ACH_NUCLEAR_WAR);
    }
  }

  @Override
  public void builtScathis(int count, boolean survived) throws IOException {
    if (survived && count > 0) {
      unlockAchievement(ACH_MAKE_IT_HAIL);
    }
  }

  @Override
  public void builtSalvations(int count, boolean survived) throws IOException {
    if (survived && count > 0) {
      unlockAchievement(ACH_RAINMAKER);
    }
  }

  @Override
  public void builtMavors(int count, boolean survived) throws IOException {
    if (survived && count > 0) {
      unlockAchievement(ACH_I_HAVE_A_CANON);
    }
  }

  @Override
  public void builtAtlantis(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_IT_AINT_A_CITY, count);
  }

  @Override
  public void builtTempests(int count, boolean survived) throws IOException {
    if (survived && count > 0) {
      incrementAchievement(ACH_STORMY_SEA, count);
    }
  }

  @Override
  public void builtCzars(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_DEATH_FROM_ABOVE, count);
  }

  @Override
  public void builtAhwasshas(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_ASS_WASHER, count);
  }

  @Override
  public void builtYthothas(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_ALIEN_INVASION, count);
  }

  @Override
  public void builtFatboys(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_FATTER_IS_BETTER, count);
  }

  @Override
  public void builtMonkeylords(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_ARACHNOLOGIST, count);
  }

  @Override
  public void builtGalacticColossus(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_INCOMING_ROBOTS, count);
  }

  @Override
  public void builtSoulRippers(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_FLYING_DEATH, count);
  }

  @Override
  public void builtMercies(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_NO_MERCY, count);
  }

  @Override
  public void builtFireBeetles(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_DEADLY_BUGS, count);
  }

  @Override
  public void builtSupportCommanders(int count, boolean survived) throws IOException {
    if (count >= 10) {
      unlockAchievement(ACH_WHO_NEEDS_SUPPORT);
    }
  }

  @Override
  public void builtMegaliths(int count, boolean survived) throws IOException {
    incrementAchievement(ACH_DEADLY_BUGS, count);
  }

  @NotNull
  private EventRecordRequest createEventRequest(String eventId, long updateCount) {
    // The event that occurred, and how many times
    EventUpdateRequest updateRequest = new EventUpdateRequest();
    updateRequest.setDefinitionId(eventId);
    updateRequest.setUpdateCount(updateCount);
    List<EventUpdateRequest> updates = Collections.singletonList(updateRequest);

    // The period in which these events occurred
    EventPeriodRange timePeriod = new EventPeriodRange();
    timePeriod.setPeriodStartMillis(System.currentTimeMillis());
    timePeriod.setPeriodEndMillis(System.currentTimeMillis());

    // Putting event and periods together
    EventPeriodUpdate periodUpdate = new EventPeriodUpdate();
    periodUpdate.setTimePeriod(timePeriod);
    periodUpdate.setUpdates(updates);
    List<EventPeriodUpdate> timePeriods = Collections.singletonList(periodUpdate);

    // The final request
    EventRecordRequest eventRecordRequest = new EventRecordRequest();
    eventRecordRequest.setTimePeriods(timePeriods);
    return eventRecordRequest;
  }

  private void checkBatchRequest() {
    if (batchRequest == null) {
      throw new IllegalStateException("Batch update has not been started");
    }
  }

  private void unlockAchievement(String achievement) throws IOException {
    games.achievements().unlock(achievement).queue(batchRequest, UNLOCK_CALLBACK);
  }

  private void incrementAchievement(String achievement, int steps) throws IOException {
    games.achievements().increment(achievement, steps).queue(batchRequest, INCREMENT_CALLBACK);
  }

  private void recordEvent(String event, long updateCount) throws IOException {
    games.events().record(createEventRequest(EVENT_CUSTOM_GAMES_PLAYED, updateCount)).queue(batchRequest, RECORD_CALLBACK);
  }

  /**
   * Starts a local server that listens for the verification code. <p> After the user authorized the application, google
   * redirects to a URL specified by the application (http://localhost:####) to send the verification code there.</p>
   */
  private VerificationCodeReceiver verificationCodeReceiver() {

    return new VerificationCodeReceiver() {
      public Future<String> codeFuture;

      @Override
      public String getRedirectUri() throws IOException {
        CompletableFuture<Integer> portFuture = startReceiver();

        try {
          return "http://localhost:" + portFuture.get();
        } catch (InterruptedException | ExecutionException e) {
          throw new IOException("Receiver could not be started", e);
        }
      }

      @Override
      public String waitForCode() throws IOException {
        try {
          return codeFuture.get();
        } catch (InterruptedException | ExecutionException e) {
          throw new IOException("Code could not be received", e);
        }
      }

      @Override
      public void stop() throws IOException {
      }

      private CompletableFuture<Integer> startReceiver() throws IOException {
        CompletableFuture<Integer> portFuture = new CompletableFuture<>();
        Callable<String> callable = () -> {
          try (ServerSocket serverSocket = new ServerSocket(0)) {
            GooglePlayServices.this.verificationCodeServerSocket = serverSocket;
            logger.debug("Started verification code listener at port {}", serverSocket.getLocalPort());
            portFuture.complete(serverSocket.getLocalPort());

            try (Socket socket = serverSocket.accept()) {
              logger.debug("Accepted connection from browser");
              BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
              String line = reader.readLine();

              Matcher matcher = AUTHORIZATION_CODE_PATTERN.matcher(line);
              if (!matcher.find()) {
                throw new IOException("Could not extract code from: " + line);
              }

              String code = matcher.group(1);
              logger.debug("Received code: {}", code);

              socket.getOutputStream().write(Resources.toByteArray(getClass().getResource("/google_auth_answer.txt")));

              return code;
            }
          }
        };

        codeFuture = executorService.submit(callable);
        return portFuture;
      }
    };
  }
}

