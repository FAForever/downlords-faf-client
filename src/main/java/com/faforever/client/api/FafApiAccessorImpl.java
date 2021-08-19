package com.faforever.client.api;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.faforever.client.io.CountingFileSystemResource;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.ApiException;
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
import com.github.jasminb.jsonapi.exceptions.ResourceParseException;
import com.github.jasminb.jsonapi.models.errors.Errors;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static java.lang.String.format;

@SuppressWarnings("unchecked")
@Slf4j
@Component
@Profile("!offline")
@RequiredArgsConstructor
public class FafApiAccessorImpl implements FafApiAccessor, InitializingBean {

  private static final String REPLAY_ENDPOINT = "/data/game";
  private static final String MAP_ENDPOINT = "/data/map";
  private static final String MAP_VERSION_ENDPOINT = "/data/mapVersion";
  private static final String MOD_ENDPOINT = "/data/mod";
  private static final String MOD_VERSION_ENDPOINT = "/data/modVersion";
  private static final String ACHIEVEMENT_ENDPOINT = "/data/achievement";
  private static final String LEADERBOARD_ENDPOINT = "/data/leaderboard";
  private static final String LEADERBOARD_ENTRY_ENDPOINT = "/data/leaderboardRating";
  private static final String REPORT_ENDPOINT = "/data/moderationReport";
  private static final String TOURNAMENT_LIST_ENDPOINT = "/challonge/v1/tournaments.json";
  private static final String REPLAY_INCLUDES = "featuredMod,playerStats,playerStats.player,playerStats.ratingChanges,reviews," +
      "reviews.player,mapVersion,mapVersion.map,reviewsSummary";
  private static final String MAP_INCLUDES = "latestVersion,author,reviewsSummary," +
      "versions.reviews,versions.reviews.player";
  private static final String MAP_VERSION_INCLUDES = "map,map.latestVersion,map.author," +
      "map.reviewsSummary,map.versions.reviews,map.versions.reviews.player";
  private static final String MATCHMAKER_POOL_INCLUDES = "mapVersion,mapVersion.map,mapVersion.map.latestVersion," +
      "mapVersion.map.author,mapVersion.map.reviewsSummary,mapVersion.map.versions.reviews," +
      "mapVersion.map.versions.reviews.player";
  private static final String MOD_INCLUDES = "latestVersion,reviewsSummary,versions,versions.reviews," +
      "versions.reviews.player";
  private static final String MOD_VERSION_INCLUDES = "mod,mod.latestVersion,mod.versions,mod.versions.reviews," +
      "mod.versions.reviews.player,mod.reviewsSummary,mod.uploader";
  private static final String LEADERBOARD_ENTRY_INCLUDES = "player,leaderboard";
  private static final String COOP_RESULT_INCLUDES = "game.playerStats.player";
  private static final String PLAYER_INCLUDES = "names";
  private static final String REPORT_INCLUDES = "reporter,lastModerator,reportedUsers,game";
  private static final String FILTER = "filter";
  private static final String SORT = "sort";
  private static final String INCLUDE = "include";
  private static final String NOT_HIDDEN = "latestVersion.hidden==\"false\"";
  private static final String FILENAME_TEMPLATE = "maps/%s.zip";
  private static final String CLAN_INCLUDES = "leader,founder,memberships,memberships.player";

  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private final JsonApiReader jsonApiReader;
  private final JsonApiWriter jsonApiWriter;
  private final OAuthTokenFilter oAuthTokenFilter;

  private CountDownLatch authorizedLatch = new CountDownLatch(1);
  private WebClient webClient;

  private static String rsql(Condition<?> eq) {
    return eq.query(new RSQLVisitor());
  }

  private static <T extends QBuilder<T>> QBuilder<T> qBuilder() {
    return new QBuilder<>();
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  @Override
  public void authorize() {
    Api apiProperties = clientProperties.getApi();

    webClient = WebClient.builder()
        .baseUrl(apiProperties.getBaseUrl())
        .filter(oAuthTokenFilter)
        .codecs(clientCodecConfigurer -> {
          clientCodecConfigurer.customCodecs().register(jsonApiReader);
          clientCodecConfigurer.customCodecs().register(jsonApiWriter);
        })
        .build();

    authorizedLatch.countDown();
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    authorizedLatch = new CountDownLatch(1);
  }

  @Subscribe
  public void onSessionExpiredEvent(SessionExpiredEvent event) {
    authorizedLatch = new CountDownLatch(1);
  }

  @Override
  public Flux<PlayerAchievement> getPlayerAchievements(int playerId) {
    return getAll("/data/playerAchievement", java.util.Map.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  public Flux<PlayerEvent> getPlayerEvents(int playerId) {
    return getAll("/data/playerEvent", java.util.Map.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId))
    ));
  }

  @Override
  @Cacheable(value = CacheNames.ACHIEVEMENTS, sync = true)
  public Flux<AchievementDefinition> getAchievementDefinitions() {
    return getAll(ACHIEVEMENT_ENDPOINT, java.util.Map.of(
        SORT, "order"
    ));
  }

  @Override
  @Cacheable(value = CacheNames.ACHIEVEMENTS, sync = true)
  public Mono<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return getOne(ACHIEVEMENT_ENDPOINT + "/" + achievementId, AchievementDefinition.class);
  }

  @Override
  @Cacheable(value = CacheNames.MODS, sync = true)
  public Flux<Mod> getMods() {
    return getAll(MOD_ENDPOINT, java.util.Map.of(
        INCLUDE, MOD_INCLUDES));
  }

  @Override
  @Cacheable(value = CacheNames.FEATURED_MODS, sync = true)
  public Flux<com.faforever.commons.api.dto.FeaturedMod> getFeaturedMods() {
    return getMany("/data/featuredMod", 1000, java.util.Map.of());
  }

  @Override
  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public Flux<Leaderboard> getLeaderboards() {
    return getAll(LEADERBOARD_ENDPOINT);
  }

  @Override
  public Flux<LeaderboardEntry> getLeaderboardEntriesForPlayer(int playerId) {
    return getAll(LEADERBOARD_ENTRY_ENDPOINT, java.util.Map.of(
        FILTER, rsql(qBuilder().intNum("player.id").eq(playerId)),
        INCLUDE, LEADERBOARD_ENTRY_INCLUDES,
        SORT, "-rating"));
  }

  @Override
  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public Flux<LeaderboardEntry> getAllLeaderboardEntries(String leaderboardTechnicalName) {
    return getAll(LEADERBOARD_ENTRY_ENDPOINT, java.util.Map.of(
        FILTER, rsql(qBuilder().string("leaderboard.technicalName").eq(leaderboardTechnicalName)
            .and().instant("updateTime").after(LocalDateTime.now().minusMonths(1).toInstant(ZoneOffset.UTC), false)),
        INCLUDE, LEADERBOARD_ENTRY_INCLUDES,
        SORT, "-rating"));
  }

  @Override
  @Cacheable(value = CacheNames.LEADERBOARD, sync = true)
  public Mono<Tuple2<List<LeaderboardEntry>, Integer>> getLeaderboardEntriesWithTotalPages(String leaderboardId, int count, int page) {
    return getPageWithTotalPages(LEADERBOARD_ENTRY_ENDPOINT, count, page, ImmutableMap.of(
        INCLUDE, LEADERBOARD_ENTRY_INCLUDES,
        SORT, "-rating"));
  }

  @Override
  @Cacheable(value = CacheNames.RATING_HISTORY, sync = true)
  public Flux<LeaderboardRatingJournal> getRatingJournal(int playerId, int leaderboardId) {
    return getAll("/data/leaderboardRatingJournal", java.util.Map.of(
        FILTER, rsql(qBuilder()
            .intNum("gamePlayerStats.player.id").eq(playerId)
            .and()
            .intNum("leaderboard.id").eq(leaderboardId)),
        INCLUDE, "gamePlayerStats"));
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getRecommendedMapsWithTotalPages(int count, int page) {
    return getPageWithTotalPages(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        FILTER, rsql(qBuilder()
            .bool("recommended").isTrue())
    ));
  }

  @Override
  @Cacheable(value = CacheNames.MAPS, sync = true)
  public Mono<Tuple2<List<Map>, Integer>> getMostPlayedMapsWithTotalPages(int count, int page) {
    return getPageWithTotalPages(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-gamesPlayed"));
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getHighestRatedMapsWithTotalPages(int count, int page) {
    return getPageWithTotalPages(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-reviewsSummary.lowerBound"));
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getNewestMapsWithTotalPages(int count, int page) {
    return getPageWithTotalPages(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-updateTime",
        FILTER, NOT_HIDDEN
    ));
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getMapsByIdWithTotalPages(List<Integer> mapIdList, int count, int page) {
    String filterCriteria = mapIdList.stream()
        .map(Object::toString)
        .collect(Collectors.joining(",", "latestVersion.map.id=in=(", ")"));

    return getPageWithTotalPages(MAP_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MAP_INCLUDES,
        SORT, "-updateTime",
        FILTER, filterCriteria
    ));
  }

  @Override
  public Flux<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
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
  public Mono<Void> uploadMod(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    return postMultipartForm("/mods/upload", multipartContent);
  }

  @Override
  public Mono<Void> uploadMap(Path file, boolean isRanked, ByteCountListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    multipartContent.add("metadata", java.util.Map.of("isRanked", isRanked));
    return postMultipartForm("/maps/upload", multipartContent);
  }

  @Override
  public Mono<ModVersion> getModVersion(String uid) {
    return getMany(MOD_VERSION_ENDPOINT, 1,
        java.util.Map.of(FILTER, rsql(qBuilder().string("uid").eq(uid)), INCLUDE, MOD_VERSION_INCLUDES)
    )
        .cast(ModVersion.class)
        .next();
  }

  @Override
  @Cacheable(value = CacheNames.FEATURED_MOD_FILES, sync = true)
  public Flux<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    String endpoint = format("/featuredMods/%s/files/%s", featuredMod.getId(),
        Optional.ofNullable(version).map(String::valueOf).orElse("latest"));
    return getMany(endpoint, 10_000, java.util.Map.of());
  }

  @Override
  public Mono<Tuple2<List<Game>, Integer>> getNewestReplaysWithTotalPages(int count, int page) {
    return getPageWithTotalPages(REPLAY_ENDPOINT, count, page, java.util.Map.of(
        SORT, "-endTime",
        INCLUDE, REPLAY_INCLUDES,
        FILTER, "endTime=isnull=false"
    ));
  }

  @Override
  public Mono<Tuple2<List<Game>, Integer>> getHighestRatedReplaysWithTotalPages(int count, int page) {
    return getPageWithTotalPages("/data/gameReviewsSummary", count, page, java.util.Map.of(
        SORT, "-lowerBound",
        // TODO this was done in a rush, check what is actually needed
        INCLUDE, "game,game.featuredMod,game.playerStats,game.playerStats.player,game.playerStats.ratingChanges,game.reviews,game.reviews.player," +
            "game.mapVersion,game.mapVersion.map",
        FILTER, "game.endTime=isnull=false"
    ))
        .map(tuple -> tuple.mapT1(gameReviews ->
            gameReviews.stream()
                .map(gameReviewSummary -> ((GameReviewsSummary) gameReviewSummary).getGame())
                .collect(Collectors.toList()))
        );
  }

  @Override
  public Mono<Tuple2<List<Game>, Integer>> findReplaysByQueryWithTotalPages(String query, int maxResults, int page, SortConfig sortConfig) {
    return getPageWithTotalPages(REPLAY_ENDPOINT, maxResults, page, java.util.Map.of(
        FILTER, query + ";endTime=isnull=false",
        INCLUDE, REPLAY_INCLUDES,
        SORT, sortConfig.toQuery()
    ));
  }

  @Override
  public Mono<MapVersion> findMapByFolderName(String folderName) {
    return getMany(MAP_VERSION_ENDPOINT, 1, java.util.Map.of(
        FILTER, rsql(qBuilder().string("filename").eq(format(FILENAME_TEMPLATE, folderName))),
        INCLUDE, MAP_VERSION_INCLUDES))
        .cast(MapVersion.class)
        .next();
  }

  @Override
  public Mono<MapVersion> getMapLatestVersion(String mapFolderName) {
    String queryFilter = rsql(qBuilder()
        .string("filename").eq(format(FILENAME_TEMPLATE, mapFolderName))
        .and()
        .string("map.latestVersion.hidden").eq("false"));
    return getMany(MAP_VERSION_ENDPOINT, 1, java.util.Map.of(
        FILTER, queryFilter,
        INCLUDE, MAP_VERSION_INCLUDES
    ))
        .cast(MapVersion.class)
        .next();
  }

  @Override
  public Flux<Player> getPlayersByIds(Collection<Integer> playerIds) {
    List<String> ids = playerIds.stream().map(String::valueOf).collect(Collectors.toList());

    return getMany("/data/player", playerIds.size(), java.util.Map.of(
        INCLUDE, PLAYER_INCLUDES,
        FILTER, rsql(qBuilder().string("id").in(ids))));
  }

  @Override
  public Mono<Player> queryPlayerByName(String playerName) {
    return getAll("/data/player", java.util.Map.of(
        INCLUDE, PLAYER_INCLUDES,
        FILTER, rsql(qBuilder().string("login").eq(playerName))))
        .cast(Player.class)
        .next();

  }

  @SneakyThrows
  @Override
  public Mono<MeResult> getMe() {
    return getOne("/me", MeResult.class);
  }

  @Override
  public Mono<GameReview> createGameReview(GameReview review) {
    return post(REPLAY_ENDPOINT + "/" + review.getGame().getId() + "/reviews", review, GameReview.class);
  }

  @Override
  public Mono<Void> updateGameReview(GameReview review) {
    return patch("/data/gameReview/" + review.getId(), review);
  }

  @Override
  public Mono<ModVersionReview> createModVersionReview(ModVersionReview review) {
    return post(MOD_VERSION_ENDPOINT + "/" + review.getModVersion().getId() + "/reviews", review, ModVersionReview.class);
  }

  @Override
  public Mono<Void> updateModVersionReview(ModVersionReview review) {
    return patch("/data/modVersionReview/" + review.getId(), review);
  }

  @Override
  public Mono<MapVersionReview> createMapVersionReview(MapVersionReview review) {
    return post(MAP_VERSION_ENDPOINT + "/" + review.getMapVersion().getId() + "/reviews", review, MapVersionReview.class);
  }

  @Override
  public Mono<Void> updateMapVersionReview(MapVersionReview review) {
    return patch("/data/mapVersionReview/" + review.getId(), review);
  }

  @Override
  public Mono<Void> deleteGameReview(String id) {
    return delete("/data/gameReview/" + id);
  }

  @Override
  public Mono<Void> deleteMapVersionReview(String id) {
    return delete("/data/mapVersionReview/" + id);
  }

  @Override
  public Mono<Tuple2<List<Mod>, Integer>> findModsByQueryWithTotalPages(SearchConfig searchConfig, int count, int page) {
    MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>();
    if (searchConfig.hasQuery()) {
      parameterMap.add(FILTER, searchConfig.getSearchQuery() + ";" + NOT_HIDDEN);
    }
    parameterMap.add(INCLUDE, MOD_INCLUDES);
    parameterMap.add(SORT, searchConfig.getSortConfig().toQuery());
    return getPageWithTotalPages(MOD_ENDPOINT, count, page, parameterMap);
  }

  @Override
  public Mono<Tuple2<List<Mod>, Integer>> getRecommendedModsWithTotalPages(int count, int page) {
    return getPageWithTotalPages(MOD_ENDPOINT, count, page, java.util.Map.of(
        INCLUDE, MOD_INCLUDES,
        FILTER, rsql(qBuilder()
            .bool("recommended").isTrue())
    ));
  }

  @Override
  public Mono<Void> deleteModVersionReview(String id) {
    return delete("/data/modVersionReview/" + id);
  }

  @Override
  public Mono<Game> findReplayById(int id) {
    return getOne(REPLAY_ENDPOINT + "/" + id, Game.class, java.util.Map.of(INCLUDE, REPLAY_INCLUDES));
  }

  @SneakyThrows
  @Override
  @Cacheable(value = CacheNames.MATCHMAKER_POOLS, sync = true)
  public Flux<MapPoolAssignment> getMatchmakerPoolMaps(int matchmakerQueueId, float rating) {
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
  public Mono<MatchmakerQueue> getMatchmakerQueue(String technicalName) {
    return getAll("/data/matchmakerQueue", java.util.Map.of(
        INCLUDE, "leaderboard",
        FILTER, rsql(qBuilder().string("technicalName").eq(technicalName))))
        .cast(MatchmakerQueue.class)
        .next();
  }

  @Override
  public Flux<TutorialCategory> getTutorialCategories() {
    return getAll("/data/tutorialCategory",
        java.util.Map.of(INCLUDE, "tutorials,tutorials.mapVersion.map,tutorials.mapVersion.map.latestVersion," +
            "tutorials.mapVersion.map.author"));
  }

  @Override
  public Mono<Tuple2<List<MapVersion>, Integer>> getOwnedMapsWithTotalPages(int playerId, int loadMoreCount, int page) {
    return getPageWithTotalPages(MAP_VERSION_ENDPOINT, loadMoreCount, page, java.util.Map.of(
        INCLUDE, MAP_VERSION_INCLUDES,
        FILTER, rsql(qBuilder().string("map.author.id").eq(String.valueOf(playerId)))
    ));
  }

  @Override
  public Mono<Void> updateMapVersion(String id, MapVersion mapVersion) {
    return patch(format("/data/mapVersion/%s", id), mapVersion);
  }

  @Override
  @Cacheable(value = CacheNames.CLAN, sync = true)
  public Mono<Clan> getClanByTag(String tag) {
    return getMany("/data/clan", 1, java.util.Map.of(
        INCLUDE, CLAN_INCLUDES,
        FILTER, rsql(qBuilder().string("tag").eq(tag))
    ))
        .cast(Clan.class)
        .next();
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> findMapsByQueryWithTotalPages(SearchConfig searchConfig, int count, int page) {
    MultiValueMap<String, String> parameterMap = new LinkedMultiValueMap<>();
    if (searchConfig.hasQuery()) {
      parameterMap.add(FILTER, searchConfig.getSearchQuery() + ";" + NOT_HIDDEN);
    }
    parameterMap.add(INCLUDE, MAP_INCLUDES);
    parameterMap.add(SORT, searchConfig.getSortConfig().toQuery());
    return getPageWithTotalPages(MAP_ENDPOINT, count, page, parameterMap);
  }

  @Override
  @Cacheable(value = CacheNames.COOP_MAPS, sync = true)
  public Flux<CoopMission> getCoopMissions() {
    return getAll("/data/coopMission");
  }

  @Override
  @Cacheable(value = CacheNames.COOP_LEADERBOARD, sync = true)
  public Flux<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
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
  public Flux<Tournament> getAllTournaments() {
    authorizedLatch.await();
    return getAllNoPaging(TOURNAMENT_LIST_ENDPOINT, java.util.Map.of());
  }

  @Override
  public Flux<ModerationReport> getPlayerModerationReports(int playerId) {
    return getAllNoPaging(REPORT_ENDPOINT, java.util.Map.of(
        INCLUDE, REPORT_INCLUDES,
        FILTER, rsql(qBuilder().intNum("reporter.id").eq(playerId))))
        .cast(ModerationReport.class);
  }

  @Override
  public Mono<ModerationReport> postModerationReport(com.faforever.client.reporting.ModerationReport report) {
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
    return post(REPORT_ENDPOINT, body, ModerationReport.class);
  }

  @NotNull
  private MultiValueMap<String, Object> createFileMultipart(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new CountingFileSystemResource(file, listener));
    return form;
  }

  @SneakyThrows
  private Mono<Void> postMultipartForm(String endpointPath, Object request) {
    authorizedLatch.await();
    return retrieveMonoWithErrorHandling(Void.class, webClient.post().uri(endpointPath)
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(request))
        .doOnNext(object -> log.debug("Posted {} to {}", object, endpointPath));
  }

  @SneakyThrows
  private <T> Mono<T> post(String endpointPath, Object request, Class<T> type) {
    authorizedLatch.await();
    return retrieveMonoWithErrorHandling(type, webClient.post().uri(endpointPath)
        .contentType(MediaType.parseMediaType("application/vnd.api+json;charset=utf-8"))
        .bodyValue(request))
        .doOnNext(object -> log.debug("Posted {} to {} with type {}", object, endpointPath, type));
  }

  @SneakyThrows
  private Mono<Void> patch(String endpointPath, Object request) {
    authorizedLatch.await();
    return retrieveMonoWithErrorHandling(Void.class, webClient.patch().uri(endpointPath)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request))
        .doOnNext(aVoid -> log.debug("Patched {} at {}", request, endpointPath));
  }

  @SneakyThrows
  private Mono<Void> delete(String endpointPath) {
    authorizedLatch.await();
    return retrieveMonoWithErrorHandling(Void.class, webClient.delete().uri(endpointPath))
        .doOnNext(aVoid -> log.debug("Deleted {}", endpointPath));
  }

  @SneakyThrows
  private <T> Mono<T> getOne(String endpointPath, Class<T> type) {
    authorizedLatch.await();
    return retrieveMonoWithErrorHandling(type, webClient.get().uri(endpointPath))
        .cache()
        .doOnNext(object -> log.debug("Retrieved {} from {} with type {}", object, endpointPath, type));
  }

  @NotNull
  private <T> Mono<T> getOne(String endpointPath, Class<T> type, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(CollectionUtils.toMultiValueMap(multiValues))
        .build();

    return getOne(uriComponents.toUriString(), type);
  }

  private <T> Flux<T> getAll(String endpointPath) {
    return getAll(endpointPath, Collections.emptyMap());
  }

  private <T> Flux<T> getAll(String endpointPath, java.util.Map<String, Serializable> params) {
    return getMany(endpointPath, clientProperties.getApi().getMaxPageSize(), params);
  }

  @SneakyThrows
  private <T> Flux<T> getMany(String endpointPath, int count, java.util.Map<String, Serializable> params) {
    return getPage(endpointPath, count, 1, params);
  }

  private <T> Flux<T> getPage(String endpointPath, int pageSize, int page, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    return getPage(endpointPath, pageSize, page, CollectionUtils.toMultiValueMap(multiValues));
  }

  private <T> Mono<Tuple2<List<T>, Integer>> getPageWithTotalPages(String endpointPath, int pageSize, int page, java.util.Map<String, Serializable> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));

    return getPageWithTotalPages(endpointPath, pageSize, page, CollectionUtils.toMultiValueMap(multiValues));
  }

  @SneakyThrows
  private <T> Flux<T> getAllNoPaging(String endpointPath, java.util.Map<String, String> params) {
    java.util.Map<String, List<String>> multiValues = params.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> Collections.singletonList(String.valueOf(entry.getValue()))));
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(CollectionUtils.toMultiValueMap(multiValues))
        .build();

    authorizedLatch.await();
    String url = uriComponents.toUriString();
    return (Flux<T>) retrieveFluxWithErrorHandling(Object.class, webClient.get().uri(url))
        .cache()
        .doOnNext(list -> log.debug("Retrieved {} from {}", list, url));
  }

  @SneakyThrows
  private <T> Flux<T> getPage(String endpointPath, int pageSize, int page, MultiValueMap<String, String> params) {
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(params)
        .replaceQueryParam("page[size]", pageSize)
        .replaceQueryParam("page[number]", page)
        .build();

    authorizedLatch.await();
    String url = uriComponents.toUriString();
    return (Flux<T>) retrieveFluxWithErrorHandling(Object.class, webClient.get().uri(url))
        .cache()
        .doOnNext(list -> log.debug("Retrieved {} from {}", list, url));
  }

  @SneakyThrows
  private <T> Mono<Tuple2<List<T>, Integer>> getPageWithTotalPages(String endpointPath, int pageSize, int page, MultiValueMap<String, String> params) {
    UriComponents uriComponents = UriComponentsBuilder.fromPath(endpointPath)
        .queryParams(params)
        .replaceQueryParam("page[size]", pageSize)
        .replaceQueryParam("page[number]", page)
        .queryParam("page[totals]")
        .build();

    authorizedLatch.await();
    String url = uriComponents.toUriString();
    return retrieveMonoWithErrorHandling(JSONAPIDocument.class, webClient.get().uri(url))
        .map(jsonapiDocument -> (JSONAPIDocument<List<T>>) jsonapiDocument)
        .flatMap(document -> Mono.zip(
            Mono.fromCallable(document::get),
            Mono.fromCallable(document::getMeta)
                .map(meta -> ((java.util.Map<String, Integer>) meta.get("page")).get("totalPages"))))
        .switchIfEmpty(Mono.zip(Mono.just(List.of()), Mono.just(0)))
        .cache()
        .doOnNext(tuple -> log.debug("Retrieved {} from {}", tuple.getT1(), url));
  }

  private <T> Mono<T> retrieveMonoWithErrorHandling(Class<T> type, WebClient.RequestHeadersSpec<?> requestSpec) {
    return requestSpec.exchangeToMono(response -> {
      if (response.statusCode().equals(HttpStatus.OK)) {
        return response.bodyToMono(type);
      } else if (response.statusCode().equals(HttpStatus.BAD_REQUEST)) {
        return response.bodyToMono(type).onErrorMap(ResourceParseException.class, exception -> new ApiException(exception.getErrors().getErrors()));
      } else if (response.statusCode().is4xxClientError()) {
        return response.createException().flatMap(Mono::error);
      } else if (response.statusCode().is5xxServerError()) {
        return response.createException().flatMap(Mono::error);
      } else {
        log.warn("Unknown status returned by api");
        return response.createException().flatMap(Mono::error);
      }
    });
  }

  private <T> Flux<T> retrieveFluxWithErrorHandling(Class<T> type, WebClient.RequestHeadersSpec<?> requestSpec) {
    return requestSpec.exchangeToFlux(response -> {
      if (response.statusCode().equals(HttpStatus.OK)) {
        return response.bodyToFlux(type);
      } else if (response.statusCode().equals(HttpStatus.BAD_REQUEST)) {
        return response.bodyToFlux(Errors.class)
            .flatMap(errors -> Mono.error(new IllegalArgumentException(new ApiException(errors.getErrors()))));
      } else if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
        return response.createException().flatMapMany(Mono::error);
      } else if (response.statusCode().is4xxClientError()) {
        return response.bodyToFlux(Errors.class)
            .flatMap(errors -> Mono.error(new ApiException(errors.getErrors())));
      } else if (response.statusCode().is5xxServerError()) {
        return response.bodyToFlux(Errors.class)
            .flatMap(errors -> Mono.error(new ApiException(errors.getErrors())));
      } else {
        log.warn("Unknown status returned by api");
        return response.createException().flatMapMany(Flux::error);
      }
    });
  }
}
