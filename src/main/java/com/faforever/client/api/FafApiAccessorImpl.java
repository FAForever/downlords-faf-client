package com.faforever.client.api;

import com.faforever.client.play.AchievementDefinition;
import com.faforever.client.play.AchievementUpdate;
import com.faforever.client.play.AchievementUpdatesRequest;
import com.faforever.client.play.AchievementUpdatesResponse;
import com.faforever.client.play.EventUpdatesRequest;
import com.faforever.client.play.EventUpdatesResponse;
import com.faforever.client.play.ListResult;
import com.faforever.client.play.PlayerAchievement;
import com.faforever.client.play.UpdatedAchievement;
import com.faforever.client.play.UpdatedEvent;
import com.faforever.client.preferences.PreferencesService;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.io.Resources;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FafApiAccessorImpl implements FafApiAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Pattern AUTHORIZATION_CODE_PATTERN = Pattern.compile("\\?code=(.*?)&");

  @Resource
  RestTemplate restTemplate;
  @Resource
  JsonFactory jsonFactory;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ExecutorService executorService;

  @Value("${api.baseUrl}")
  String baseUrl;
  @Value("${oauth.authUri}")
  String oAuthAuthorizationServerUrl;
  @Value("${oauth.tokenUri}")
  String oAuthTokenServerUrl;
  @Value("${oauth.clientId}")
  String oAuthClientId;
  @Value("${oauth.clientSecret}")
  String oAuthClientSecret;

  private FileDataStoreFactory dataStoreFactory;
  private NetHttpTransport httpTransport;
  private ServerSocket verificationCodeServerSocket;

  @PostConstruct
  void postConstruct() throws IOException {
    Path playServicesDirectory = preferencesService.getPreferencesDirectory().resolve("play-services");

    httpTransport = new NetHttpTransport.Builder().build();
    dataStoreFactory = new FileDataStoreFactory(playServicesDirectory.toFile());
  }

  @PreDestroy
  void shutDown() {
    IOUtils.closeQuietly(verificationCodeServerSocket);
  }

  @Override
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    return restTemplate.exchange(
        baseUrl + "player/{playerId}/achievements", HttpMethod.GET, null, new ParameterizedTypeReference<ListResult<PlayerAchievement>>() {
        }, playerId).getBody().getItems();
  }

  @Override
  public List<AchievementDefinition> getAchievementDefinitions() {
    return restTemplate.exchange(
        baseUrl + "achievements", HttpMethod.GET, null, new ParameterizedTypeReference<ListResult<AchievementDefinition>>() {
        }).getBody().getItems();
  }

  @Override
  public List<UpdatedAchievement> executeAchievementUpdates(AchievementUpdatesRequest achievementUpdatesRequest, int playerId) {
    Collection<AchievementUpdate> updates = achievementUpdatesRequest.getUpdates();
    logger.debug("Updating {} achievements", updates.size());


    return restTemplate.postForObject(baseUrl + "achievements/updateMultiple?player_id={playerId}",
        achievementUpdatesRequest, AchievementUpdatesResponse.class, playerId).getUpdatedAchievements();
  }

  @Override
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    return restTemplate.getForObject(baseUrl + "achievements/{achievementId}", AchievementDefinition.class, achievementId);
  }

  @Override
  public List<UpdatedEvent> recordEvents(EventUpdatesRequest eventUpdatesRequest, int playerId) {
    logger.debug("Updating {} events", eventUpdatesRequest.getUpdates().size());

    return restTemplate.postForObject(baseUrl + "events/recordMultiple?player_id={playerId}", eventUpdatesRequest,
        EventUpdatesResponse.class, playerId).getUpdatedEvents();
  }

  @Override
  public void authorize(int playerId) {
    try {
      AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(
          BearerToken.authorizationHeaderAccessMethod(),
          httpTransport,
          jsonFactory,
          new GenericUrl(oAuthTokenServerUrl),
          new ClientParametersAuthentication(oAuthClientId, oAuthClientSecret),
          oAuthClientId,
          oAuthAuthorizationServerUrl)
          .setDataStoreFactory(dataStoreFactory)
          .build();

      new AuthorizationCodeInstalledApp(flow, verificationCodeReceiver()).authorize(String.valueOf(playerId));
    } catch (IOException e) {
      throw new RuntimeException(e);
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
            FafApiAccessorImpl.this.verificationCodeServerSocket = serverSocket;
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
