package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.chat.avatar.AvatarBean;
import com.faforever.client.chat.avatar.event.AvatarChangedEvent;
import com.faforever.client.clan.Clan;
import com.faforever.client.config.CacheNames;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.Leaderboard;
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
import com.faforever.client.reporting.ModerationReport;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.tournament.TournamentBean;
import com.faforever.client.tutorial.TutorialCategory;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.Mod;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.NeroxisGeneratorParams;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.io.ByteCountListener;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

  public CompletableFuture<GameLaunchMessage> startSearchMatchmaker() {
    return fafServerAccessor.startSearchMatchmaker();
  }

  public void stopSearchMatchmaker() {
    fafServerAccessor.stopSearchMatchmaker();
  }

  public void requestMatchmakerInfo() {
    fafServerAccessor.requestMatchmakerInfo();
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
  public CompletableFuture<List<Leaderboard>> getLeaderboards() {
    return CompletableFuture.completedFuture(fafApiAccessor.getLeaderboards().stream()
        .map(Leaderboard::fromDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<LeaderboardEntry>> getLeaderboardEntriesForPlayer(int playerId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getLeaderboardEntriesForPlayer(playerId)
        .stream()
        .map(LeaderboardEntry::fromDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<List<LeaderboardEntry>> getAllLeaderboardEntries(String leaderboardTechnicalName) {
    return CompletableFuture.completedFuture(fafApiAccessor.getAllLeaderboardEntries(leaderboardTechnicalName)
        .parallelStream()
        .map(LeaderboardEntry::fromDto)
        .collect(toList()));
  }

  @Async
  public CompletableFuture<Tuple<List<LeaderboardEntry>, Integer>> getLeaderboardEntriesWithPageCount(String leaderboardTechnicalName, int count, int page) {
    Tuple<List<com.faforever.commons.api.dto.LeaderboardEntry>, java.util.Map<String, ?>> tuple = fafApiAccessor.getLeaderboardEntriesWithMeta(leaderboardTechnicalName, count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(LeaderboardEntry::fromDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
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
  public CompletableFuture<Tuple<List<MapBean>, Integer>> getMostPlayedMapsWithPageCount(int count, int page) {
    Tuple<List<Map>, java.util.Map<String, ?>> tuple = fafApiAccessor.getMostPlayedMapsWithMeta(count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(MapBean::fromMapDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Integer> getRecommendedMapCount() {
    int numMaps = fafApiAccessor.getRecommendedMapCount();
    return CompletableFuture.completedFuture(numMaps);
  }

  @Async
  public CompletableFuture<Tuple<List<MapBean>, Integer>> getRecommendedMapsWithPageCount(int count, int page) {
    Tuple<List<Map>, java.util.Map<String, ?>> tuple = fafApiAccessor.getRecommendedMapsWithMeta(count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(MapBean::fromMapDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Tuple<List<MapBean>, Integer>> getMapsByIdWithPageCount(List<Integer> mapIdList, int count, int page) {
    Tuple<List<Map>, java.util.Map<String, ?>> tuple = fafApiAccessor.getMapsByIdWithMeta(mapIdList, count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(MapBean::fromMapDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Tuple<List<MapBean>, Integer>> getHighestRatedMapsWithPageCount(int count, int page) {
    Tuple<List<Map>, java.util.Map<String, ?>> tuple = fafApiAccessor.getHighestRatedMapsWithMeta(count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(MapBean::fromMapDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Tuple<List<MapBean>, Integer>> getNewestMapsWithPageCount(int count, int page) {
    Tuple<List<Map>, java.util.Map<String, ?>> tuple = fafApiAccessor.getNewestMapsWithMeta(count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(MapBean::fromMapDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
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

  @Async
  public CompletableFuture<Set<String>> getPermissions() {
    return CompletableFuture.completedFuture(fafApiAccessor.getOwnPlayer().getPermissions());
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
  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, int leaderboardId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getRatingJournal(playerId, leaderboardId)
        .parallelStream()
        .filter(gamePlayerStats -> gamePlayerStats.getCreateTime() != null
            && gamePlayerStats.getMeanAfter() != null
            && gamePlayerStats.getDeviationAfter() != null)
        .map(entry -> new RatingHistoryDataPoint(entry.getGamePlayerStats().getScoreTime(), entry.getMeanAfter(), entry.getDeviationAfter()))
        .collect(Collectors.toList()));
  }

  @Async
  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedMods().stream()
        .sorted(Comparator.comparingInt(com.faforever.commons.api.dto.FeaturedMod::getOrder))
        .map(FeaturedMod::fromFeaturedMod)
        .collect(Collectors.toList()));
  }

  @Async
  public CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return CompletableFuture.completedFuture(fafApiAccessor.getFeaturedModFiles(featuredMod, version));
  }

  @Async
  public CompletableFuture<Tuple<List<Replay>, Integer>> getNewestReplaysWithPageCount(int topElementCount, int page) {
    Tuple<List<Game>, java.util.Map<String, ?>> tuple = fafApiAccessor.getNewestReplaysWithMeta(topElementCount, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Tuple<List<Replay>, Integer>> getHighestRatedReplaysWithPageCount(int topElementCount, int page) {
    Tuple<List<Game>, java.util.Map<String, ?>> tuple = fafApiAccessor.getHighestRatedReplaysWithMeta(topElementCount, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
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
  public CompletableFuture<Tuple<List<Replay>, Integer>> findReplaysByQueryWithPageCount(String query, int maxResults, int page, SortConfig sortConfig) {
    Tuple<List<Game>, java.util.Map<String, ?>> tuple = fafApiAccessor.findReplaysByQueryWithMeta(query, maxResults, page, sortConfig);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(Replay::fromDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Tuple<List<MapBean>, Integer>> findMapsByQueryWithPageCount(SearchConfig query, int count, int page) {
    Tuple<List<Map>, java.util.Map<String, ?>> tuple = fafApiAccessor.findMapsByQueryWithMeta(query, count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(MapBean::fromMapDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  public CompletableFuture<Optional<MapBean>> findMapByFolderName(String folderName) {
    return CompletableFuture.completedFuture(fafApiAccessor.findMapByFolderName(folderName)
        .map(MapBean::fromMapVersionDto));
  }

  @Async
  public CompletableFuture<Optional<MapBean>> getMapLatestVersion(String mapFolderName) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMapLatestVersion(mapFolderName)
        .map(MapBean::fromMapVersionDto));
  }

  public CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerIds) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayersByIds(playerIds).stream()
        .map(Player::fromDto)
        .collect(toList()));
  }

  public CompletableFuture<Optional<Player>> queryPlayerByName(String playerName) {
    return CompletableFuture.completedFuture(fafApiAccessor.queryPlayerByName(playerName).map(Player::fromDto));
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
              .setPlayer((com.faforever.commons.api.dto.Player) (new com.faforever.commons.api.dto.Player().setId(String.valueOf(review.getPlayer().getId()))))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateGameReview((GameReview) gameReview.setId(review.getId()));
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
              .setModVersion((com.faforever.commons.api.dto.ModVersion) new com.faforever.commons.api.dto.ModVersion().setId(String.valueOf(modVersionId)))
              .setPlayer((com.faforever.commons.api.dto.Player) new com.faforever.commons.api.dto.Player().setId(String.valueOf(review.getPlayer().getId())))
              .setId(String.valueOf(review.getId()))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateModVersionReview((ModVersionReview) modVersionReview.setId(review.getId()));
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
              .setMapVersion((MapVersion) new MapVersion().setId(mapVersionId))
              .setPlayer((com.faforever.commons.api.dto.Player) new com.faforever.commons.api.dto.Player().setId(String.valueOf(review.getPlayer().getId())))
              .setId(String.valueOf(review.getId()))
      );
      review.setId(updatedReview.getId());
    } else {
      fafApiAccessor.updateMapVersionReview((MapVersionReview) mapVersionReview.setId(review.getId()));
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
  public CompletableFuture<Tuple<List<ModVersion>, Integer>> findModsByQueryWithPageCount(SearchConfig query, int count, int page) {
    Tuple<List<Mod>, java.util.Map<String, ?>> tuple = fafApiAccessor.findModsByQueryWithMeta(query, count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(ModVersion::fromModDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Integer> getRecommendedModCount() {
    int numMods = fafApiAccessor.getRecommendedModCount();
    return CompletableFuture.completedFuture(numMods);
  }

  @Async
  public CompletableFuture<Tuple<List<ModVersion>, Integer>> getRecommendedModsWithPageCount(int count, int page) {
    Tuple<List<Mod>, java.util.Map<String, ?>> tuple = fafApiAccessor.getRecommendedModsWithMeta(count, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(ModVersion::fromModDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
  }

  @Async
  public CompletableFuture<Tuple<List<MapBean>, Integer>> getMatchmakerMapsWithPageCount(int matchmakerQueueId, float rating, int count, int page) {
    List<MapPoolAssignment> poolAssignments = fafApiAccessor.getMatchmakerPoolMaps(matchmakerQueueId, rating);
    List<MapBean> mapVersions = poolAssignments.stream()
        .map(MapPoolAssignment::getMapVersion)
        .distinct()
        .filter(Objects::nonNull)
        .map(MapBean::fromMapVersionDto)
        .collect(Collectors.toList());
    mapVersions.addAll(poolAssignments.stream()
        .map(MapPoolAssignment::getMapParams)
        .filter(mapParams -> mapParams instanceof NeroxisGeneratorParams)
        .distinct()
        .map(mapParams -> MapBean.fromNeroxisGeneratedMapParams((NeroxisGeneratorParams) mapParams))
        .collect(Collectors.toList()));
    mapVersions.sort(Comparator.comparing(MapBean::getSize).thenComparing(MapBean::getDisplayName, String.CASE_INSENSITIVE_ORDER));
    return paginateResult(count, page, mapVersions);
  }

  @NotNull
  private <T> CompletableFuture<Tuple<List<T>, Integer>> paginateResult(int count, int page, List<T> results) {
    int totalPages = (results.size() - 1) / count + 1;
    return CompletableFuture.completedFuture(new Tuple<>(results
        .stream()
        .skip((long) (page - 1) * count)
        .limit(count)
        .collect(toList()),
        totalPages));
  }

  @Async
  public CompletableFuture<Optional<MatchmakingQueue>> getMatchmakingQueue(String technicalName) {
    return CompletableFuture.completedFuture(fafApiAccessor.getMatchmakerQueue(technicalName)
        .map(MatchmakingQueue::fromDto));
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
  public CompletableFuture<List<ModerationReport>> getAllModerationReports(int playerId) {
    return CompletableFuture.completedFuture(fafApiAccessor.getPlayerModerationReports(playerId)
        .stream()
        .map(ModerationReport::fromReportDto)
        .collect(toList()));
  }

  public CompletableFuture<Void> postModerationReport(ModerationReport report) {
    fafApiAccessor.postModerationReport(report);
    return CompletableFuture.completedFuture(null);
  }

  @Async
  public CompletableFuture<Tuple<List<MapBean>, Integer>> getOwnedMapsWithPageCount(int playerId, int loadMoreCount, int page) {
    Tuple<List<MapVersion>, java.util.Map<String, ?>> tuple = fafApiAccessor.getOwnedMapsWithMeta(playerId, loadMoreCount, page);
    return CompletableFuture.completedFuture(new Tuple<>(tuple.getFirst()
        .parallelStream()
        .map(MapBean::fromMapVersionDto)
        .collect(toList()),
        ((HashMap<String, Integer>) tuple.getSecond().get("page")).get("totalPages")));
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
  public CompletableFuture<Void> unRankMapVersion(MapBean map) {
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
