package com.faforever.client.api;

import com.faforever.client.config.CacheNames;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.io.CountingFileContent;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.Mod;
import com.faforever.client.net.UriUtil;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.Replay;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow.Builder;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpMediaType;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.JsonToken;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.escape.Escaper;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.net.MediaType;
import com.google.common.net.UrlEscapers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;

@Lazy
@Component
@Profile("!local")
// TODO devide and conquer
public class FafApiAccessorImpl implements FafApiAccessor {

  private static final String HTTP_LOCALHOST = "http://localhost:";
  private static final String ENCODED_HTTP_LOCALHOST = HTTP_LOCALHOST.replace(":", "%3A").replace("/", "%2F");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final List<String> SCOPES = Arrays.asList("read_achievements", "read_events", "upload_map", "upload_mod", "write_account_data");

  private final JsonFactory jsonFactory;
  private final PreferencesService preferencesService;
  private final HttpTransport httpTransport;
  private final ClientHttpRequestFactory clientHttpRequestFactory;
  private final EventBus eventBus;

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

  private CountDownLatch authorizedLatch;

  @Inject
  public FafApiAccessorImpl(JsonFactory jsonFactory, PreferencesService preferencesService, HttpTransport httpTransport, ClientHttpRequestFactory clientHttpRequestFactory, EventBus eventBus) {
    authorizedLatch = new CountDownLatch(1);
    this.jsonFactory = jsonFactory;
    this.preferencesService = preferencesService;
    this.httpTransport = httpTransport;
    this.clientHttpRequestFactory = clientHttpRequestFactory;
    this.eventBus = eventBus;
  }

  @PostConstruct
  void postConstruct() throws IOException {
    Path oauthCredentialsDirectory = preferencesService.getPreferencesDirectory().resolve("oauth");
    dataStoreFactory = new FileDataStoreFactory(oauthCredentialsDirectory.toFile());
    eventBus.register(this);
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    authorizedLatch = new CountDownLatch(1);
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    authorize(event.getUserId(), event.getUsername(), event.getPassword());
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
  public void authorize(int playerId, String username, String password) {
    noCatch(() -> {
      // TODO until username/password login and/or re-authorization is implemented
      Files.deleteIfExists(dataStoreFactory.getDataDirectory().toPath().resolve("StoredCredential"));

      AuthorizationCodeFlow flow = new Builder(
          authorizationHeaderAccessMethod(),
          httpTransport,
          jsonFactory,
          new GenericUrl(oAuthTokenServerUrl),
          new ClientParametersAuthentication(oAuthClientId, oAuthClientSecret),
          oAuthClientId,
          oAuthUrl)
          .setDataStoreFactory(dataStoreFactory)
          .setScopes(SCOPES)
          .build();

      credential = authorize(flow, valueOf(playerId), username, password);
      requestFactory = httpTransport.createRequestFactory(credential);
      authorizedLatch.countDown();
    });
  }

  @Override
  @Cacheable(CacheNames.MODS)
  public List<Mod> getMods() {
    logger.debug("Loading available mods");
    return getMany("/mods", com.faforever.client.api.Mod.class).stream()
        .map(Mod::fromModInfo)
        .collect(toList());
  }

  @Override
  @Cacheable(CacheNames.FEATURED_MODS)
  public List<FeaturedMod> getFeaturedMods() {
    logger.debug("Getting featured mods");
    return getMany("/featured_mods", FeaturedMod.class);
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

  @Override
  public MapBean findMapByName(String mapId) {
    logger.debug("Searching map: {}", mapId);
    return MapBean.fromMap(getSingle("/maps/" + mapId, com.faforever.client.api.Map.class));
  }

  @Override
  @Cacheable(CacheNames.LEADERBOARD)
  public List<Ranked1v1EntryBean> getLeaderboardEntries(RatingType ratingType) {
    return getMany("/leaderboards/" + ratingType.getString(), LeaderboardEntry.class).stream()
        .map(Ranked1v1EntryBean::fromLeaderboardEntry)
        .collect(toList());
  }

  @Override
  public Ranked1v1Stats getRanked1v1Stats() {
    return getSingle("/leaderboards/1v1/stats", Ranked1v1Stats.class);
  }

  @Override
  public Ranked1v1EntryBean getRanked1v1EntryForPlayer(int playerId) {
    return Ranked1v1EntryBean.fromLeaderboardEntry(getSingle("/leaderboards/1v1/" + playerId, LeaderboardEntry.class));
  }

  @Override
  @Cacheable(CacheNames.RATING_HISTORY)
  public History getRatingHistory(RatingType ratingType, int playerId) {
    return getSingle(format("/players/%d/ratings/%s/history", playerId, ratingType.getString()), History.class);
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<MapBean> getMaps() {
    logger.debug("Getting all maps");
    // FIXME don't page 1
    return requestMaps("/maps", 1);
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<MapBean> getMostDownloadedMaps(int count) {
    logger.debug("Getting most downloaded maps");
    return requestMaps(format("/maps?page[size]=%d&sort=-downloads", count), 1);
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<MapBean> getMostPlayedMaps(int count) {
    logger.debug("Getting most played maps");
    return requestMaps(format("/maps?page[size]=%d&sort=-times_played", count), 1);
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<MapBean> getBestRatedMaps(int count) {
    logger.debug("Getting most liked maps");
    return requestMaps(format("/maps?page[size]=%d&sort=-rating", count), 1);
  }

  @Override
  public List<MapBean> getNewestMaps(int count) {
    logger.debug("Getting most liked maps");
    return requestMaps(format("/maps?page[size]=%d&sort=-create_time", count), 1);
  }

  @Override
  public void uploadMod(Path file, ByteCountListener listener) throws IOException {
    MultipartContent multipartContent = createFileMultipart(file, listener);
    post("/mods/upload", multipartContent);
  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException {
    MultipartContent multipartContent = createFileMultipart(file, listener);
    multipartContent.addPart(new MultipartContent.Part(
        new HttpHeaders().set("Content-Disposition", "form-data; name=\"metadata\";"),
        new JsonHttpContent(jsonFactory, new GenericJson() {
          {
            set("is_ranked", isRanked);
          }
        })));

    post("/maps/upload", multipartContent);
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) throws IOException {
    logger.debug("Changing password");

    HashMap<String, String> httpDict = new HashMap<>();
    // TODO this should not be necessary; we are oauthed so the server knows our username
    httpDict.put("name", username);
    httpDict.put("pw_hash_old", currentPasswordHash);
    httpDict.put("pw_hash_new", newPasswordHash);

    HttpContent httpContent = new UrlEncodedContent(httpDict);

    post("/users/change_password", httpContent);
  }

  @Override
  public Mod getMod(String uid) {
    return Mod.fromModInfo(getSingle("/mods/" + uid, com.faforever.client.api.Mod.class));
  }

  @Override
  @Cacheable(CacheNames.FEATURED_MOD_FILES)
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedModBean featuredModBean, Integer version) {
    String innerVersion = version == null ? "latest" : String.valueOf(version);
    return getMany(format("/featured_mods/%s/files/%s", featuredModBean.getId(), innerVersion), FeaturedModFile.class);
  }

  @Override
  public List<Replay> searchReplayByPlayer(String playerName) {
    return getMany("/replays?filter[player]=" + playerName, ReplayInfo.class)
        .parallelStream().map(Replay::fromReplayInfo).collect(Collectors.toList());
  }

  @Override
  public List<Replay> searchReplayByMap(String mapName) {
    return getMany("/replays?filter[map]=" + mapName, ReplayInfo.class)
        .parallelStream().map(Replay::fromReplayInfo).collect(Collectors.toList());
  }

  @Override
  public List<Replay> searchReplayByMod(FeaturedMod featuredMod) {
    return getMany("/replays?filter[mod]=" + featuredMod.getId(), ReplayInfo.class)
        .parallelStream().map(Replay::fromReplayInfo).collect(Collectors.toList());
  }

  @Override
  public List<Replay> getNewestReplays(int count) {
    return getMany(format("/replays?page[size]=%d&sort=-date", count), ReplayInfo.class)
        .parallelStream().map(Replay::fromReplayInfo).collect(Collectors.toList());
  }

  @Override
  public List<Replay> getHighestRatedReplays(int count) {
    return getMany(format("/replays?page[size]=%d&sort=-rating", count), ReplayInfo.class)
        .parallelStream().map(Replay::fromReplayInfo).collect(Collectors.toList());
  }

  @Override
  public List<Replay> getMostWatchedReplays(int count) {
    return getMany(format("/replays?page[size]=%d&sort=-plays", count), ReplayInfo.class)
        .parallelStream().map(Replay::fromReplayInfo).collect(Collectors.toList());
  }

  @Override
  @Cacheable(CacheNames.COOP_MAPS)
  public List<CoopMission> getCoopMissions() {
    logger.debug("Loading available coop missions");
    return getMany("/coop/missions", com.faforever.client.api.CoopMission.class)
        .stream().map(CoopMission::fromCoopInfo).collect(toList());
  }

  @Override
  @Cacheable(CacheNames.COOP_LEADERBOARD)
  public List<CoopLeaderboardEntry> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return getMany(String.format("/coop/leaderboards/%s/%d?page[size]=100", missionId, numberOfPlayers), CoopLeaderboardEntry.class);
  }

  @NotNull
  private MultipartContent createFileMultipart(Path file, ByteCountListener listener) {
    HttpMediaType mediaType = new HttpMediaType("multipart/form-data").setParameter("boundary", "__END_OF_PART__");
    MultipartContent multipartContent = new MultipartContent().setMediaType(mediaType);

    String fileName = file.getFileName().toString();
    CountingFileContent fileContent = new CountingFileContent(guessMediaType(fileName).toString(), file, listener);

    HttpHeaders headers = new HttpHeaders().set("Content-Disposition", format("form-data; name=\"file\"; filename=\"%s\"", fileName));

    return multipartContent.addPart(new MultipartContent.Part(headers, fileContent));
  }

  private void post(String endpointPath, HttpContent content) throws IOException {
    noCatch(() -> authorizedLatch.await());

    String url = baseUrl + endpointPath;
    logger.trace("Posting to: {}", url);
    HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(url), content)
        .setThrowExceptionOnExecuteError(false)
        .setParser(new JsonObjectParser(jsonFactory));
    credential.initialize(request);
    HttpResponse httpResponse = request.execute();

    if (httpResponse.getStatusCode() == 400) {
      throw new ApiException(httpResponse.parseAs(ErrorResponse.class));
    } else if (!httpResponse.isSuccessStatusCode()) {
      throw new HttpResponseException(httpResponse);
    }
  }

  @NotNull
  private MediaType guessMediaType(String fileName) {
    if (fileName.endsWith(".zip")) {
      return MediaType.ZIP;
    }
    return MediaType.OCTET_STREAM;
  }

  private List<MapBean> requestMaps(String query, int page) {
    logger.debug("Loading available maps");
    return getMany(query, Map.class, page)
        .stream()
        .map(MapBean::fromMap)
        .collect(toList());
  }

  private Credential authorize(AuthorizationCodeFlow flow, String userId, String username, String password) throws IOException {
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
    byte[] postData = ("username=" + escaper.escape(username) +
        "&password=" + escaper.escape(password) +
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
    noCatch(() -> authorizedLatch.await());

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
    if (idField != null) {
      ReflectionUtils.makeAccessible(idField);
      idField.set(object, id);
    }
    return object;
  }
}
