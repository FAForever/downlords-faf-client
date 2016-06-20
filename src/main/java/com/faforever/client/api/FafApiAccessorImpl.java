package com.faforever.client.api;

import com.faforever.client.config.CacheNames;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.net.UriUtil;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.JsonToken;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.escape.Escaper;
import com.google.common.net.MediaType;
import com.google.common.net.UrlEscapers;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;

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
  HttpTransport httpTransport;
  @Resource
  UserService userService;
  @Resource
  ClientHttpRequestFactory clientHttpRequestFactory;

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
  URI oAuthLoginUrl;

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
  @Cacheable(CacheNames.LEADERBOARD)
  public List<Ranked1v1EntryBean> getRanked1v1Entries() {
    return getMany("/ranked1v1?filter[is_active]=true", LeaderboardEntry.class).stream()
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

  @Override
  public void uploadMod(InputStream inputStream) {
    upload("/mods/upload", inputStream);
  }

  private void upload(String endpointPath, InputStream inputStream) {
    if (requestFactory == null) {
      throw new IllegalStateException("authorize() must be called first");
    }

    String url = baseUrl + endpointPath;
    logger.trace("Posting to: {}", url);
    noCatch(() -> {

      MultipartContent content = new MultipartContent().setMediaType(
          new HttpMediaType("multipart/form-data")
              .setParameter("boundary", "__END_OF_PART__"));

      InputStreamContent fileContent = new InputStreamContent(MediaType.ZIP.toString(), inputStream);
      MultipartContent.Part part = new MultipartContent.Part(fileContent);
      part.setHeaders(new HttpHeaders().set(
          "Content-Disposition",
          "form-data; name=\"file\"; filename=\"tmp-mod-name.zip\""));
      content.addPart(part);

      HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(url), content);
      credential.initialize(request);
      int statusCode = request.execute().getStatusCode();
      if (statusCode != HttpStatusCodes.STATUS_CODE_OK) {
        throw new RuntimeException("Request failed with status code: " + statusCode);
      }
    });
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
    Credential credential = flow.loadCredential(userId);
    if (credential != null && (credential.getRefreshToken() != null || credential.getExpiresInSeconds() > 60)) {
      return credential;
    }

    // The redirect URI is irrelevant to this implementation, however the server requires one
    String redirectUri = "http://localhost:1337";
    AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);

    // Google's GenericUrl does not escape ":" and "/", but Flask (FAF's OAuth) requires it.
    URI fixedAuthorizationUri = UriUtil.fromString(authorizationUrl.build()
        .replaceFirst("uri=" + Pattern.quote(HTTP_LOCALHOST), "uri=" + ENCODED_HTTP_LOCALHOST));

    Escaper escaper = UrlEscapers.urlFormParameterEscaper();
    byte[] postData = ("username=" + escaper.escape(userService.getUsername()) +
        "&password=" + escaper.escape(userService.getPassword()) +
        "&next=" + fixedAuthorizationUri).getBytes(StandardCharsets.UTF_8);
    int postDataLength = postData.length;

    ClientHttpRequest loginRequest = clientHttpRequestFactory.createRequest(oAuthLoginUrl, HttpMethod.POST);
    loginRequest.getHeaders().add("Content-Length", Integer.toString(postDataLength));
    try (DataOutputStream outputStream = new DataOutputStream(loginRequest.getBody())) {
      outputStream.write(postData);
    }
    ClientHttpResponse loginResponse = loginRequest.execute();

    if (!loginResponse.getStatusCode().is3xxRedirection()) {
      throw new RuntimeException("Could not log in (" + loginResponse.getStatusCode() + ")");
    }

    String cookie = Joiner.on("").join(loginResponse.getHeaders().get("set-cookie"));

    postData = "allow=yes".getBytes(StandardCharsets.UTF_8);
    postDataLength = postData.length;

    ClientHttpRequest authorizeRequest = clientHttpRequestFactory.createRequest(fixedAuthorizationUri, HttpMethod.POST);
    authorizeRequest.getHeaders().add("Content-Length", Integer.toString(postDataLength));
    authorizeRequest.getHeaders().add("Cookie", cookie);
    try (DataOutputStream outputStream = new DataOutputStream(authorizeRequest.getBody())) {
      outputStream.write(postData);
    }
    ClientHttpResponse authorizeResponse = authorizeRequest.execute();
    URI redirectLocation = authorizeResponse.getHeaders().getLocation();

    if (!authorizeResponse.getStatusCode().is3xxRedirection()
        || !redirectLocation.toString().contains("code=")) {
      throw new RuntimeException("Could not authorize (" + authorizeResponse.getStatusCode() + ")");
    }

    String code = UriComponentsBuilder.fromUri(redirectLocation).build().getQueryParams().get("code").get(0);

    TokenResponse tokenResponse = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

    return flow.createAndStoreCredential(tokenResponse, userId);
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
    String innerEndpointPath = endpointPath;
    if (page > 0) {
      innerEndpointPath += endpointPath.contains("?") ? "&" : "?";
      innerEndpointPath += "page[number]=" + page;
    }

    ArrayList<T> result = new ArrayList<>();
    try (InputStream inputStream = executeGet(innerEndpointPath)) {
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
