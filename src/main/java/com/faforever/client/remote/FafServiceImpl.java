package com.faforever.client.remote;

import com.faforever.client.api.AchievementDefinition;
import com.faforever.client.api.CoopLeaderboardEntry;
import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.FeaturedMod;
import com.faforever.client.api.FeaturedModFile;
import com.faforever.client.api.PlayerAchievement;
import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.api.RatingType;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.faforever.client.config.CacheNames;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.mod.Mod;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.GameEndedMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.SdpRecordClientMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.replay.Replay;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.Long.parseLong;
import static java.time.LocalDateTime.ofEpochSecond;
import static java.time.ZoneOffset.UTC;

@Lazy
@Service
public class FafServiceImpl implements FafService {

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final EventBus eventBus;

  @Inject
  public FafServiceImpl(FafServerAccessor fafServerAccessor, FafApiAccessor fafApiAccessor, ThreadPoolExecutor threadPoolExecutor, EventBus eventBus) {
    this.fafServerAccessor = fafServerAccessor;
    this.fafApiAccessor = fafApiAccessor;
    this.threadPoolExecutor = threadPoolExecutor;
    this.eventBus = eventBus;
  }

  @Override
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.addOnMessageListener(type, listener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.removeOnMessageListener(type, listener);
  }

  @Override
  public CompletionStage<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return fafServerAccessor.requestHostGame(newGameInfo);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  @Override
  public CompletionStage<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return fafServerAccessor.requestJoinGame(gameId, password
    );
  }

  @Override
  public CompletionStage<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port) {
    return fafServerAccessor.startSearchRanked1v1(faction);
  }

  @Override
  public void stopSearchingRanked() {
    fafServerAccessor.stopSearchingRanked();
  }

  @Override
  public void sendGpgGameMessage(GpgGameMessage message) {
    fafServerAccessor.sendGpgMessage(message);
  }

  @Override
  public CompletionStage<LoginMessage> connectAndLogIn(String username, String password) {
    return fafServerAccessor.connectAndLogIn(username, password);
  }

  @Override
  public void disconnect() {
    fafServerAccessor.disconnect();
  }

  @Override
  public void addFriend(Player player) {
    fafServerAccessor.addFriend(player.getId());
  }

  @Override
  public void addFoe(Player player) {
    fafServerAccessor.addFoe(player.getId());
  }

  @Override
  public void removeFriend(Player player) {
    fafServerAccessor.removeFriend(player.getId());
  }

  @Override
  public void removeFoe(Player player) {
    fafServerAccessor.removeFoe(player.getId());
  }

  @Override
  public CompletionStage<Ranked1v1Stats> getRanked1v1Stats() {
    return CompletableFuture.supplyAsync(fafApiAccessor::getRanked1v1Stats, threadPoolExecutor);
  }

  @Override
  public CompletionStage<Ranked1v1EntryBean> getRanked1v1EntryForPlayer(int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1EntryForPlayer(playerId), threadPoolExecutor);
  }

  @Override
  public void notifyGameEnded() {
    fafServerAccessor.sendGpgMessage(new GameEndedMessage());
  }

  @Override
  public List<MapBean> getMaps() {
    return fafApiAccessor.getMaps();
  }

  @Override
  public MapBean findMapByName(String mapName) {
    return fafApiAccessor.findMapByName(mapName);
  }

  @Override
  public List<Mod> getMods() {
    return fafApiAccessor.getMods();
  }

  @Override
  public Mod getMod(String uid) {
    return fafApiAccessor.getMod(uid);
  }

  @Override
  public void reconnect() {
    fafServerAccessor.reconnect();
  }

  @Override
  public CompletionStage<List<MapBean>> getMostDownloadedMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getMostDownloadedMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<MapBean>> getMostPlayedMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getMostPlayedMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<MapBean>> getMostLikedMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getBestRatedMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletionStage<List<MapBean>> getNewestMaps(int count) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getNewestMaps(count), threadPoolExecutor);
  }

  @Override
  public CompletableFuture<List<CoopMission>> getCoopMaps() {
    return CompletableFuture.supplyAsync(fafApiAccessor::getCoopMissions);
  }

  @Override
  public CompletionStage<List<AvatarBean>> getAvailableAvatars() {
    return CompletableFuture.supplyAsync(fafServerAccessor::getAvailableAvatars)
        .thenApply(avatars -> avatars.stream()
            .map(AvatarBean::fromAvatar)
            .collect(Collectors.toList()));
  }

  @Override
  public void selectAvatar(AvatarBean avatar) {
    fafServerAccessor.selectAvatar(avatar == null ? null : avatar.getUrl());
    eventBus.post(new AvatarChangedEvent(avatar));
  }

  @Override
  @CacheEvict(CacheNames.MODS)
  public void evictModsCache() {
    // Nothing to see, please move along
  }

  @Override
  public CompletableFuture<List<CoopLeaderboardEntry>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getCoopLeaderboard(mission.getId(), numberOfPlayers));
  }

  @Override
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(RatingType ratingType, int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRatingHistory(ratingType, playerId)
        .getData().entrySet()
        .stream()
        .sorted(Comparator.comparingLong(o -> parseLong(o.getKey())))
        .map(entry -> new RatingHistoryDataPoint(ofEpochSecond(parseLong(entry.getKey()), 0, UTC), entry.getValue().get(0), entry.getValue().get(1)))
        .collect(Collectors.toList())
    );
  }

  @Override
  public void sendSdp(int remotePlayerId, String sdp) {
    fafServerAccessor.sendGpgMessage(new SdpRecordClientMessage(remotePlayerId, sdp));
  }

  @Override
  public CompletableFuture<List<FeaturedModBean>> getFeaturedMods() {
    return CompletableFuture.supplyAsync(fafApiAccessor::getFeaturedMods)
        .thenApply(featuredMods -> featuredMods.stream()
            .sorted(Comparator.comparingInt(FeaturedMod::getDisplayOrder))
            .map(FeaturedModBean::fromFeaturedMod)
            .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedModBean featuredMod, Integer version) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getFeaturedModFiles(featuredMod, version));
  }

  @Override
  public CompletionStage<List<Ranked1v1EntryBean>> getLeaderboardEntries(KnownFeaturedMod mod) {
    RatingType ratingType;
    switch (mod) {
      case FAF:
        ratingType = RatingType.GLOBAL;
        break;
      case LADDER_1V1:
        ratingType = RatingType.LADDER_1V1;
        break;
      default:
        throw new IllegalArgumentException("Not supported: " + mod);
    }
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getLeaderboardEntries(ratingType));
  }

  @Override
  public CompletableFuture<List<Replay>> searchReplayByPlayer(String playerName) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.searchReplayByPlayer(playerName));
  }

  @Override
  public CompletionStage<List<Replay>> getNewestReplays(int topElementCount) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getNewestReplays(topElementCount));
  }

  @Override
  public CompletionStage<List<Replay>> getHighestRatedReplays(int topElementCount) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getHighestRatedReplays(topElementCount));
  }

  @Override
  public CompletionStage<List<Replay>> getMostWatchedReplays(int topElementCount) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getMostWatchedReplays(topElementCount));
  }

  @Override
  public void uploadMod(Path modFile, ByteCountListener byteListener) {
    fafApiAccessor.uploadMod(modFile, byteListener);
  }

  @Override
  public CompletionStage<List<PlayerAchievement>> getPlayerAchievements(int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getPlayerAchievements(playerId));
  }

  @Override
  public CompletionStage<List<AchievementDefinition>> getAchievementDefinitions() {
    return CompletableFuture.supplyAsync(fafApiAccessor::getAchievementDefinitions);
  }

  @Override
  public CompletionStage<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getAchievementDefinition(achievementId));
  }

  @Override
  public CompletableFuture<List<Replay>> searchReplayByMap(String mapName) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.searchReplayByMap(mapName));
  }

  @Override
  public CompletableFuture<List<Replay>> searchReplayByMod(FeaturedMod featuredMod) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.searchReplayByMod(featuredMod));
  }
}
