package com.faforever.client.api;

import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.HostService;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.JsonToken;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.util.ReflectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FafApiAccessorImpl implements FafApiAccessor {

  private static final String HTTP_LOCALHOST = "http://localhost:";
  private static final String ENCODED_HTTP_LOCALHOST = HTTP_LOCALHOST.replace(":", "%3A").replace("/", "%2F");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String SCOPE_READ_ACHIEVEMENTS = "read_achievements";
  private static final String SCOPE_READ_EVENTS = "read_events";

  @Resource
  JsonFactory jsonFactory;
  @Resource
  PreferencesService preferencesService;
  @Resource
  ExecutorService executorService;
  @Resource
  HostService hostServices;
  @Resource
  HttpTransport httpTransport;
  @Resource
  VerificationCodeReceiver verificationCodeReceiver;
  @Resource
  UserService userService;

  @Value("${api.baseUrl}")
  String baseUrl;
  @Value("${oauth.authUri}")
  String oAuthUrl;
  @Value("${oauth.tokenUri}")
  String oAuthTokenServerUrl;
  @Value("${oauth.clientId}")
  String oAuthClientId;
  @Value("${oauth.clientSecret}")
  String oAuthClientSecret;
  @Value("${oauth.loginUri}")
  String oAuthLoginUrl;
  @VisibleForTesting
  Credential credential;
  @VisibleForTesting
  HttpRequestFactory requestFactory;
  private FileDataStoreFactory dataStoreFactory;


  @PostConstruct
  void postConstruct() throws IOException {
    Path playServicesDirectory = preferencesService.getPreferencesDirectory().resolve("oauth");
    dataStoreFactory = new FileDataStoreFactory(playServicesDirectory.toFile());
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    logger.debug("Loading achievements for player: {}", playerId);
    return getMany("/players/" + playerId + "/achievements", PlayerAchievement.class, 1);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    logger.debug("Loading events for player: {}", playerId);
    return getMany("/players/" + playerId + "/events", PlayerEvent.class, 1);
  }

  @Override
  @SuppressWarnings("unchecked")
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public List<AchievementDefinition> getAchievementDefinitions() {
    logger.debug("Loading achievement definitions");
    return getMany("/achievements?sort=order", AchievementDefinition.class, 1);
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    logger.debug("Getting definition for achievement {}", achievementId);
    return getSingle("/achievements/" + achievementId, AchievementDefinition.class);
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
          oAuthUrl)
          .setDataStoreFactory(dataStoreFactory)
          .setScopes(Arrays.asList(SCOPE_READ_ACHIEVEMENTS, SCOPE_READ_EVENTS))
          .build();

      credential = authorize(flow, String.valueOf(playerId));
      requestFactory = httpTransport.createRequestFactory(credential);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Cacheable(CacheNames.MODS)
  public List<ModInfoBean> getMods() {
    logger.debug("Loading available mods");
    return getMany("/mods", Mod.class).stream()
        .map(ModInfoBean::fromModInfo)
        .collect(Collectors.toList());
  }

  @Override
  public List<Ranked1v1EntryBean> getRanked1v1Entries() {
    return getMany("/ranked1v1", LeaderboardEntry.class).stream()
        .map(Ranked1v1EntryBean::fromLeaderboardEntry)
        .collect(Collectors.toList());
  }

  @Override
  public Ranked1v1Stats getRanked1v1Stats() {
    return getSingle("/ranked1v1/stats", Ranked1v1Stats.class);
  }

  @Override
  public Ranked1v1EntryBean getRanked1v1EntryForPlayer(int playerId) {
    return Ranked1v1EntryBean.fromLeaderboardEntry(getSingle("/ranked1v1/" + playerId, LeaderboardEntry.class));
  }

  private <T> List<T> getMany(String endpointPath, Class<T> type) {
    List<T> result = new LinkedList<>();
    List<T> current = null;
    int page = 1;
    while (current == null || !current.isEmpty()) {
      current = getMany(endpointPath, type, page++);
      result.addAll(current);
    }
    return result;
  }

  private Credential authorize(AuthorizationCodeFlow flow, String userId) throws IOException {
    try {
      Credential credential = flow.loadCredential(userId);
      if (credential != null
          && (credential.getRefreshToken() != null || credential.getExpiresInSeconds() > 60)) {
        return credential;
      }

      String redirectUri = verificationCodeReceiver.getRedirectUri();
      if (redirectUri == null) {
        return null;
      }
      AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);

      // Google's GenericUrl does not escape ":" and "/", but Flask (FAF's OAuth) requires it.
      String fixedAuthorizationUrl = authorizationUrl.build()
          .replaceFirst("uri=" + Pattern.quote(HTTP_LOCALHOST), "uri=" + ENCODED_HTTP_LOCALHOST);

      Map<String, Object> data = new HashMap<>();
      data.put("username", userService.getUsername());
      data.put("password", userService.getPassword());
      data.put("next", fixedAuthorizationUrl);

      HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
      HttpRequest loginRequest = requestFactory.buildPostRequest(new GenericUrl(oAuthLoginUrl), new UrlEncodedContent(data));
      HttpResponse loginResponse = loginRequest.execute();

      if (!HttpStatusCodes.isRedirect(loginResponse.getStatusCode())) {
        throw new RuntimeException("Login failed: (" + loginResponse.getStatusCode() + ") " + loginResponse.getStatusMessage());
      }

      String cookie = loginResponse.getHeaders().getCookie();

      data.clear();
      data.put("allow", "");

      HttpRequest httpRequest = requestFactory.buildPostRequest(new GenericUrl(fixedAuthorizationUrl), new UrlEncodedContent(data));
      HttpHeaders headers = new HttpHeaders();
      headers.setCookie(cookie);
      httpRequest.setHeaders(headers);
      HttpResponse response = httpRequest.execute();

      if (!response.isSuccessStatusCode()) {
        throw new RuntimeException("Could not authorize: " + response.getStatusMessage() + " (" + response.getStatusCode() + ")");
      }

      String code = verificationCodeReceiver.waitForCode();
      TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

      return flow.createAndStoreCredential(tokenResponse, userId);
    } finally {
      verificationCodeReceiver.stop();
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T getSingle(String endpointPath, Class<T> type) {
    try (InputStream inputStream = executeGet(endpointPath)) {
      JsonParser jsonParser = jsonFactory.createJsonParser(inputStream, StandardCharsets.UTF_8);
      jsonParser.nextToken();
      jsonParser.skipToKey("data");

      return extractObject(type, jsonParser);
    } catch (IOException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> getMany(String endpointPath, Class<T> type, int page) {
    ArrayList<T> result = new ArrayList<>();
    try (InputStream inputStream = executeGet(endpointPath)) {
      JsonParser jsonParser = jsonFactory.createJsonParser(inputStream, StandardCharsets.UTF_8);
      jsonParser.nextToken();
      jsonParser.skipToKey("data");

      while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
        T object = extractObject(type, jsonParser);
        result.add(object);
      }
      return result;
    } catch (IOException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private InputStream executeGet(String endpointPath) throws IOException {
    if (requestFactory == null) {
      throw new IllegalStateException("authorize() must be called first");
    }
    String url = baseUrl + endpointPath;
    logger.trace("Calling: {}", url);
    HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(url));
    credential.initialize(request);
    return request.execute().getContent();
  }

  @Nullable
  private <T> T extractObject(Class<T> type, JsonParser jsonParser) throws IOException, IllegalAccessException {
    T object = null;
    String id = null;
    JsonToken currentToken = jsonParser.nextToken();
    while (currentToken != null && currentToken != JsonToken.END_OBJECT) {
      switch (jsonParser.getCurrentToken()) {
        case START_OBJECT:
          break;
        case FIELD_NAME:
          if ("attributes".equals(jsonParser.getCurrentName())) {
            jsonParser.nextToken();
            object = jsonParser.parse(type);
          } else if ("id".equals(jsonParser.getCurrentName())) {
            jsonParser.nextToken();
            id = jsonParser.getText();
          }
          break;
      }
      currentToken = jsonParser.nextToken();
    }
    Field idField = ReflectionUtils.findField(type, "id");
    ReflectionUtils.makeAccessible(idField);
    idField.set(object, id);
    return object;
  }
}
