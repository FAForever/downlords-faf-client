package com.faforever.client.api;

import com.faforever.client.achievements.AchievementDefinition;
import com.faforever.client.achievements.PlayerAchievement;
import com.faforever.client.config.CacheNames;
import com.faforever.client.fx.HostService;
import com.faforever.client.mod.ModInfoBean;
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
import com.google.api.client.http.HttpTransport;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FafApiAccessorImpl implements FafApiAccessor {

  private static final String HTTP_LOCALHOST = "http://localhost:";
  private static final String ENCODED_HTTP_LOCALHOST = HTTP_LOCALHOST.replace(":", "%3A").replace("/", "%2F");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
  @Resource
  HttpTransport httpTransport;
  @Resource
  VerificationCodeReceiver verificationCodeReceiver;

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
    return getMany("/players/" + playerId + "/achievements", PlayerAchievement.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public List<AchievementDefinition> getAchievementDefinitions() {
    logger.debug("Loading achievement definitions");
    return getMany("/achievements", AchievementDefinition.class);
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

  @Override
  @Cacheable(CacheNames.MODS)
  public List<ModInfoBean> getMods() {
    logger.debug("Loading available mods");
    return getMany("/mods", Mod.class).stream()
        .map(ModInfoBean::fromModInfo)
        .collect(Collectors.toList());
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

      hostServices.showDocument(fixedAuthorizationUrl);

      String code = verificationCodeReceiver.waitForCode();
      TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

      return flow.createAndStoreCredential(response, userId);
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
  private <T> List<T> getMany(String endpointPath, Class<T> type) {
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
    HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(baseUrl + endpointPath));
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
