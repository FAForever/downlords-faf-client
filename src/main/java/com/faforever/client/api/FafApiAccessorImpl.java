package com.faforever.client.api;

import com.faforever.client.events.AchievementDefinition;
import com.faforever.client.events.ListResult;
import com.faforever.client.events.PlayerAchievement;
import com.faforever.client.fx.HostService;
import com.faforever.client.preferences.PreferencesService;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Arrays;
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
  private static final Pattern AUTHORIZATION_CODE_PATTERN = Pattern.compile("[?&]code=(.*?)[& ]");
  private static final String HTTP_LOCALHOST = "http://localhost:";
  private static final String ENCODED_HTTP_LOCALHOST = HTTP_LOCALHOST.replace(":", "%3A").replace("/", "%2F");
  private static final String SCOPE_READ_ACHIEVEMENTS = "read_achievements";
  private static final String SCOPE_WRITE_ACHIEVEMENTS = "write_achievements";
  private static final String SCOPE_WRITE_EVENTS = "write_events";

  @Resource
  JsonFactory jsonFactory;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ExecutorService executorService;
  @Resource
  HostService hostServices;

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
  private Credential credential;
  private HttpRequestFactory requestFactory;

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
  @SuppressWarnings("unchecked")
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    logger.debug("Loading achievements for player: {}", playerId);
    Type returnType = new TypeToken<ListResult<PlayerAchievement>>() {
    }.getType();
    return ((ListResult<PlayerAchievement>) sendGetRequest("/players/" + playerId + "/achievements", returnType))
        .getItems();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<AchievementDefinition> getAchievementDefinitions() {
    logger.debug("Loading achievement definitions");
    Type returnType = new TypeToken<ListResult<AchievementDefinition>>() {
    }.getType();
    return ((ListResult<AchievementDefinition>) sendGetRequest("/achievements", returnType)).getItems();
  }

  @Override
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    logger.debug("Getting definition for achievement {}", achievementId);
    return sendGetRequest("/achievements/" + achievementId, AchievementDefinition.class);
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
          .setScopes(Arrays.asList(SCOPE_READ_ACHIEVEMENTS, SCOPE_WRITE_ACHIEVEMENTS, SCOPE_WRITE_EVENTS))
          .build();

      credential = authorize(flow, String.valueOf(playerId));
      requestFactory = httpTransport.createRequestFactory(credential);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Credential authorize(AuthorizationCodeFlow flow, String userId) throws IOException {
    VerificationCodeReceiver verificationCodeReceiver = verificationCodeReceiver();
    try {
      Credential credential = flow.loadCredential(userId);
      if (credential != null
          && (credential.getRefreshToken() != null || credential.getExpiresInSeconds() > 60)) {
        return credential;
      }

      String redirectUri = verificationCodeReceiver.getRedirectUri();
      AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);

      // Google's GenericUrl does not escape ":" and "/", but Flask (FAF's OAuth) requires it.
      String fixedAuthorizationUrl = authorizationUrl.build()
          .replaceFirst("uri=" + Pattern.quote(HTTP_LOCALHOST), "uri=" + ENCODED_HTTP_LOCALHOST);

      hostServices.showDocument(fixedAuthorizationUrl);

      String code = verificationCodeReceiver.waitForCode();
      TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

      return flow.createAndStoreCredential(response, userId);
    } finally {
      verificationCodeReceiver.stop();
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
          return HTTP_LOCALHOST + portFuture.get();
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

  @SuppressWarnings("unchecked")
  private <T> T sendGetRequest(String endpointPath, Type type) {
    try {
      HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(baseUrl + endpointPath));
      request.setParser(new JsonObjectParser(jsonFactory));
      credential.initialize(request);
      return (T) request.execute().parseAs(type);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T sendPostRequest(String endpointPath, Object content, Class<T> type) {
    try {
      HttpRequest request = requestFactory.buildPostRequest(
          new GenericUrl(baseUrl + endpointPath),
          new JsonHttpContent(jsonFactory, content));
      request.setParser(new JsonObjectParser(jsonFactory));
      credential.initialize(request);
      return request.execute().parseAs(type);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
