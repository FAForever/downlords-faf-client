package com.faforever.client.api;

import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.CoopMission;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.MapStatistics;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Api;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.io.CountingFileSystemResource;
import com.faforever.client.io.ProgressListener;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Component
@Profile("!offline")
public class FafApiAccessorImpl implements FafApiAccessor {

  private static final String MAP_ENDPOINT = "/data/map";
  private final EventBus eventBus;
  private final RestTemplateBuilder restTemplateBuilder;
  private final ClientProperties clientProperties;

  private CountDownLatch authorizedLatch;
  private RestOperations restOperations;

  @Inject
  public FafApiAccessorImpl(EventBus eventBus, RestTemplateBuilder restTemplateBuilder, ClientProperties clientProperties) {
    this.eventBus = eventBus;
    this.restTemplateBuilder = restTemplateBuilder;
    this.clientProperties = clientProperties;
    authorizedLatch = new CountDownLatch(1);
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
        "filter[playerAchievement.player.id]", String.valueOf(playerId)
    ));
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    return getAll("/data/playerEvent", ImmutableMap.of(
        "filter[playerEvent.player.id]", String.valueOf(playerId)
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
    return getAll("/data/featuredMod");
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
    // This is not an ordinary JSON-API route and thus doesn't support paging, that's why it's called manually
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
        "filter[gamePlayerStats.player.id]", String.valueOf(playerId),
        "filter[gamePlayerStats.game.featuredMod.technicalName]", knownFeaturedMod.getTechnicalName()
    ));
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<Map> getAllMaps() {
    return getAll(MAP_ENDPOINT);
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<Map> getMostDownloadedMaps(int count) {
    return this.<MapStatistics>getMany("/data/mapStatistics", count, ImmutableMap.of(
        "include", "map,map.latestVersion",
        "sort", "-downloads")).stream()
        .map(MapStatistics::getMap)
        .collect(Collectors.toList());
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<Map> getMostPlayedMaps(int count) {
    return this.<MapStatistics>getMany("/data/mapStatistics", count, ImmutableMap.of(
        "include", "map,map.latestVersion",
        "sort", "-plays")).stream()
        .map(MapStatistics::getMap)
        .collect(Collectors.toList());
  }

  @Override
  @Cacheable(CacheNames.MAPS)
  public List<Map> getHighestRatedMaps(int count) {
    throw new UnsupportedOperationException("Not yet supported");
  }

  @Override
  public List<Map> getNewestMaps(int count) {
    return getMany("/data/map", count, ImmutableMap.of(
        "include", "latestVersion",
        "sort", "-updateTime"));
  }

  @Override
  public void uploadMod(Path file, ProgressListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    post("/mods/upload", multipartContent);
  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ProgressListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    post("/maps/upload", multipartContent);
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) {
    java.util.Map<String, String> body = new HashMap<>();
    // TODO this should not be necessary; we are oauthed so the server knows our username
    body.put("name", username);
    body.put("pw_hash_old", currentPasswordHash);
    body.put("pw_hash_new", newPasswordHash);

    post("/users/change_password", body);
  }

  @Override
  public Mod getMod(String uid) {
    return getOne("/mod/" + uid, Mod.class, ImmutableMap.of(
        "include", "latestVersion"));
  }

  @Override
  @Cacheable(CacheNames.FEATURED_MOD_FILES)
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    throw new UnsupportedOperationException("API support missing");
  }

  @Override
  public List<Game> getNewestReplays(int count) {
    return getAll("/data/game", ImmutableMap.of(
        "sort", "-endTime",
        "include", "featuredMod"
    ));
  }

  @Override
  public List<Game> getHighestRatedReplays(int count) {
    // FIXME implement once supported by API
    return Collections.emptyList();
  }

  @Override
  public List<Game> getMostWatchedReplays(int count) {
    // FIXME implement once supported by API
    return Collections.emptyList();
  }

  @Override
  public List<Game> findReplaysByQuery(String query) {
    return getAll("/data/game", ImmutableMap.of(
        "filter", query,
        "include", "featuredMod"
    ));
  }

  @Override
  @Cacheable(CacheNames.COOP_MAPS)
  public List<CoopMission> getCoopMissions() {
    return this.getAll("/data/coopMission");
  }

  @Override
  @Cacheable(CacheNames.COOP_LEADERBOARD)
  public List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return getMany("/data/coopLeaderboard", numberOfPlayers, ImmutableMap.of(
        "filter[playerCount]", numberOfPlayers,
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
  private MultiValueMap<String, Object> createFileMultipart(Path file, ProgressListener listener) {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new CountingFileSystemResource(file, listener));
    return form;
  }

  @SneakyThrows
  private void post(String endpointPath, Object request) {
    authorizedLatch.await();
    ResponseEntity<Void> entity = restOperations.postForEntity(endpointPath, request, Void.class);
    if (entity.getStatusCode() != HttpStatus.OK) {
      throw new FileUploadException(entity.getStatusCode());
    }
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private <T> T getOne(String endpointPath, Class<T> type) {
    return getOne(endpointPath, type, Collections.emptyMap());
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  private <T> T getOne(String endpointPath, Class<T> type, java.util.Map<String, Serializable> params) {
    authorizedLatch.await();
    return restOperations.getForObject(endpointPath, type, params);
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
    while ((current == null || !current.isEmpty()) && result.size() < count) {
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
    return (List<T>) restOperations.getForObject(uriComponents.getPath() + "?" + uriComponents.getQuery(), List.class);
  }

  public class FileUploadException extends RuntimeException {

    FileUploadException(HttpStatus statusCode) {
      super("Upload failed with status code: " + statusCode);
    }
  }
}
