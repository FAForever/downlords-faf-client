package com.faforever.client.api;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.Clan;
import com.faforever.client.api.dto.CoopMission;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.MapStatistics;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.Player;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.io.CountingFileSystemResource;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.io.ByteCountListener;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Component
@Profile("!offline")
public class FafApiAccessorImpl implements FafApiAccessor {

  private static final String MAP_ENDPOINT = "/data/map";
  private static final String REPLAY_INCLUDES = "featuredMod,playerStats,playerStats.player,reviews,reviews.player,mapVersion,mapVersion.map,mapVersion.reviews";
  private final EventBus eventBus;
  private final RestTemplateBuilder restTemplateBuilder;
  private final ClientProperties clientProperties;
  private final HttpComponentsClientHttpRequestFactory requestFactory;

  private CountDownLatch authorizedLatch;
  private RestOperations restOperations;

  @Inject
  public FafApiAccessorImpl(EventBus eventBus, RestTemplateBuilder restTemplateBuilder,
                            ClientProperties clientProperties, JsonApiMessageConverter jsonApiMessageConverter,
                            JsonApiErrorHandler jsonApiErrorHandler) {
    this.eventBus = eventBus;
    this.clientProperties = clientProperties;
    authorizedLatch = new CountDownLatch(1);

    requestFactory = new HttpComponentsClientHttpRequestFactory();
    this.restTemplateBuilder = restTemplateBuilder
        .requestFactory(requestFactory)
        .additionalMessageConverters(jsonApiMessageConverter)
        .errorHandler(jsonApiErrorHandler)
        .rootUri(clientProperties.getApi().getBaseUrl());
  }

  private static String rsql(Condition<?> eq) {
    return eq.query(new RSQLVisitor());
  }

  private static <T extends QBuilder<T>> QBuilder<T> qBuilder() {
    return new QBuilder<>();
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    authorizedLatch = new CountDownLatch(1);
    restOperations = null;
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    authorize(event.getUserId(), event.getUsername(), event.getPassword());
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    return getAll("/data/playerAchievement", ImmutableMap.of(
        "filter", rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    return getAll("/data/playerEvent", ImmutableMap.of(
        "filter", rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  @SuppressWarnings("unchecked")
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public List<AchievementDefinition> getAchievementDefinitions() {
    return getAll("/data/achievement", ImmutableMap.of(
        "sort", "order"
    ));
  }

  @Override
  @Cacheable(CacheNames.ACHIEVEMENTS)
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    return getOne("/data/achievement/" + achievementId, AchievementDefinition.class);
  }

  @Override
  @Cacheable(CacheNames.MODS)
  public List<Mod> getMods() {
    return getAll("/data/mod", ImmutableMap.of(
        "include", "latestVersion"));
  }

  @Override
  @Cacheable(CacheNames.FEATURED_MODS)
  public List<com.faforever.client.api.dto.FeaturedMod> getFeaturedMods() {
    return getMany("/data/featuredMod", 1000, ImmutableMap.of());
  }

  @Override
  @Cacheable(CacheNames.GLOBAL_LEADERBOARD)
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public List<GlobalLeaderboardEntry> getGlobalLeaderboard() {
    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it's called manually
    authorizedLatch.await();
    return restOperations.getForObject("/leaderboards/global", List.class,
        ImmutableMap.of(
            "sort", "-rating",
            "include", "player",
            "fields[globalRating]", "rating,numGames",
            "fields[player]", "login"
        ));
  }

  @Override
  @Cacheable(CacheNames.LADDER_1V1_LEADERBOARD)
  @SneakyThrows
  @SuppressWarnings("unchecked")
  public List<Ladder1v1LeaderboardEntry> getLadder1v1Leaderboard() {
    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it doesn't use getAll()
    authorizedLatch.await();
    return restOperations.getForObject("/leaderboards/ladder1v1", List.class,
        ImmutableMap.of(
            "sort", "-rating",
            "include", "player",
            "fields[ladder1v1Rating]", "rating,numGames,winGames",
            "fields[player]", "login"
        ));
  }

  @Override
  public Ladder1v1LeaderboardEntry getLadder1v1EntryForPlayer(int playerId) {
    return getOne("/leaderboards/ladder1v1/" + playerId, Ladder1v1LeaderboardEntry.class);
  }

  @Override
  @Cacheable(CacheNames.RATING_HISTORY)
  public List<GamePlayerStats> getGamePlayerStats(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return getAll("/data/gamePlayerStats", ImmutableMap.of(
        "filter", rsql(qBuilder()
            .intNum("player.id").eq(playerId)
            .and()
            .string("game.featuredMod.technicalName").eq(knownFeaturedMod.getTechnicalName())
        )));
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<Map> getMostPlayedMaps(int count, int page) {
    return this.<MapStatistics>getPage("/data/mapStatistics", count, page, ImmutableMap.of(
        "include", "map,map.latestVersion,map.author,map.versions.reviews",
        "sort", "-plays")).stream()
        .map(MapStatistics::getMap)
        .collect(Collectors.toList());
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<Map> getHighestRatedMaps(int count, int page) {
    // FIXME https://github.com/FAForever/downlords-faf-client/issues/547
    // In order to be able to sort by rating, the database and API need to be extended
    // I (Downlord) already started the DB part locally
    return this.<MapStatistics>getPage("/data/mapStatistics", count, page, ImmutableMap.of(
        "include", "map,map.latestVersion,map.author,map.versions.reviews",
        "sort", "-plays")).stream()
        .map(MapStatistics::getMap)
        .collect(Collectors.toList());
  }

  @Override
  public List<Map> getNewestMaps(int count, int page) {
    return getPage(MAP_ENDPOINT, count, page, ImmutableMap.of(
        "include", "latestVersion,author,versions.reviews",
        "sort", "-updateTime"));
  }

  @Override
  public List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
    return getMany("/data/game", count, ImmutableMap.of(
        "filter", rsql(qBuilder()
            .string("mapVersion.id").eq(mapVersionId)
            .and()
            .intNum("playerStats.player.id").eq(playerId)),
        "sort", "-endTime"
    ));
  }

  @Override
  public void uploadMod(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    post("/mods/upload", multipartContent, false);
  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ByteCountListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    multipartContent.add("metadata", ImmutableMap.of("isRanked", isRanked));
    post("/maps/upload", multipartContent, false);
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) {
    java.util.Map<String, String> body = new HashMap<>();
    // TODO this should not be necessary; we are oauthed so the server knows our username
    body.put("name", username);
    body.put("pw_hash_old", currentPasswordHash);
    body.put("pw_hash_new", newPasswordHash);

    post("/users/change_password", body, true);
  }

  @Override
  public Mod getMod(String uid) {
    return getOne("/data/mod/" + uid, Mod.class, ImmutableMap.of(
        "include", "latestVersion"));
  }

  @Override
  @Cacheable(CacheNames.FEATURED_MOD_FILES)
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    String endpoint = String.format("/featuredMods/%s/files/%s", featuredMod.getId(),
        Optional.ofNullable(version).map(String::valueOf).orElse("latest"));
    return getMany(endpoint, 10_000, ImmutableMap.of());
  }

  @Override
  public List<Game> getNewestReplays(int count, int page) {
    return getPage("/data/game", count, page, ImmutableMap.of(
        "sort", "-endTime",
        "include", REPLAY_INCLUDES,
        "filter", "endTime=isnull=false"
    ));
  }

  @Override
  public List<Game> getHighestRatedReplays(int count, int page) {
    // FIXME implement once supported by API
    return Collections.emptyList();
  }

  @Override
  public List<Game> getMostWatchedReplays(int count, int page) {
    // FIXME implement once supported by API
    return Collections.emptyList();
  }

  @Override
  public List<Game> findReplaysByQuery(String query, int maxResults, int page) {
    return getPage("/data/game", maxResults, page, ImmutableMap.of(
        "filter", "(" + query + ");endTime=isnull=false",
        "include", REPLAY_INCLUDES
    ));
  }

  @Override
  public Optional<MapVersion> findMapByFolderName(String folderName) {
    List<MapVersion> maps = getMany(MAP_ENDPOINT, 1, ImmutableMap.of(
        "include", "latestVersion,author",
        "sort", "-updateTime"));
    if (maps.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(maps.get(0));
  }

  @Override
  public List<Player> getPlayersByIds(Collection<Integer> playerIds) {
    List<String> ids = playerIds.stream().map(String::valueOf).collect(Collectors.toList());

    return getMany("/data/player", playerIds.size(), ImmutableMap.of(
        "include", "globalRating,ladder1v1Rating",
        "filter", rsql(qBuilder().string("id").in(ids))
    ));
  }

  @Override
  public GameReview createGameReview(GameReview review) {
    return post("/data/game/" + review.getGame().getId() + "/reviews", review, GameReview.class);
  }

  @Override
  public void updateGameReview(GameReview review) {
    patch("/data/gameReview/" + review.getId(), review, Void.class);
  }

  @Override
  public ModVersionReview createModVersionReview(ModVersionReview review) {
    return post("/data/modVersion/" + review.getModVersion().getId() + "/reviews", review, ModVersionReview.class);
  }

  @Override
  public void updateModVersionReview(ModVersionReview review) {
    patch("/data/modVersionReview/" + review.getId(), review, Void.class);
  }

  @Override
  public MapVersionReview createMapVersionReview(MapVersionReview review) {
    return post("/data/mapVersion/" + review.getMapVersion().getId() + "/reviews", review, MapVersionReview.class);
  }

  @Override
  public void updateMapVersionReview(MapVersionReview review) {
    patch("/data/mapVersionReview/" + review.getId(), review, Void.class);
  }

  @Override
  public void deleteGameReview(String id) {
    delete("/data/gameReview/" + id);
  }

  @Override
  public void deleteMapVersionReview(String id) {
    delete("/data/mapVersionReview/" + id);
  }

  @Override
  public void deleteModVersionReview(String id) {
    delete("/data/modVersionReview/" + id);
  }

  @Override
  public Optional<Game> findReplayById(int id) {
    return Optional.ofNullable(getOne("/data/game/" + id, Game.class, ImmutableMap.of("include", REPLAY_INCLUDES)));
  }

  @Override
  public Optional<Clan> getClanByTag(String tag) {
    List<Clan> clans = getMany("/data/clan", 1, ImmutableMap.of(
        "include", "leader,founder,memberships,memberships.player",
        "filter", rsql(qBuilder().string("tag").eq(tag))
    ));
    if (clans.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(clans.get(0));
  }

  @Override
  public List<Map> findMapsByQuery(String query, int page, int maxResults) {
    return getPage(MAP_ENDPOINT, maxResults, page, ImmutableMap.of(
        "filter", query,
        "include", "latestVersion,latestVersion.reviews,author,statistics"
    ));
  }

  @Override
  public Optional<MapVersion> findMapVersionById(String id) {
    // TODO check what is returned if map does not exist
    return Optional.ofNullable(getOne(MAP_ENDPOINT + "/" + id, MapVersion.class));
  }

  @Override
  @Cacheable(CacheNames.COOP_MAPS)
  public List<CoopMission> getCoopMissions() {
    return this.getAll("/data/coopMission");
  }

  @Override
  @Cacheable(CacheNames.COOP_LEADERBOARD)
  public List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return getMany("/data/coopResult", numberOfPlayers, ImmutableMap.of(
        "filter", rsql(qBuilder().intNum("playerCount").eq(numberOfPlayers)),
        "sort", "-duration"
    ));
  }

  @Override
  @SneakyThrows
  public void authorize(int playerId, String username, String password) {
    Api apiProperties = clientProperties.getApi();

    ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
    details.setClientId(apiProperties.getClientId());
    details.setClientSecret(apiProperties.getClientSecret());
    details.setClientAuthenticationScheme(AuthenticationScheme.header);
    details.setAccessTokenUri(apiProperties.getAccessTokenUri());
    details.setUsername(username);
    details.setPassword(password);

    restOperations = restTemplateBuilder.configure(new OAuth2RestTemplate(details));

    authorizedLatch.countDown();
  }

  @NotNull
  private MultiValueMap<String, Object> createFileMultipart(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new CountingFileSystemResource(file, listener));
    return form;
  }

  @SneakyThrows
  private void post(String endpointPath, Object request, boolean bufferRequestBody) {
    authorizedLatch.await();
    requestFactory.setBufferRequestBody(bufferRequestBody);

    try {
      // Don't use Void.class here, otherwise Spring won't even try to deserialize error messages in the body
      restOperations.postForEntity(endpointPath, request, String.class);
    } finally {
      requestFactory.setBufferRequestBody(true);
    }
  }

  @SneakyThrows
  private <T> T post(String endpointPath, Object request, Class<T> type) {
    authorizedLatch.await();
    ResponseEntity<T> entity = restOperations.postForEntity(endpointPath, request, type);
    return entity.getBody();
  }

  @SneakyThrows
  private <T> T patch(String endpointPath, Object request, Class<T> type) {
    authorizedLatch.await();
    return restOperations.patchForObject(endpointPath, request, type);
  }

  private void delete(String endpointPath) {
    restOperations.delete(endpointPath);
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private <T> T getOne(String endpointPath, Class<T> type) {
    return restOperations.getForObject(endpointPath, type, Collections.emptyMap());
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private <T> T getOne(String endpointPath, Class<T> type, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(CollectionUtils.toMultiValueMap(multiValues))
        .build();

    authorizedLatch.await();
    return getOne(uriComponents.toUriString(), type);
  }

  private <T> List<T> getAll(String endpointPath) {
    return getAll(endpointPath, Collections.emptyMap());
  }

  private <T> List<T> getAll(String endpointPath, java.util.Map<String, Serializable> params) {
    return getMany(endpointPath, clientProperties.getApi().getMaxPageSize(), params);
  }

  @SneakyThrows
  private <T> List<T> getMany(String endpointPath, int count, java.util.Map<String, Serializable> params) {
    List<T> result = new LinkedList<>();
    List<T> current = null;
    int page = 1;
    int maxPageSize = clientProperties.getApi().getMaxPageSize();
    while ((current == null || current.size() >= maxPageSize) && result.size() < count) {
      current = getPage(endpointPath, count, page++, params);
      result.addAll(current);
    }
    return result;
  }

  private <T> List<T> getPage(String endpointPath, int pageSize, int page, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    return getPage(endpointPath, pageSize, page, CollectionUtils.toMultiValueMap(multiValues));
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private <T> List<T> getPage(String endpointPath, int pageSize, int page, MultiValueMap<String, String> params) {
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(params)
        .replaceQueryParam("page[size]", pageSize)
        .replaceQueryParam("page[number]", page)
        .build();

    authorizedLatch.await();
    return (List<T>) restOperations.getForObject(uriComponents.toUriString(), List.class);
  }
}
