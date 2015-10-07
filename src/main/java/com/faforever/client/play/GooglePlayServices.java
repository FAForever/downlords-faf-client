package com.faforever.client.play;

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
import com.google.api.services.games.model.EventPeriodRange;
import com.google.api.services.games.model.EventPeriodUpdate;
import com.google.api.services.games.model.EventRecordRequest;
import com.google.api.services.games.model.EventUpdateRequest;
import org.apache.commons.compress.utils.IOUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GooglePlayServices implements PlayServices {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Lock BATCH_REQUEST_LOCK = new ReentrantLock();
  private static final String ACH_GETTING_STARTED = "CgkI9r3Q7egOEAIQAw";
  private static final String ACH_GLOBAL_500 = "CgkI9r3Q7egOEAIQBA";
  private static final String ACH_RANKED_1V1_500 = "CgkI9r3Q7egOEAIQBQ";
  private static final String ACH_PLAY_100_GAMES = "CgkI9r3Q7egOEAIQBg";
  private static final String ACH_PLAY_500_GAMES = "CgkI9r3Q7egOEAIQBw";
  private static final String ACH_KILL_3_ENEMIES_IN_ONE_GAME = "CgkI9r3Q7egOEAIQDQ";
  private static final String ACH_SURVIVE_12000_DAMAGE = "CgkI9r3Q7egOEAIQDg";
  private static final String ACH_TOP_SCORING_PLAYER = "CgkI9r3Q7egOEAIQDw";
  private static final String EVENT_NUMBER_OF_CUSTOM_GAMES = "CgkI9r3Q7egOEAIQAQ";
  private static final String EVENT_NUMBER_OF_RANKED_1V1_GAMES = "CgkI9r3Q7egOEAIQAg";
  private static final String EVENT_FALLEN_ACUS = "CgkI9r3Q7egOEAIQCA";
  private static final String EVENT_FALLEN_AIR_UNITS = "CgkI9r3Q7egOEAIQCQ";
  private static final String EVENT_FALLEN_LAND_UNITS = "CgkI9r3Q7egOEAIQCg";
  private static final String EVENT_FALLEN_NAVAL_UNITS = "CgkI9r3Q7egOEAIQCw";
  private static final String EVENT_FALLEN_ENGINEERS = "CgkI9r3Q7egOEAIQDA";

  /**
   * Be sure to specify the name of your application. If the application name is {@code null} or blank, the application
   * will log a warning. Suggested format is "MyCompany-ProductName/1.0".
   */
  private static final String APPLICATION_NAME = "downlords-faf-client";
  private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
  private static final Pattern AUTHORIZATION_CODE_PATTERN = Pattern.compile("\\?code=(.*?)&");

  @Resource
  PreferencesService preferencesService;
  @Resource
  ExecutorService executorService;
  @Resource
  UserService userService;

  private FileDataStoreFactory dataStoreFactory;
  private Games games;
  private NetHttpTransport httpTransport;
  private ServerSocket verificationCodeServerSocket;
  private BatchRequest batchRequest;

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
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };

    return CompletableFuture.runAsync(runnable, executorService);
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
    } finally {
      BATCH_REQUEST_LOCK.unlock();
    }
  }

  @Override
  public void customGamePlayed() throws IOException {
    checkBatchRequest();
    games.achievements().increment(ACH_GETTING_STARTED, 1).queue(batchRequest, batchCallback());
    games.achievements().increment(ACH_PLAY_100_GAMES, 1).queue(batchRequest, batchCallback());
    games.achievements().increment(ACH_PLAY_500_GAMES, 1).queue(batchRequest, batchCallback());
    games.events().record(createEventRequest(EVENT_NUMBER_OF_CUSTOM_GAMES, 1L)).queue(batchRequest, batchCallback());
  }

  @Override
  public void ranked1v1GamePlayed() throws IOException {
    checkBatchRequest();
    games.achievements().increment(ACH_GETTING_STARTED, 1).queue(batchRequest, batchCallback());
    games.achievements().increment(ACH_PLAY_100_GAMES, 1).queue(batchRequest, batchCallback());
    games.achievements().increment(ACH_PLAY_500_GAMES, 1).queue(batchRequest, batchCallback());
    games.events().record(createEventRequest(EVENT_NUMBER_OF_RANKED_1V1_GAMES, 1L)).queue(batchRequest, batchCallback());
  }

  @Override
  public void killedCommanders(int number, boolean survived) throws IOException {
    checkBatchRequest();
    if (number >= 3 && survived) {
      games.achievements().unlock(ACH_KILL_3_ENEMIES_IN_ONE_GAME).queue(batchRequest, batchCallback());
    }
    games.events().record(createEventRequest(EVENT_FALLEN_ACUS, 1L)).queue(batchRequest, batchCallback());
  }

  @Override
  public void acuDamageReceived(double damage, boolean survived) throws IOException {
    if (damage < 12_000 || !survived) {
      return;
    }
    checkBatchRequest();
    games.achievements().unlock(ACH_SURVIVE_12000_DAMAGE).queue(batchRequest, batchCallback());
  }

  @Override
  public void airUnitStats(long built, long killed) throws IOException {
    checkBatchRequest();
    games.events().record(createEventRequest(EVENT_FALLEN_AIR_UNITS, killed)).queue(batchRequest, batchCallback());
  }

  @Override
  public void landUnitStats(long built, long killed) throws IOException {
    checkBatchRequest();
    games.events().record(createEventRequest(EVENT_FALLEN_LAND_UNITS, killed)).queue(batchRequest, batchCallback());
  }

  @Override
  public void navalUnitStats(long built, long killed) throws IOException {
    checkBatchRequest();
    games.events().record(createEventRequest(EVENT_FALLEN_NAVAL_UNITS, killed)).queue(batchRequest, batchCallback());
  }

  @Override
  public void engineerStats(long built, long killed) throws IOException {
    checkBatchRequest();
    games.events().record(createEventRequest(EVENT_FALLEN_ENGINEERS, killed)).queue(batchRequest, batchCallback());
  }

  @Override
  public void techUnitsBuilt(int builtTech1Units, int builtTech2Units, int builtTech3Units) throws IOException {
    checkBatchRequest();
    games.achievements().unlock(ACH_KILL_3_ENEMIES_IN_ONE_GAME).queue(batchRequest, batchCallback());
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
    games.achievements().unlock(ACH_TOP_SCORING_PLAYER).queue(batchRequest, batchCallback());
  }

  @NotNull
  private <T> JsonBatchCallback<T> batchCallback() {
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

