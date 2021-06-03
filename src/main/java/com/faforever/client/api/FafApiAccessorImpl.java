package com.faforever.client.api;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.faforever.client.io.CountingFileSystemResource;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.Clan;
import com.faforever.commons.api.dto.CoopMission;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.GameReviewsSummary;
import com.faforever.commons.api.dto.Leaderboard;
import com.faforever.commons.api.dto.LeaderboardEntry;
import com.faforever.commons.api.dto.LeaderboardRatingJournal;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapStatistics;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.MatchmakerQueue;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersion;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.ModerationReport;
import com.faforever.commons.api.dto.Player;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.api.dto.PlayerEvent;
import com.faforever.commons.api.dto.Tournament;
import com.faforever.commons.api.dto.TutorialCategory;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static java.lang.String.format;

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
  private static final String LEADERBOARD_ENDPOINT = "/data/leaderboard";
  private static final String LEADERBOARD_ENTRY_ENDPOINT = "/data/leaderboardRating";
  private static final String REPORT_ENDPOINT = "/data/moderationReport";
  private static final String TOURNAMENT_LIST_ENDPOINT = "/challonge/v1/tournaments.json";
  private static final String REPLAY_INCLUDES = "featuredMod,playerStats,playerStats.player,playerStats.ratingChanges,reviews," +
      "reviews.player,mapVersion,mapVersion.map,reviewsSummary";
  private static final String MAP_INCLUDES = "latestVersion,author,statistics,reviewsSummary," +
      "versions.reviews,versions.reviews.player";
  private static final String MAP_VERSION_INCLUDES = "map,map.latestVersion,map.author,map.statistics," +
      "map.reviewsSummary,map.versions.reviews,map.versions.reviews.player";
  private static final String MAP_STATISTICS_INCLUDES = "map,map.statistics,map.latestVersion,map.author," +
      "map.versions.reviews,map.versions.reviews.player,map.reviewsSummary";
  private static final String MATCHMAKER_POOL_INCLUDES = "mapVersion,mapVersion.map,mapVersion.map.latestVersion," +
      "mapVersion.map.author,mapVersion.map.statistics,mapVersion.map.reviewsSummary,mapVersion.map.versions.reviews," +
      "mapVersion.map.versions.reviews.player";
  private static final String MOD_INCLUDES = "latestVersion,reviewsSummary,versions,versions.reviews," +
      "versions.reviews.player";
  private static final String LEADERBOARD_ENTRY_INCLUDES = "player,leaderboard";
  private static final String COOP_RESULT_INCLUDES = "game.playerStats.player";
  private static final String PLAYER_INCLUDES = "names";
  private static final String REPORT_INCLUDES = "reporter,lastModerator,reportedUsers,game";
  private static final String OAUTH_TOKEN_PATH = "/oauth/token";
  private static final String FILTER = "filter";
  private static final String SORT = "sort";
  private static final String INCLUDE = "include";
  private static final String NOT_HIDDEN = "latestVersion.hidden==\"false\"";
  private static final String FILENAME_TEMPLATE = "maps/%s.zip";


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
    return getAll("/data/playerAchievement", java.util.Map.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    return getAll("/data/playerEvent", java.util.Map.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  @Cacheable(value = CacheNames.ACHIEVEMENTS, sync = true)
  public List<AchievementDefinition> getAchievementDefinitions() {
    return getAll(ACHIEVEMENT_ENDPOINT, java.util.Map.of(
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
    return getAll(MOD_ENDPOINT, java.util.Map.of(
        INCLUDE, MOD_INCLUDES));
  }

  @Override
  @Cacheable(value = CacheNames.FEATURED_MODS, sync = true)
  public List<com.faforever.commons.api.dto.FeaturedMod> getFeaturedMods() {
    return getMany("/data/featuredMod", 1000, java.util.Map.of());
  }

  @Override
  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public List<Leaderboard> getLeaderboards() {
    return getAll(LEADERBOARD_ENDPOINT);
  }

  @Override
  public List<LeaderboardEntry> getLeaderboardEntriesForPlayer(int playerId) {
    return getAll(LEADERBOARD_ENTRY_ENDPOINT, java.util.Map.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId)),
        INCLUDE, LEADERBOARD_ENTRY_INCLUDES,
        SORT, "-rating"));
  }

  @Override
  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public List<LeaderboardEntry> getAllLeaderboardEntries(String leaderboardTechnicalName) {
    return getAll(LEADERBOARD_ENTRY_ENDPOINT, java.util.Map.of(
        FILTER, rsql(qBuilder().string("leaderboard.technicalName").eq(leaderboardTechnicalName)
            .and().instant("updateTime").after(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC), false)),
        INCLUDE, LEADERBOARD_ENTRY_INCLUDES,
        SORT, "-rating"));
  }

  @Override
  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public Tuple<List<LeaderboardEntry>, java.util.Map<String, ?>> getLeaderboardEntriesWithMeta(String leaderboardId, int count, int page) {
    JSONAPIDocument<List<LeaderboardEntry>> jsonApiDoc = getPageWithMeta(LEADERBOARD_ENTRY_ENDPOINT, count, page, ImmutableMap.of(
        INCLUDE, LEADERBOARD_ENTRY_INCLUDES,
        SORT, "-rating"));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  @Cacheable(value = CacheNames.RATING_HISTORY, sync = true)
  public List<LeaderboardRatingJournal> getRatingJournal(int playerId, int leaderboardId) {
    return getAll("/data/leaderboardRatingJournal", java.util.Map.of(
        FILTER, rsql(qBuilder()
            .intNum("gamePlayerStats.player.id").eq(playerId)
            .and()
            .intNum("leaderboard.id").eq(leaderboardId)),
        INCLUDE, "gamePlayerStats"));
  }

  public int getRecommendedMapCount() {
    return getAll(MAP_ENDPOINT, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        FILTER, rsql(qBuilder()
            .bool("recommended").isTrue()))).size();
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getRecommendedMapsWithMeta(int count, int page) {
    JSONAPIDocument<List<Map>> jsonApiDoc = getPageWithMeta(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        FILTER, rsql(qBuilder()
            .bool("recommended").isTrue())
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  @Cacheable(value = CacheNames.MAPS, sync = true)
  public Tuple<List<Map>, java.util.Map<String, ?>> getMostPlayedMapsWithMeta(int count, int page) {
    JSONAPIDocument<List<MapStatistics>> jsonApiDoc = getPageWithMeta(MAP_STATISTICS_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_STATISTICS_INCLUDES,
        SORT, "-plays"));
    return new Tuple<>(jsonApiDoc.get().stream().map(MapStatistics::getMap).collect(Collectors.toList()), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getHighestRatedMapsWithMeta(int count, int page) {
    JSONAPIDocument<List<MapStatistics>> jsonApiDoc = getPageWithMeta(MAP_STATISTICS_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_STATISTICS_INCLUDES,
        SORT, "-map.reviewsSummary.lowerBound"));
    return new Tuple<>(jsonApiDoc.get().stream().map(MapStatistics::getMap).collect(Collectors.toList()), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getNewestMapsWithMeta(int count, int page) {
    JSONAPIDocument<List<Map>> jsonApiDoc = getPageWithMeta(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-updateTime",
        FILTER, NOT_HIDDEN
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getMapsByIdWithMeta(List<Integer> mapIdList, int count, int page) {
    String filterCriteria = mapIdList.stream()
        .map(Object::toString)
        .collect(Collectors.joining(",", "latestVersion.map.id=in=(", ")"));

    JSONAPIDocument<List<Map>> jsonApiDoc = getPageWithMeta(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-updateTime",
        FILTER, filterCriteria
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
    return getMany(REPLAY_ENDPOINT, count, java.util.Map.of(
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
    multipartContent.add("metadata", java.util.Map.of("isRanked", isRanked));
    post("/maps/upload", multipartContent, false);
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) {
    java.util.Map<String, String> body = java.util.Map.of(
        "currentPassword", currentPasswordHash,
        "newPassword", newPasswordHash
    );

    post("/users/changePassword", body, true);
  }

  @Override
  public ModVersion getModVersion(String uid) {
    return (ModVersion) getMany(MOD_VERSION_ENDPOINT, 1,
        java.util.Map.of(FILTER, rsql(qBuilder().string("uid").eq(uid)), INCLUDE, "mod,mod.latestVersion,mod.versions,mod.uploader")
    ).get(0);
  }

  @Override
  @Cacheable(value = CacheNames.FEATURED_MOD_FILES, sync = true)
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    String endpoint = format("/featuredMods/%s/files/%s", featuredMod.getId(),
        Optional.ofNullable(version).map(String::valueOf).orElse("latest"));
    return getMany(endpoint, 10_000, java.util.Map.of());
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> getNewestReplaysWithMeta(int count, int page) {
    JSONAPIDocument<List<Game>> jsonApiDoc = getPageWithMeta(REPLAY_ENDPOINT, count, page, java.util.Map.of(
        SORT, "-endTime",
        INCLUDE, REPLAY_INCLUDES,
        FILTER, "endTime=isnull=false"
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> getHighestRatedReplaysWithMeta(int count, int page) {
    JSONAPIDocument<List<GameReviewsSummary>> pageWithPageCount = getPageWithMeta("/data/gameReviewsSummary", count, page, java.util.Map.of(
        SORT, "-lowerBound",
        // TODO this was done in a rush, check what is actually needed
        INCLUDE, "game,game.featuredMod,game.playerStats,game.playerStats.player,game.playerStats.ratingChanges,game.reviews,game.reviews.player," +
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
    JSONAPIDocument<List<Game>> jsonApiDoc = getPageWithMeta(REPLAY_ENDPOINT, maxResults, page, java.util.Map.of(
        FILTER, query + ";endTime=isnull=false",
        INCLUDE, REPLAY_INCLUDES,
        SORT, sortConfig.toQuery()
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public Optional<MapVersion> findMapByFolderName(String folderName) {
    List<MapVersion> maps = getMany(MAP_VERSION_ENDPOINT, 1, java.util.Map.of(
        FILTER, rsql(qBuilder().string("filename").eq(format(FILENAME_TEMPLATE, folderName))),
        INCLUDE, MAP_VERSION_INCLUDES));
    if (maps.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(maps.get(0));
  }

  @Override
  public Optional<MapVersion> getMapLatestVersion(String mapFolderName) {
    String queryFilter = rsql(qBuilder()
        .string("filename").eq(format(FILENAME_TEMPLATE, mapFolderName))
        .and()
        .string("map.latestVersion.hidden").eq("false"));
    List<MapVersion> currentVersionMap = getMany(MAP_VERSION_ENDPOINT, 1, java.util.Map.of(
        FILTER, queryFilter,
        INCLUDE, MAP_VERSION_INCLUDES
    ));
    return Optional.ofNullable(currentVersionMap.isEmpty() ? null : currentVersionMap.get(0).getMap().getLatestVersion());
  }

  @Override
  public List<Player> getPlayersByIds(Collection<Integer> playerIds) {
    List<String> ids = playerIds.stream().map(String::valueOf).collect(Collectors.toList());

    return getMany("/data/player", playerIds.size(), java.util.Map.of(
        INCLUDE, PLAYER_INCLUDES,
        FILTER, rsql(qBuilder().string("id").in(ids))));
  }

  @Override
  public Optional<Player> queryPlayerByName(String playerName) {
    List<Player> players = getAll("/data/player", java.util.Map.of(
        INCLUDE, PLAYER_INCLUDES,
        FILTER, rsql(qBuilder().string("login").eq(playerName))));
    if (players.size() == 1) {
      return Optional.of(players.get(0));
    } else {
      return Optional.empty();
    }
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
      parameterMap.add(FILTER, searchConfig.getSearchQuery() + ";" + NOT_HIDDEN);
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
    try {
      return Optional.of(getOne(REPLAY_ENDPOINT + "/" + id, Game.class, java.util.Map.of(INCLUDE, REPLAY_INCLUDES)));
    } catch (HttpClientErrorException.NotFound e) {
      return Optional.empty();
    }
  }

  @SneakyThrows
  @Override
  @Cacheable(value = CacheNames.MATCHMAKER_POOLS, sync = true)
  public List<MapPoolAssignment> getMatchmakerPoolMaps(int matchmakerQueueId, float rating) {
    QBuilder qBuilder = new QBuilder<>();
    List<Condition<?>> conditions = new ArrayList<>();
    conditions.add(qBuilder().string("mapPool.matchmakerQueueMapPool.matchmakerQueue.id").eq(String.valueOf(matchmakerQueueId)));
    conditions.add(qBuilder().floatNum("mapPool.matchmakerQueueMapPool.minRating").lte(rating).or()
        .floatNum("mapPool.matchmakerQueueMapPool.minRating").ne(null));
    return getAll("/data/mapPoolAssignment", java.util.Map.of(
        INCLUDE, MATCHMAKER_POOL_INCLUDES,
        FILTER, rsql(qBuilder.and(conditions)).replace("ex", "isnull"),
        SORT, "mapVersion.width,mapVersion.map.displayName"));
  }

  @Override
  @Cacheable(value = CacheNames.MATCHMAKER_QUEUES, sync = true)
  public Optional<MatchmakerQueue> getMatchmakerQueue(String technicalName) {
    List<MatchmakerQueue> queue = getAll("/data/matchmakerQueue", java.util.Map.of(
        INCLUDE, "leaderboard",
        FILTER, rsql(qBuilder().string("technicalName").eq(technicalName))));
    if (queue.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(queue.get(0));
  }

  @Override
  public List<TutorialCategory> getTutorialCategories() {
    return getAll("/data/tutorialCategory",
        java.util.Map.of(INCLUDE, "tutorials,tutorials.mapVersion.map,tutorials.mapVersion.map.latestVersion," +
            "tutorials.mapVersion.map.author,tutorials.mapVersion.map.statistics"));
  }

  @Override
  public Tuple<List<MapVersion>, java.util.Map<String, ?>> getOwnedMapsWithMeta(int playerId, int loadMoreCount, int page) {
    JSONAPIDocument<List<MapVersion>> jsonApiDoc = getPageWithMeta(MAP_VERSION_ENDPOINT, loadMoreCount, page, java.util.Map.of(
        INCLUDE, MAP_VERSION_INCLUDES,
        FILTER, rsql(qBuilder().string("map.author.id").eq(String.valueOf(playerId)))
    ));
    return new Tuple<>(jsonApiDoc.get(), jsonApiDoc.getMeta());
  }

  @Override
  public void updateMapVersion(String id, MapVersion mapVersion) {
    patch(format("/data/mapVersion/%s", id), mapVersion, Void.class);
  }

  @Override
  @Cacheable(value = CacheNames.CLAN, sync = true)
  public Optional<Clan> getClanByTag(String tag) {
    List<Clan> clans = getMany("/data/clan", 1, java.util.Map.of(
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
      parameterMap.add(FILTER, searchConfig.getSearchQuery() + ";" + NOT_HIDDEN);
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

    return getMany("/data/coopResult", 1000, java.util.Map.of(
        FILTER, rsql(filterCondition),
        INCLUDE, COOP_RESULT_INCLUDES,
        SORT, "duration"
    ));
  }

  @Override
  @SneakyThrows
  public List<Tournament> getAllTournaments() {
    List<Tournament> tournaments = Arrays.asList(restOperations.getForObject(TOURNAMENT_LIST_ENDPOINT, Tournament[].class));
    log.debug("Retrieved {} from {}", tournaments, TOURNAMENT_LIST_ENDPOINT);
    return tournaments;
  }

  @Override
  public List<ModerationReport> getPlayerModerationReports(int playerId) {
    return getAllNoPaging(REPORT_ENDPOINT, java.util.Map.of(
        INCLUDE, REPORT_INCLUDES,
        FILTER, rsql(qBuilder().intNum("reporter.id").eq(playerId))));
  }

  @Override
  public void postModerationReport(com.faforever.client.reporting.ModerationReport report) {
    List<java.util.Map<String, String>> reportedUsers = new ArrayList<>();
    report.getReportedUsers().forEach(player -> reportedUsers.add(java.util.Map.of("type", "player", "id", String.valueOf(player.getId()))));
    java.util.Map<String, Object> relationships = new HashMap<>(java.util.Map.of("reportedUsers", java.util.Map.of("data", reportedUsers)));
    if (report.getGameId() != null) {
      relationships.put("game", java.util.Map.of("data", java.util.Map.of("type", "game", "id", report.getGameId())));
    }
    java.util.Map<String, Object> body = java.util.Map.of("data", List.of(java.util.Map.of(
        "type", "moderationReport",
        "attributes", java.util.Map.of("gameIncidentTimecode", report.getGameIncidentTimeCode(), "reportDescription", report.getReportDescription()),
        "relationships", relationships)));
    post(REPORT_ENDPOINT, body, false);
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
    log.debug("Posted {} to {} with type {}", request, endpointPath, type);
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

  private <T> T getOne(String endpointPath, Class<T> type) {
    T object = restOperations.getForObject(endpointPath, type, Collections.emptyMap());
    log.debug("Retrieved {} from {} with type {}", object, endpointPath, type);
    return object;
  }

  @SneakyThrows
  @NotNull
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
  private <T> List<T> getAllNoPaging(String endpointPath, java.util.Map<String, String> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(CollectionUtils.toMultiValueMap(multiValues))
        .build();

    authorizedLatch.await();
    List<T> objects = restOperations.getForObject(uriComponents.toUriString(), List.class);
    log.debug("Retrieved {} from {} with parameters {}", objects, endpointPath, params);
    return objects;
  }

  @SneakyThrows
  private <T> List<T> getPage(String endpointPath, int pageSize, int page, MultiValueMap<String, String> params) {
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(params)
        .replaceQueryParam("page[size]", pageSize)
        .replaceQueryParam("page[number]", page)
        .build();

    authorizedLatch.await();
    List<T> objects = restOperations.getForObject(uriComponents.toUriString(), List.class);
    log.debug("Retrieved {} from {} with parameters {}", objects, endpointPath, params);
    return objects;
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
    JSONAPIDocument<List<T>> objects = restOperations.getForObject(uriComponents.toUriString(), JSONAPIDocument.class);
    if (objects != null) {
      log.debug("Retrieved {} from {} with parameters {} with meta {}", objects.get(), endpointPath, params, objects.getMeta());
    } else {
      log.warn("Retrieved nothing from {} with parameters {}", endpointPath, params);
    }
    return objects;
  }
}
