package com.faforever.client.play;

import com.faforever.client.preferences.PreferencesService;
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
import com.google.api.services.games.model.AchievementIncrementResponse;
import com.google.api.services.games.model.EventPeriodRange;
import com.google.api.services.games.model.EventPeriodUpdate;
import com.google.api.services.games.model.EventRecordRequest;
import com.google.api.services.games.model.EventUpdateRequest;
import com.google.api.services.games.model.EventUpdateResponse;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GooglePlayServices implements PlayServices {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String ACH_GETTING_STARTED = "CgkI9r3Q7egOEAIQAw";
  private static final String ACH_GLOBAL_500 = "CgkI9r3Q7egOEAIQBA";
  private static final String ACH_RANKED_1V1_500 = "CgkI9r3Q7egOEAIQBQ";
  private static final String ACH_PLAY_100_GAMES = "CgkI9r3Q7egOEAIQBg";
  private static final String ACH_PLAY_500_GAMES = "CgkI9r3Q7egOEAIQBw";
  private static final String EVENT_NUMBER_OF_CUSTOM_GAMES = "CgkI9r3Q7egOEAIQAQ";
  private static final String EVENT_NUMBER_OF_RANKED_1V1_GAMES = "CgkI9r3Q7egOEAIQAg";

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
  ThreadPoolExecutor threadPoolExecutor;
  private FileDataStoreFactory dataStoreFactory;
  private Games games;
  private NetHttpTransport httpTransport;
  private ServerSocket verificationCodeServerSocket;
  private BooleanProperty authorized;

  public GooglePlayServices() {
    authorized = new SimpleBooleanProperty();
  }

  @PostConstruct
  void postConstruct() throws GeneralSecurityException, IOException {
    Path playServicesDirectory = preferencesService.getPreferencesDirectory().resolve("play-services");

    httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    dataStoreFactory = new FileDataStoreFactory(playServicesDirectory.toFile());
  }

  @Override
  public void authorize(String uid) {
    Callable<Void> callable = new Callable<Void>() {
      public Void call() throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
            new InputStreamReader(getClass().getResourceAsStream("/client_secrets.json")));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport, JSON_FACTORY, clientSecrets, GamesScopes.all())
            .setDataStoreFactory(dataStoreFactory)
            .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, verificationCodeReceiver()).authorize(uid);

        games = new Games.Builder(httpTransport, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();

        authorized.set(true);

        return null;
      }
    };
    threadPoolExecutor.submit(callable);
  }

  @Override
  public void incrementPlayedCustomGames() throws IOException {
    BatchRequest batchRequest = games.batch();
    games.achievements().increment(ACH_PLAY_100_GAMES, 1).queue(batchRequest, achievementCallback());
    games.achievements().increment(ACH_PLAY_500_GAMES, 1).queue(batchRequest, achievementCallback());
    games.events().record(createEventRequest(EVENT_NUMBER_OF_CUSTOM_GAMES, 1L)).queue(batchRequest, eventCallback());

    batchRequest.execute();
  }

  @Override
  public void incrementPlayedRanked1v1Games() throws IOException {
    BatchRequest batchRequest = games.batch();
    games.achievements().increment(ACH_PLAY_100_GAMES, 1).queue(batchRequest, achievementCallback());
    games.achievements().increment(ACH_PLAY_500_GAMES, 1).queue(batchRequest, achievementCallback());
    games.events().record(createEventRequest(EVENT_NUMBER_OF_RANKED_1V1_GAMES, 1L)).queue(batchRequest, eventCallback());

    batchRequest.execute();
  }

  @Override
  public BooleanProperty authorizedProperty() {
    return authorized;
  }

  @NotNull
  private JsonBatchCallback<AchievementIncrementResponse> achievementCallback() {
    return new JsonBatchCallback<AchievementIncrementResponse>() {
      @Override
      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
        logger.warn("Event could not be updated: {}", e);
      }

      @Override
      public void onSuccess(AchievementIncrementResponse achievementIncrementResponse, HttpHeaders responseHeaders) throws IOException {

      }
    };
  }

  @NotNull
  private EventRecordRequest createEventRequest(String eventId, Long updateCount) {
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

  private JsonBatchCallback<EventUpdateResponse> eventCallback() {
    return new JsonBatchCallback<EventUpdateResponse>() {
      @Override
      public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
        logger.warn("Event could not be updated: {}", e);
      }

      @Override
      public void onSuccess(EventUpdateResponse eventUpdateResponse, HttpHeaders responseHeaders) throws IOException {

      }
    };
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

        codeFuture = threadPoolExecutor.submit(callable);
        return portFuture;
      }
    };
  }

  @PreDestroy
  void shutDown() {
    IOUtils.closeQuietly(verificationCodeServerSocket);
  }
}

