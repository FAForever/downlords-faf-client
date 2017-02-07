package com.faforever.client.api;

import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.Mod;
import com.faforever.client.replay.Replay;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Lazy
@Component
@Profile("!local")
// TODO devide and conquer
public class FafApiAccessorImpl implements FafApiAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ClientProperties clientProperties;
  private final EventBus eventBus;

  private CountDownLatch authorizedLatch;

  @Inject
  public FafApiAccessorImpl(ClientProperties clientProperties, EventBus eventBus) {
    this.clientProperties = clientProperties;
    this.eventBus = eventBus;
    authorizedLatch = new CountDownLatch(1);
  }

  @PostConstruct
  void postConstruct() throws IOException {
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
  public void uploadMod(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> multipartContent = createFileMultipart(file, listener);
    noCatch(() -> post("/mods/upload", multipartContent));
  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ByteCountListener listener) throws IOException {
    // FIXME fix with #481
//    post("/maps/upload", multipartContent);
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) throws IOException {
    logger.debug("Changing password");

    HashMap<String, String> httpDict = new HashMap<>();
    // TODO this should not be necessary; we are oauthed so the server knows our username
    httpDict.put("name", username);
    httpDict.put("pw_hash_old", currentPasswordHash);
    httpDict.put("pw_hash_new", newPasswordHash);

    post("/users/change_password", httpDict);
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
  private MultiValueMap<String, Object> createFileMultipart(Path file, ByteCountListener listener) {
    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
    form.add("file", new FileSystemResource(file.toFile()));
    return form;
  }

  private void post(String endpointPath, Object content) throws IOException {
    // FIXME fix with #481
  }

  private List<MapBean> requestMaps(String query, int page) {
    logger.debug("Loading available maps");
    return getMany(query, Map.class, page)
        .stream()
        .map(MapBean::fromMap)
        .collect(toList());
  }

  @SuppressWarnings("unchecked")
  private <T> T getSingle(String endpointPath, Class<T> type) {
    // FIXME fix with #481
    return null;
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> getMany(String endpointPath, Class<T> type, int page) {
    // FIXME fix with #481
    return null;
  }

  private String buildUrl(String endpointPath) {
    return clientProperties.getApi().getBaseUrl() + endpointPath;
  }
}
