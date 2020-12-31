package com.faforever.client.api;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.Clan;
import com.faforever.client.api.dto.CoopMission;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.GameReviewsSummary;
import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1Map;
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.MapStatistics;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.MeResult;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.ModVersion;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.Player;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.api.dto.Tournament;
import com.faforever.client.api.dto.TutorialCategory;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.io.CountingFileSystemResource;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.io.ByteCountListener;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("!offline")
@RequiredArgsConstructor
public class FafApiAccessorImpl implements FafApiAccessor, InitializingBean {

  private static final String REPLAY_ENDPOINT = "/data/game";
  private static final String MAP_ENDPOINT = "/data/map";
  private static final String MAP_VERSION_ENDPOINT = "/data/mapVersion";
  private static final String MAP_STATISTICS_ENDPOINT = "/data/mapStatistics";
  private static final String MOD_ENDPOINT = "/data/mod";
  private static final String MOD_VERSION_ENDPOINT = "/data/modVersion";
  private static final String ACHIEVEMENT_ENDPOINT = "/data/achievement";
  private static final String TOURNAMENT_LIST_ENDPOINT = "/challonge/v1/tournaments.json";
  private static final String REPLAY_INCLUDES = "featuredMod,playerStats,playerStats.player,reviews," +
      "reviews.player,mapVersion,mapVersion.map,reviewsSummary";
  private static final String MAP_INCLUDES = "latestVersion,author,statistics,reviewsSummary," +
      "versions.reviews,versions.reviews.player";
  private static final String MAP_VERSION_INCLUDES = "map,map.latestVersion,map.author,map.statistics," +
      "map.reviewsSummary,map.versions.reviews,map.versions.reviews.player";
  private static final String MAP_STATISTICS_INCLUDES = "map,map.statistics,map.latestVersion,map.author," +
      "map.versions.reviews,map.versions.reviews.player,map.reviewsSummary";
  private static final String MOD_INCLUDES = "latestVersion,reviewsSummary,versions,versions.reviews," +
      "versions.reviews.player";
  private static final String COOP_RESULT_INCLUDES = "game.playerStats.player";
  private static final String PLAYER_INCLUDES = "globalRating,ladder1v1Rating,names";
  private static final String OAUTH_TOKEN_PATH = "/oauth/token";
  private static final String FILTER = "filter";
  private static final String SORT = "sort";
  private static final String INCLUDE = "include";
  private static final String HIDDEN = "latestVersion.hidden==\"false\"";


  private final EventBus eventBus;
  private final RestTemplateBuilder unconfiguredTemplateBuilder;
  private final ClientProperties clientProperties;
  private final JsonApiMessageConverter jsonApiMessageConverter;
  private final JsonApiErrorHandler jsonApiErrorHandler;
  private final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();

  private RestTemplateBuilder templateBuilder;
  private CountDownLatch authorizedLatch = new CountDownLatch(1);
  private RestOperations restOperations;

  private static String rsql(Condition<?> eq) {
    return eq.query(new RSQLVisitor());
  }

  private static <T extends QBuilder<T>> QBuilder<T> qBuilder() {
    return new QBuilder<>();
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    templateBuilder = unconfiguredTemplateBuilder
        .requestFactory(() -> requestFactory)
        .additionalMessageConverters(jsonApiMessageConverter)
        .errorHandler(jsonApiErrorHandler);
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
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    return getAll("/data/playerAchievement", ImmutableMap.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    return getAll("/data/playerEvent", ImmutableMap.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  @Cacheable(value = CacheNames.ACHIEVEMENTS, sync = true)
  public List<AchievementDefinition> getAchievementDefinitions() {
    return getAll(ACHIEVEMENT_ENDPOINT, ImmutableMap.of(
        SORT, "order"
    ));
  }

  @Override
  @Cacheable(value = CacheNames.ACHIEVEMENTS, sync = true)
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    return getOne(ACHIEVEMENT_ENDPOINT + "/" + achievementId, AchievementDefinition.class);
  }

  @Override
  @Cacheable(value = CacheNames.MODS, sync = true)
  public List<Mod> getMods() {
    return getAll(MOD_ENDPOINT, ImmutableMap.of(
        INCLUDE, MOD_INCLUDES));
  }

  @Override
  @Cacheable(value = CacheNames.FEATURED_MODS, sync = true)
  public List<com.faforever.client.api.dto.FeaturedMod> getFeaturedMods() {
    return getMany("/data/featuredMod", 1000, ImmutableMap.of());
  }

  @Override
  @Cacheable(value = CacheNames.GLOBAL_LEADERBOARD, sync = true)
  @SneakyThrows
  public List<GlobalLeaderboardEntry> getGlobalLeaderboard() {
    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it's called manually
    authorizedLatch.await();
    return restOperations.getForObject("/leaderboards/global", List.class,
        ImmutableMap.of(
            SORT, "-rating",
            INCLUDE, "player",
            "fields[globalRating]", "rating,numGames",
            "fields[player]", "login"
        ));
  }

  @Override
  @Cacheable(value = CacheNames.LADDER_1V1_LEADERBOARD, sync = true)
  @SneakyThrows
  public List<Ladder1v1LeaderboardEntry> getLadder1v1Leaderboard() {
    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it doesn't use getAll()
    authorizedLatch.await();
    return restOperations.getForObject("/leaderboards/ladder1v1", List.class,
        ImmutableMap.of(
            SORT, "-rating",
            INCLUDE, "player",
            "fields[ladder1v1Rating]", "rating,numGames,winGames",
            "fields[player]", "login"
        ));
  }

  @Override
  public Ladder1v1LeaderboardEntry getLadder1v1EntryForPlayer(int playerId) {
    return getOne("/leaderboards/ladder1v1/" + playerId, Ladder1v1LeaderboardEntry.class);
  }

  @Override
  @Cacheable(value = CacheNames.RATING_HISTORY, sync = true)
  public List<GamePlayerStats> getGamePlayerStats(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return getAll("/data/gamePlayerStats", ImmutableMap.of(
        FILTER, rsql(qBuilder()
            .intNum("player.id").eq(playerId)
            .and()
            .string("game.featuredMod.technicalName").eq(knownFeaturedMod.getTechnicalName())
        )));
  }

  @Override
  @Cacheable(value = CacheNames.MAPS, sync = true)
  public Tuple<List<Map>, java.util.Map<String, ?>> getMostPlayedMapsWithMeta(int count, int page) {
    JSONAPIDocument<List<MapStatistics>> jsonApiDoc = getPageWithMeta(MAP_STATISTICS_ENDPOINT, count, page, ImmutableMap.of(
        INCLUDE, MAP_STATISTICS_INCLUDES,
        SORT, "-plays"));
    return new Tuple<>(jsonApiDoc.get().stream().map(MapStatistics::getMap).collect(Collectors.toList()), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getHighestRatedMapsWithMeta(int count, int page) {
    JSONAPIDocument<List<MapStatistics>> jsonApiDoc = getPageWithMeta(MAP_STATISTICS_ENDPOINT, count, page, ImmutableMap.of(
        INCLUDE, MAP_STATISTICS_INCLUDES,
        SORT, "-map.reviewsSummary.lowerBound"));
    return new Tuple<>(jsonApiDoc.get().stream().map(MapStatistics::getMap).collect(Collectors.toList()), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getNewestMapsWithMeta(int count, int page) {
    JSONAPIDocument<List<Map>> jsonApiDoc = getPageWithMeta(MAP_ENDPOINT, count, page, ImmutableMap.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-updateTime",
        FILTER, HIDDEN
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getMapsByIdWithMeta(List<Integer> mapIdList, int count, int page) {
    String filterCriteria = mapIdList.stream()
        .map(Object::toString)
        .collect(Collectors.joining(",", "latestVersion.map.id=in=(", ")"));

    JSONAPIDocument<List<Map>> jsonApiDoc = getPageWithMeta(MAP_ENDPOINT, count, page, ImmutableMap.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-updateTime",
        FILTER, filterCriteria
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
    return getMany(REPLAY_ENDPOINT, count, ImmutableMap.of(
        FILTER, rsql(qBuilder()
            .string("mapVersion.id").eq(mapVersionId)
            .and()
            .intNum("playerStats.player.id").eq(playerId)),
        INCLUDE, REPLAY_INCLUDES,
        SORT, "-endTime"
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
    java.util.Map<String, String> body = ImmutableMap.of(
        "currentPassword", currentPasswordHash,
        "newPassword", newPasswordHash
    );

    post("/users/changePassword", body, true);
  }

  @Override
  public ModVersion getModVersion(String uid) {
    return (ModVersion) getMany(MOD_VERSION_ENDPOINT, 1,
        ImmutableMap.of(FILTER, rsql(qBuilder().string("uid").eq(uid)), INCLUDE, "mod,mod.latestVersion,mod.versions,mod.uploader")
    ).get(0);
  }

  @Override
  @Cacheable(value = CacheNames.FEATURED_MOD_FILES, sync = true)
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    String endpoint = String.format("/featuredMods/%s/files/%s", featuredMod.getId(),
        Optional.ofNullable(version).map(String::valueOf).orElse("latest"));
    return getMany(endpoint, 10_000, ImmutableMap.of());
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> getNewestReplaysWithMeta(int count, int page) {
    JSONAPIDocument<List<Game>> jsonApiDoc = getPageWithMeta(REPLAY_ENDPOINT, count, page, ImmutableMap.of(
        SORT, "-endTime",
        INCLUDE, REPLAY_INCLUDES,
        FILTER, "endTime=isnull=false"
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> getHighestRatedReplaysWithMeta(int count, int page) {
    JSONAPIDocument<List<GameReviewsSummary>> pageWithPageCount = getPageWithMeta("/data/gameReviewsSummary", count, page, ImmutableMap.of(
        SORT, "-lowerBound",
        // TODO this was done in a rush, check what is actually needed
        INCLUDE, "game,game.featuredMod,game.playerStats,game.playerStats.player,game.reviews,game.reviews.player," +
            "game.mapVersion,game.mapVersion.map",
        FILTER, "game.endTime=isnull=false"
    ));
    return new Tuple<>(pageWithPageCount.get().stream()
        .map(GameReviewsSummary::getGame)
        .collect(Collectors.toList()),
        pageWithPageCount.getMeta());
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> findReplaysByQueryWithMeta(String query, int maxResults, int page, SortConfig sortConfig) {
    JSONAPIDocument<List<Game>> jsonApiDoc = getPageWithMeta(REPLAY_ENDPOINT, maxResults, page, ImmutableMap.of(
        FILTER, "(" + query + ");endTime=isnull=false",
        INCLUDE, REPLAY_INCLUDES,
        SORT, sortConfig.toQuery()
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public Optional<MapVersion> findMapByFolderName(String folderName) {
    List<MapVersion> maps = getMany(MAP_VERSION_ENDPOINT, 1, ImmutableMap.of(
        FILTER, String.format("filename==\"*%s*\"", folderName),
        INCLUDE, MAP_VERSION_INCLUDES));
    if (maps.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(maps.get(0));
  }

  @Override
  public List<Player> getPlayersByIds(Collection<Integer> playerIds) {
    List<String> ids = playerIds.stream().map(String::valueOf).collect(Collectors.toList());

    return getMany("/data/player", playerIds.size(), ImmutableMap.of(
        INCLUDE, PLAYER_INCLUDES,
        FILTER, rsql(qBuilder().string("id").in(ids))
    ));
  }

  @Override
  public MeResult getOwnPlayer() {
    return getOne("/me", MeResult.class);
  }

  @Override
  public GameReview createGameReview(GameReview review) {
    return post(REPLAY_ENDPOINT + "/" + review.getGame().getId() + "/reviews", review, GameReview.class);
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
    return post(MAP_VERSION_ENDPOINT + "/" + review.getMapVersion().getId() + "/reviews", review, MapVersionReview.class);
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
  public Tuple<List<Mod>, java.util.Map<String, ?>> findModsByQueryWithMeta(SearchConfig searchConfig, int count, int page) {
    MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>();
    if (searchConfig.hasQuery()) {
      parameterMap.add(FILTER, searchConfig.getSearchQuery() + ";" + HIDDEN);
    }
    parameterMap.add(INCLUDE, MOD_INCLUDES);
    parameterMap.add(SORT, searchConfig.getSortConfig().toQuery());
    JSONAPIDocument<List<Mod>> jsonApiDoc = getPageWithMeta(MOD_ENDPOINT, count, page, parameterMap);
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public void deleteModVersionReview(String id) {
    delete("/data/modVersionReview/" + id);
  }

  @Override
  public Optional<Game> findReplayById(int id) {
    return Optional.ofNullable(getOne(REPLAY_ENDPOINT + id, Game.class, ImmutableMap.of(INCLUDE, REPLAY_INCLUDES)));
  }

  @Override
  public Tuple<List<Ladder1v1Map>, java.util.Map<String, ?>> getLadder1v1MapsWithMeta(int count, int page) {
    JSONAPIDocument<List<Ladder1v1Map>> jsonApiDoc = getPageWithMeta("/data/ladder1v1Map", count, page, ImmutableMap.of(
        INCLUDE, "mapVersion,mapVersion.map,mapVersion.map.latestVersion,mapVersion.map.author," +
            "mapVersion.map.statistics,mapVersion.map.reviewsSummary,mapVersion.map.versions.reviews," +
            "mapVersion.map.versions.reviews.player"));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public List<TutorialCategory> getTutorialCategories() {
    return getAll("/data/tutorialCategory",
        ImmutableMap.of(INCLUDE, "tutorials,tutorials.mapVersion.map,tutorials.mapVersion.map.latestVersion," +
            "tutorials.mapVersion.map.author,tutorials.mapVersion.map.statistics"));
  }

  @Override
  public Tuple<List<MapVersion>, java.util.Map<String, ?>> getOwnedMapsWithMeta(int playerId, int loadMoreCount, int page) {
    JSONAPIDocument<List<MapVersion>> jsonApiDoc = getPageWithMeta(MAP_VERSION_ENDPOINT, loadMoreCount, page, ImmutableMap.of(
        INCLUDE, MAP_VERSION_INCLUDES,
        FILTER, rsql(qBuilder().string("map.author.id").eq(String.valueOf(playerId)))
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public void updateMapVersion(String id, MapVersion mapVersion) {
    patch(String.format("/data/mapVersion/%s", id), mapVersion, Void.class);
  }

  @Override
  @Cacheable(value = CacheNames.CLAN, sync = true)
  public Optional<Clan> getClanByTag(String tag) {
    List<Clan> clans = getMany("/data/clan", 1, ImmutableMap.of(
        INCLUDE, "leader,founder,memberships,memberships.player",
        FILTER, rsql(qBuilder().string("tag").eq(tag))
    ));
    if (clans.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(clans.get(0));
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> findMapsByQueryWithMeta(SearchConfig searchConfig, int count, int page) {
    MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>();
    if (searchConfig.hasQuery()) {
      parameterMap.add(FILTER, searchConfig.getSearchQuery() + ";" + HIDDEN);
    }
    parameterMap.add(INCLUDE, MAP_INCLUDES);
    parameterMap.add(SORT, searchConfig.getSortConfig().toQuery());
    JSONAPIDocument<List<Map>> jsonApiDoc = getPageWithMeta(MAP_ENDPOINT, count, page, parameterMap);
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public Optional<MapVersion> findMapVersionById(String id) {
    // FIXME: that is not gonna work this way
    //FIXME: filter hidden maps
    return Optional.ofNullable(getOne(MAP_ENDPOINT + "/" + id, MapVersion.class));
  }

  @Override
  @Cacheable(value = CacheNames.COOP_MAPS, sync = true)
  public List<CoopMission> getCoopMissions() {
    return getAll("/data/coopMission");
  }

  @Override
  @Cacheable(value = CacheNames.COOP_LEADERBOARD, sync = true)
  public List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    Condition<?> filterCondition = qBuilder().string("mission").eq(missionId);

    if (numberOfPlayers > 0) {
      filterCondition = filterCondition.and().intNum("playerCount").eq(numberOfPlayers);
    }

    return getMany("/data/coopResult", 1000, ImmutableMap.of(
        FILTER, rsql(filterCondition),
        INCLUDE, COOP_RESULT_INCLUDES,
        SORT, "duration"
    ));
  }

  @Override
  @SneakyThrows
  public List<Tournament> getAllTournaments() {
    return Arrays.asList(restOperations.getForObject(TOURNAMENT_LIST_ENDPOINT, Tournament[].class));
  }

  @Override
  @SneakyThrows
  public void authorize(int playerId, String username, String password) {
    Api apiProperties = clientProperties.getApi();

    ResourceOwnerPasswordResourceDetails details = new ResourceOwnerPasswordResourceDetails();
    details.setClientId(apiProperties.getClientId());
    details.setClientSecret(apiProperties.getClientSecret());
    details.setClientAuthenticationScheme(AuthenticationScheme.header);
    details.setAccessTokenUri(apiProperties.getBaseUrl() + OAUTH_TOKEN_PATH);
    details.setUsername(username);
    details.setPassword(password);

    restOperations = templateBuilder
        // Base URL can be changed in login window
        .rootUri(apiProperties.getBaseUrl())
        .configure(new OAuth2RestTemplate(details));

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

  @SneakyThrows
  private <T> T getOne(String endpointPath, Class<T> type) {
    return restOperations.getForObject(endpointPath, type, Collections.emptyMap());
  }

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

  private <T> JSONAPIDocument<List<T>> getPageWithMeta(String endpointPath, int pageSize, int page, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    return getPageWithMeta(endpointPath, pageSize, page, CollectionUtils.toMultiValueMap(multiValues));
  }

  @SneakyThrows
  private <T> List<T> getPage(String endpointPath, int pageSize, int page, MultiValueMap<String, String> params) {
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(params)
        .replaceQueryParam("page[size]", pageSize)
        .replaceQueryParam("page[number]", page)
        .build();

    authorizedLatch.await();
    return restOperations.getForObject(uriComponents.toUriString(), List.class);
  }

  @SneakyThrows
  private <T> JSONAPIDocument<List<T>> getPageWithMeta(String endpointPath, int pageSize, int page, MultiValueMap<String, String> params) {
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(params)
        .replaceQueryParam("page[size]", pageSize)
        .replaceQueryParam("page[number]", page)
        .queryParam("page[totals]")
        .build();

    authorizedLatch.await();
    return restOperations.getForObject(uriComponents.toUriString(), JSONAPIDocument.class);
  }
}
