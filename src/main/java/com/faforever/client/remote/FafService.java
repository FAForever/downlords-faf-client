package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.faforever.client.clan.Clan;
import com.faforever.client.config.CacheNames;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.GameEndedMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.IceMessage;
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.replay.Replay;
import com.faforever.client.tournament.TournamentBean;
import com.faforever.client.tutorial.TutorialCategory;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.io.ByteCountListener;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Lazy
@Service
@RequiredArgsConstructor
public class FafService {

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final EventBus eventBus;

  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.addOnMessageListener(type, listener);
  }

  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.removeOnMessageListener(type, listener);
  }

  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return fafServerAccessor.requestHostGame(newGameInfo);
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return fafServerAccessor.requestJoinGame(gameId, password);
  }

  public CompletableFuture<GameLaunchMessage> startSearchLadder1v1(Faction faction) {
    return fafServerAccessor.startSearchLadder1v1(faction);
  }

  public void requestMatchmakerInfo() {
    fafServerAccessor.requestMatchmakerInfo();
  }

  public void stopSearchingRanked() {
    fafServerAccessor.stopSearchingRanked();
  }

  public void sendGpgGameMessage(GpgGameMessage message) {
    fafServerAccessor.sendGpgMessage(message);
  }

  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    return fafServerAccessor.connectAndLogIn(username, password);
  }

  public void disconnect() {
    fafServerAccessor.disconnect();
  }

  public void addFriend(Player player) {
    fafServerAccessor.addFriend(player.getId());
  }

  public void addFoe(Player player) {
    fafServerAccessor.addFoe(player.getId());
  }

  public void removeFriend(Player player) {
    fafServerAccessor.removeFriend(player.getId());
  }

  public void removeFoe(Player player) {
    fafServerAccessor.removeFoe(player.getId());
  }

  @Async
  public void notifyGameEnded() {
    fafServerAccessor.sendGpgMessage(new GameEndedMessage());
  }

  @Async
  public CompletableFuture<LeaderboardEntry> getLadder1v1EntryForPlayer(int playerId) {
    return CompletableFuture.completedFuture(LeaderboardEntry.fromLadder1v1(fafApiAccessor.getLadder1v1EntryForPlayer(playerId)));
  }

  @Async
  public CompletableFuture<List<ModVersion>> getMods() {
    return CompletableFuture.completedFuture(fafApiAccessor.getMods().stream()
        .map(ModVersion::fromModDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<com.faforever.client.mod.ModVersion> getModVersion(String uid) {
    return CompletableFuture.completedFuture(com.faforever.client.mod.ModVersion.fromDto(fafApiAccessor.getModVersion(uid), null));
  }

  public void reconnect() {
    fafServerAccessor.reconnect();
  }

  @Async
  public CompletableFuture<List<MapBean>> getMostPlayedMaps(int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMostPlayedMaps(count, page).stream()
        .map(MapBean::fromMapDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<MapBean>> getMapsById(List<Integer> mapIdList, int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMapsById(mapIdList, count, page).stream()
        .map(MapBean::fromMapDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<MapBean>> getHighestRatedMaps(int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedMaps(count, page).stream()
        .map(MapBean::fromMapDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<MapBean>> getNewestMaps(int count, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getNewestMaps(count, page).stream()
        .map(MapBean::fromMapDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<CoopMission>> getCoopMaps() {
    return CompletableFuture.completedFuture(fafApiAccessor.getCoopMissions().stream()
        .map(CoopMission::fromCoopInfo)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<AvatarBean>> getAvailableAvatars() {
    return CompletableFuture.completedFuture(fafServerAccessor.getAvailableAvatars().stream()
        .map(AvatarBean::fromAvatar)
        .collect(Collectors.toList()));
  }

  public void selectAvatar(AvatarBean avatar) {
    fafServerAccessor.selectAvatar(avatar == null ? null : avatar.getUrl());
    eventBus.post(new AvatarChangedEvent(avatar));
  }

  @CacheEvict(CacheNames.MODS)
  public void evictModsCache() {
    // Cache eviction by annotation
  }

  @Async
  public CompletableFuture<List<CoopResult>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers) {
    return CompletableFuture.completedFuture(fafApiAccessor.getCoopLeaderboard(mission.getId(), numberOfPlayers));
  }

  @Async
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return CompletableFuture.completedFuture(fafApiAccessor.getGamePlayerStats(playerId, knownFeaturedMod)
        .parallelStream()
        .filter(gamePlayerStats -> gamePlayerStats.getScoreTime() != null
            && gamePlayerStats.getAfterMean() != null
            && gamePlayerStats.getAfterDeviation() != null)
        .sorted(Comparator.comparing(GamePlayerStats::getScoreTime))
        .map(entry -> new RatingHistoryDataPoint(entry.getScoreTime(), entry.getAfterMean(), entry.getAfterDeviation()))
        .collect(Collectors.toList())
    );
  }

  @Async
  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedMods().stream()
        .sorted(Comparator.comparingInt(com.faforever.client.api.dto.FeaturedMod::getOrder))
        .map(FeaturedMod::fromFeaturedMod)
        .collect(Collectors.toList()));
  }

  @Async
  public CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedModFiles(featuredMod, version));
  }

  @Async
  public CompletableFuture<List<LeaderboardEntry>> getLadder1v1Leaderboard() {
    return CompletableFuture.completedFuture(fafApiAccessor.getLadder1v1Leaderboard().parallelStream()
        .map(LeaderboardEntry::fromLadder1v1)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<LeaderboardEntry>> getGlobalLeaderboard() {
    return CompletableFuture.completedFuture(fafApiAccessor.getGlobalLeaderboard().parallelStream()
        .map(LeaderboardEntry::fromGlobalRating)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<Replay>> getNewestReplays(int topElementCount, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getNewestReplays(topElementCount, page)
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<Replay>> getHighestRatedReplays(int topElementCount, int page) {
    return CompletableFuture.completedFuture(fafApiAccessor.getHighestRatedReplays(topElementCount, page)
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()));
  }

  public void uploadMod(Path modFile, ByteCountListener byteListener) {
    fafApiAccessor.uploadMod(modFile, byteListener);
  }

  @Async
  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(int playerId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayerAchievements(playerId));
  }

  @Async
  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinitions());
  }

  @Async
  public CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getAchievementDefinition(achievementId));
  }

  @Async
  public CompletableFuture<List<Replay>> findReplaysByQuery(String query, int maxResults, int page, SortConfig sortConfig) {
    return CompletableFuture.completedFuture(fafApiAccessor.findReplaysByQuery(query, maxResults, page, sortConfig)
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<MapBean>> findMapsByQuery(SearchConfig query, int page, int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.findMapsByQuery(query, page, count)
        .parallelStream()
        .map(MapBean::fromMapDto)
        .collect(toList()));
  }

  public CompletableFuture<Optional<MapBean>> findMapByFolderName(String folderName) {
    return CompletableFuture.completedFuture(fafApiAccessor.findMapByFolderName(folderName)
        .map(MapBean::fromMapVersionDto));
  }

  public CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerIds) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayersByIds(playerIds).stream()
        .map(Player::fromDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<Void> saveGameReview(Review review, int gameId) {
    GameReview gameReview = (GameReview) new GameReview()
        .setScore(review.getScore().byteValue())
        .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getPlayer(), "Player ID must be set");
      GameReview updatedReview = fafApiAccessor.createGameReview(
          (GameReview) gameReview
              .setGame(new Game().setId(String.valueOf(gameId)))
              .setPlayer(new com.faforever.client.api.dto.Player().setId(String.valueOf(review.getPlayer().getId())))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateGameReview((GameReview) gameReview.setId(String.valueOf(review.getId())));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> saveModVersionReview(Review review, String modVersionId) {
    ModVersionReview modVersionReview = (ModVersionReview) new ModVersionReview()
        .setScore(review.getScore().byteValue())
        .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getPlayer(), "Player ID must be set");
      ModVersionReview updatedReview = fafApiAccessor.createModVersionReview(
          (ModVersionReview) modVersionReview
              .setModVersion(new com.faforever.client.api.dto.ModVersion().setId(String.valueOf(modVersionId)))
              .setId(String.valueOf(review.getId()))
              .setPlayer(new com.faforever.client.api.dto.Player().setId(String.valueOf(review.getPlayer().getId())))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateModVersionReview((ModVersionReview) modVersionReview.setId(String.valueOf(review.getId())));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> saveMapVersionReview(Review review, String mapVersionId) {
    MapVersionReview mapVersionReview = (MapVersionReview) new MapVersionReview()
        .setScore(review.getScore().byteValue())
        .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getPlayer(), "Player ID must be set");
      MapVersionReview updatedReview = fafApiAccessor.createMapVersionReview(
          (MapVersionReview) mapVersionReview
              .setMapVersion(new MapVersion().setId(mapVersionId))
              .setId(String.valueOf(review.getId()))
              .setPlayer(new com.faforever.client.api.dto.Player().setId(String.valueOf(review.getPlayer().getId())))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateMapVersionReview((MapVersionReview) mapVersionReview.setId(String.valueOf(review.getId())));
    }

    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Optional<Replay>> getLastGameOnMap(int playerId, String mapVersionId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getLastGamesOnMap(playerId, mapVersionId, 1).stream()
        .map(Replay::fromDto)
        .findFirst());
  }

  @Async
  public CompletableFuture<Void> deleteGameReview(Review review) {
    fafApiAccessor.deleteGameReview(review.getId());
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> deleteMapVersionReview(Review review) {
    fafApiAccessor.deleteMapVersionReview(review.getId());
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> deleteModVersionReview(Review review) {
    fafApiAccessor.deleteModVersionReview(review.getId());
    return CompletableFuture.completedFuture(null);
  }

  public CompletableFuture<Optional<Replay>> findReplayById(int id) {
    return CompletableFuture.completedFuture(fafApiAccessor.findReplayById(id)
        .map(Replay::fromDto));
  }

  public CompletableFuture<List<IceServer>> getIceServers() {
    return fafServerAccessor.getIceServers();
  }

  public void restoreGameSession(int id) {
    fafServerAccessor.restoreGameSession(id);
  }

  @Async
  public CompletableFuture<List<ModVersion>> findModsByQuery(SearchConfig query, int page, int count) {
    return CompletableFuture.completedFuture(fafApiAccessor.findModsByQuery(query, page, count)
        .parallelStream()
        .map(ModVersion::fromModDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<MapBean>> getLadder1v1Maps(int count, int page) {
    List<MapBean> maps = fafApiAccessor.getLadder1v1Maps(count, page).stream()
        .map(ladder1v1Map -> MapBean.fromMapVersionDto(ladder1v1Map.getMapVersion()))
        .collect(toList());
    return CompletableFuture.completedFuture(maps);
  }

  @Async
  public CompletableFuture<Optional<Clan>> getClanByTag(String tag) {
    return CompletableFuture.completedFuture(fafApiAccessor.getClanByTag(tag)
        .map(Clan::fromDto));
  }

  public Optional<MapBean> findMapById(String id) {
    return fafApiAccessor.findMapVersionById(id)
        .map(MapBean::fromMapVersionDto);
  }

  public void sendIceMessage(int remotePlayerId, Object message) {
    fafServerAccessor.sendGpgMessage(new IceMessage(remotePlayerId, message));
  }

  @Async
  public CompletableFuture<List<TournamentBean>> getAllTournaments() {
    return CompletableFuture.completedFuture(fafApiAccessor.getAllTournaments()
        .stream()
        .map(TournamentBean::fromTournamentDto)
        .collect(toList()));
  }


  @Async
  public CompletableFuture<List<MapBean>> getOwnedMaps(int playerId, int loadMoreCount, int page) {
    List<MapVersion> maps = fafApiAccessor.getOwnedMaps(playerId, loadMoreCount, page);
    return CompletableFuture.completedFuture(maps.stream().map(MapBean::fromMapVersionDto).collect(toList()));
  }

  @Async
  public CompletableFuture<Void> hideMapVersion(MapBean map) {
    String id = map.getId();
    MapVersion mapVersion = new MapVersion();
    mapVersion.setHidden(true);
    mapVersion.setId(map.getId());
    fafApiAccessor.updateMapVersion(id, mapVersion);
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Void> unrankeMapVersion(MapBean map) {
    String id = map.getId();
    MapVersion mapVersion = new MapVersion();
    mapVersion.setRanked(false);
    mapVersion.setId(map.getId());
    fafApiAccessor.updateMapVersion(id, mapVersion);
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {
    fafServerAccessor.banPlayer(playerId, duration, periodType, reason);
  }

  @Async
  public void closePlayersGame(int playerId) {
    fafServerAccessor.closePlayersGame(playerId);
  }

  @Async
  public void closePlayersLobby(int playerId) {
    fafServerAccessor.closePlayersLobby(playerId);
  }

  @Async
  public void broadcastMessage(String message) {
    fafServerAccessor.broadcastMessage(message);
  }

  @Async
  public CompletableFuture<List<TutorialCategory>> getTutorialCategories() {
    return CompletableFuture.completedFuture(fafApiAccessor.getTutorialCategories().stream()
        .map(TutorialCategory::fromDto)
        .collect(Collectors.toList())
    );
  }
}
