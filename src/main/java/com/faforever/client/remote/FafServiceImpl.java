package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.faforever.client.config.CacheNames;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.io.ProgressListener;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.Mod;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.GameEndedMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.IceMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.replay.Replay;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Lazy
@Service
public class FafServiceImpl implements FafService {

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final EventBus eventBus;

  @Inject
  public FafServiceImpl(FafServerAccessor fafServerAccessor, FafApiAccessor fafApiAccessor, EventBus eventBus) {
    this.fafServerAccessor = fafServerAccessor;
    this.fafApiAccessor = fafApiAccessor;
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
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return fafServerAccessor.requestHostGame(newGameInfo);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return fafServerAccessor.requestJoinGame(gameId, password);
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchLadder1v1(Faction faction, int port) {
    return fafServerAccessor.startSearchLadder1v1(faction);
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
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
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
  @Async
  public void notifyGameEnded() {
    fafServerAccessor.sendGpgMessage(new GameEndedMessage());
  }

  @Override
  @Async
  public CompletableFuture<LeaderboardEntry> getLadder1v1EntryForPlayer(int playerId) {
    return CompletableFuture.completedFuture(LeaderboardEntry.fromLadder1v1(fafApiAccessor.getLadder1v1EntryForPlayer(playerId)));
  }

  @Override
  @Async
  public CompletableFuture<List<MapBean>> getMaps() {
    return CompletableFuture.completedFuture(fafApiAccessor.getAllMaps().parallelStream()
        .map(MapBean::fromMap)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<Mod>> getMods() {
    return CompletableFuture.completedFuture(fafApiAccessor.getMods().stream()
        .map(Mod::fromDto)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<Mod> getMod(String uid) {
    return CompletableFuture.completedFuture(Mod.fromDto(fafApiAccessor.getMod(uid)));
  }

  @Override
  public void reconnect() {
    fafServerAccessor.reconnect();
  }

  @Override
  @Async
  public CompletableFuture<List<MapBean>> getMostDownloadedMaps(int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMostDownloadedMaps(count).stream()
        .map(MapBean::fromMap)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<MapBean>> getMostPlayedMaps(int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMostPlayedMaps(count).stream()
        .map(MapBean::fromMap)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<MapBean>> getMostLikedMaps(int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedMaps(count).stream()
        .map(MapBean::fromMap)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<MapBean>> getNewestMaps(int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.getNewestMaps(count).stream()
        .map(MapBean::fromMap)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<CoopMission>> getCoopMaps() {
    return CompletableFuture.completedFuture(fafApiAccessor.getCoopMissions().stream()
        .map(CoopMission::fromCoopInfo)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<AvatarBean>> getAvailableAvatars() {
    return CompletableFuture.completedFuture(fafServerAccessor.getAvailableAvatars().stream()
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
    // Cache eviction by annotation
  }

  @Override
  @Async
  public CompletableFuture<List<CoopResult>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers) {
    return CompletableFuture.completedFuture(fafApiAccessor.getCoopLeaderboard(mission.getId(), numberOfPlayers));
  }

  @Override
  @Async
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return CompletableFuture.completedFuture(fafApiAccessor.getGamePlayerStats(playerId, knownFeaturedMod)
        .parallelStream()
        .sorted(Comparator.comparing(GamePlayerStats::getScoreTime))
        .map(entry -> new RatingHistoryDataPoint(entry.getScoreTime(), entry.getAfterMean(), entry.getAfterDeviation()))
        .collect(Collectors.toList())
    );
  }

  @Override
  @Async
  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedMods().stream()
        .sorted(Comparator.comparingInt(com.faforever.client.api.dto.FeaturedMod::getDisplayOrder))
        .map(FeaturedMod::fromFeaturedMod)
        .collect(Collectors.toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedModFiles(featuredMod, version));
  }

  @Override
  @Async
  public CompletableFuture<List<LeaderboardEntry>> getLadder1v1Leaderboard() {
    return CompletableFuture.completedFuture(fafApiAccessor.getLadder1v1Leaderboard().parallelStream()
        .map(LeaderboardEntry::fromLadder1v1)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<LeaderboardEntry>> getGlobalLeaderboard() {
    return CompletableFuture.completedFuture(fafApiAccessor.getGlobalLeaderboard().parallelStream()
        .map(LeaderboardEntry::fromGlobalRating)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<Replay>> getNewestReplays(int topElementCount) {
    return CompletableFuture.completedFuture(fafApiAccessor.getNewestReplays(topElementCount)
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount) {
    return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedReplays(topElementCount)
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()));
  }

  @Override
  @Async
  public CompletableFuture<List<Replay>> getMostWatchedReplays(int topElementCount) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMostWatchedReplays(topElementCount)
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()));
  }

  @Override
  public void uploadMod(Path modFile, ProgressListener byteListener) {
    fafApiAccessor.uploadMod(modFile, byteListener);
  }

  @Override
  @Async
  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(int playerId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayerAchievements(playerId));
  }

  @Override
  @Async
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinitions());
  }

  @Override
  @Async
  public CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinition(achievementId));
  }

  @Override
  @Async
  public CompletableFuture<List<Replay>> findReplaysByQuery(String query) {
    return CompletableFuture.completedFuture(fafApiAccessor.findReplaysByQuery(query)
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()));
  }

  @Override
  public void sendIceMessage(int remotePlayerId, Object message) {
    fafServerAccessor.sendGpgMessage(new IceMessage(remotePlayerId, message));
  }
}
