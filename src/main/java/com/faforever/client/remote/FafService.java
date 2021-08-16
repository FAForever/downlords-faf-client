package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.avatar.AvatarBean;
import com.faforever.client.avatar.event.AvatarChangedEvent;
import com.faforever.client.clan.Clan;
import com.faforever.client.coop.CoopMission;
import com.faforever.client.domain.RatingHistoryDataPoint;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardEntry;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.mod.ModVersion;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.replay.Replay;
import com.faforever.client.reporting.ModerationReport;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.tournament.TournamentBean;
import com.faforever.client.tutorial.TutorialCategory;
import com.faforever.client.vault.review.Review;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
import com.faforever.commons.api.dto.MapVersion;
import com.faforever.commons.api.dto.MapVersionReview;
import com.faforever.commons.api.dto.MeResult;
import com.faforever.commons.api.dto.ModVersionReview;
import com.faforever.commons.api.dto.NeroxisGeneratorParams;
import com.faforever.commons.api.dto.PlayerAchievement;
import com.faforever.commons.api.dto.PlayerEvent;
import com.faforever.commons.io.ByteCountListener;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.IceServer;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.ServerMessage;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ReadOnlyObjectProperty;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

@Lazy
@Service
@RequiredArgsConstructor
public class FafService {

  private final FafServerAccessor fafServerAccessor;
  private final FafApiAccessor fafApiAccessor;
  private final EventBus eventBus;

  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.addEventListener(type, listener);
  }

  public CompletableFuture<GameLaunchResponse> requestHostGame(NewGameInfo newGameInfo) {
    return fafServerAccessor.requestHostGame(newGameInfo);
  }

  public ConnectionState getLobbyConnectionState() {
    return fafServerAccessor.getConnectionState();
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  public CompletableFuture<GameLaunchResponse> requestJoinGame(int gameId, String password) {
    return fafServerAccessor.requestJoinGame(gameId, password);
  }

  public CompletableFuture<GameLaunchResponse> startSearchMatchmaker() {
    return fafServerAccessor.startSearchMatchmaker();
  }

  public void requestMatchmakerInfo() {
    fafServerAccessor.requestMatchmakerInfo();
  }

  public void sendGpgMessage(GpgGameOutboundMessage message) {
    fafServerAccessor.sendGpgMessage(message);
  }

  @Async
  public CompletableFuture<LoginSuccessResponse> connectToServer() {
    return fafServerAccessor.connectAndLogIn();
  }

  public void authorizeApi() {
    fafApiAccessor.authorize();
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
    fafServerAccessor.sendGpgMessage(GpgGameOutboundMessage.Companion.gameStateMessage("Ended"));
  }

  public CompletableFuture<List<Leaderboard>> getLeaderboards() {
    return fafApiAccessor.getLeaderboards()
        .map(Leaderboard::fromDto)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<LeaderboardEntry>> getLeaderboardEntriesForPlayer(int playerId) {
    return fafApiAccessor.getLeaderboardEntriesForPlayer(playerId)
        .map(LeaderboardEntry::fromDto)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<LeaderboardEntry>> getAllLeaderboardEntries(String leaderboardTechnicalName) {
    return fafApiAccessor.getAllLeaderboardEntries(leaderboardTechnicalName)
        .map(LeaderboardEntry::fromDto)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<Tuple2<List<LeaderboardEntry>, Integer>> getLeaderboardEntriesWithPageCount(String leaderboardTechnicalName, int count, int page) {
    return fafApiAccessor.getLeaderboardEntriesWithTotalPages(leaderboardTechnicalName, count, page)
        .map(tuple -> tuple.mapT1(entries ->
            entries.stream().map(LeaderboardEntry::fromDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<List<ModVersion>> getMods() {
    return fafApiAccessor.getMods()
        .map(ModVersion::fromModDto)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<Optional<com.faforever.client.mod.ModVersion>> getModVersion(String uid) {
    return fafApiAccessor.getModVersion(uid)
        .map(modVersion -> ModVersion.fromDto(modVersion, null))
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public void reconnect() {
    fafServerAccessor.reconnect();
  }

  public CompletableFuture<Tuple2<List<MapBean>, Integer>> getMostPlayedMapsWithPageCount(int count, int page) {
    return fafApiAccessor.getMostPlayedMapsWithTotalPages(count, page)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream().map(MapBean::fromMapDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Integer> getRecommendedMapPageCount(int count) {
    return fafApiAccessor.getRecommendedMapsWithTotalPages(count, 1).map(Tuple2::getT2).toFuture();
  }

  public CompletableFuture<Tuple2<List<MapBean>, Integer>> getRecommendedMapsWithPageCount(int count, int page) {
    return fafApiAccessor.getRecommendedMapsWithTotalPages(count, page)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream().map(MapBean::fromMapDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Tuple2<List<MapBean>, Integer>> getMapsByIdWithPageCount(List<Integer> mapIdList, int count, int page) {
    return fafApiAccessor.getMapsByIdWithTotalPages(mapIdList, count, page)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream().map(MapBean::fromMapDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Tuple2<List<MapBean>, Integer>> getHighestRatedMapsWithPageCount(int count, int page) {
    return fafApiAccessor.getHighestRatedMapsWithTotalPages(count, page)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream().map(MapBean::fromMapDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Tuple2<List<MapBean>, Integer>> getNewestMapsWithPageCount(int count, int page) {
    return fafApiAccessor.getNewestMapsWithTotalPages(count, page)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream().map(MapBean::fromMapDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<List<CoopMission>> getCoopMaps() {
    return fafApiAccessor.getCoopMissions()
        .map(CoopMission::fromCoopInfo)
        .collectList()
        .toFuture();
  }

  @Async
  public CompletableFuture<List<AvatarBean>> getAvailableAvatars() {
    return fafServerAccessor.getAvailableAvatars().thenApply(avatars ->
        avatars.stream().map(AvatarBean::fromAvatar).collect(toList()));
  }

  public CompletableFuture<MeResult> getCurrentUser() {
    return fafApiAccessor.getMe().toFuture();
  }

  public CompletableFuture<Set<String>> getPermissions() {
    return fafApiAccessor.getMe().toFuture().thenApply(MeResult::getPermissions);
  }

  public void selectAvatar(AvatarBean avatar) {
    fafServerAccessor.selectAvatar(avatar == null ? null : avatar.getUrl());
    eventBus.post(new AvatarChangedEvent(avatar));
  }

  public CompletableFuture<List<CoopResult>> getCoopLeaderboard(CoopMission mission, int numberOfPlayers) {
    return fafApiAccessor.getCoopLeaderboard(mission.getId(), numberOfPlayers).collectList().toFuture();
  }

  public CompletableFuture<List<RatingHistoryDataPoint>> getRatingHistory(int playerId, int leaderboardId) {
    return fafApiAccessor.getRatingJournal(playerId, leaderboardId)
        .filter(gamePlayerStats -> gamePlayerStats.getCreateTime() != null
            && gamePlayerStats.getMeanAfter() != null
            && gamePlayerStats.getDeviationAfter() != null)
        .map(entry -> new RatingHistoryDataPoint(entry.getGamePlayerStats().getScoreTime(), entry.getMeanAfter(), entry.getDeviationAfter()))
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<FeaturedMod>> getFeaturedMods() {
    return fafApiAccessor.getFeaturedMods()
        .sort(Comparator.comparingInt(com.faforever.commons.api.dto.FeaturedMod::getOrder))
        .map(FeaturedMod::fromFeaturedMod)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<FeaturedModFile>> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return fafApiAccessor.getFeaturedModFiles(featuredMod, version).collectList().toFuture();
  }

  public CompletableFuture<Tuple2<List<Replay>, Integer>> getNewestReplaysWithPageCount(int topElementCount, int page) {
    return fafApiAccessor.getNewestReplaysWithTotalPages(topElementCount, page)
        .map(tuple -> tuple.mapT1(replays ->
            replays.stream().map(Replay::fromDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Tuple2<List<Replay>, Integer>> getHighestRatedReplaysWithPageCount(int topElementCount, int page) {
    return fafApiAccessor.getHighestRatedReplaysWithTotalPages(topElementCount, page)
        .map(tuple -> tuple.mapT1(replays ->
            replays.stream().map(Replay::fromDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Void> uploadMod(Path modFile, ByteCountListener byteListener) {
    return fafApiAccessor.uploadMod(modFile, byteListener).toFuture();
  }

  public CompletableFuture<Void> uploadMap(Path mapFile, boolean isRanked, ByteCountListener byteListener) {
    return fafApiAccessor.uploadMap(mapFile, isRanked, byteListener).toFuture();
  }

  public CompletableFuture<Map<String, PlayerEvent>> getPlayerEvents(int playerId) {
    return fafApiAccessor.getPlayerEvents(playerId)
        .collectMap(playerEvent -> playerEvent.getEvent().getId())
        .toFuture();
  }

  public CompletableFuture<List<PlayerAchievement>> getPlayerAchievements(int playerId) {
    return fafApiAccessor.getPlayerAchievements(playerId)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<List<AchievementDefinition>> getAchievementDefinitions() {
    return fafApiAccessor.getAchievementDefinitions()
        .collectList()
        .toFuture();
  }

  public CompletableFuture<AchievementDefinition> getAchievementDefinition(String achievementId) {
    return fafApiAccessor.getAchievementDefinition(achievementId)
        .toFuture();
  }

  public CompletableFuture<Tuple2<List<Replay>, Integer>> findReplaysByQueryWithPageCount(String query, int maxResults, int page, SortConfig sortConfig) {
    return fafApiAccessor.findReplaysByQueryWithTotalPages(query, maxResults, page, sortConfig)
        .map(tuple -> tuple.mapT1(replays ->
            replays.stream().map(Replay::fromDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Tuple2<List<MapBean>, Integer>> findMapsByQueryWithPageCount(SearchConfig query, int count, int page) {
    return fafApiAccessor.findMapsByQueryWithTotalPages(query, count, page)
        .map(tuple -> tuple.mapT1(maps ->
            maps.stream().map(MapBean::fromMapDto).collect(toList())
        ))
        .toFuture();
  }

  public CompletableFuture<Optional<MapBean>> findMapByFolderName(String folderName) {
    return fafApiAccessor.findMapByFolderName(folderName)
        .map(MapBean::fromMapVersionDto)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public CompletableFuture<Optional<MapBean>> getMapLatestVersion(String mapFolderName) {
    return fafApiAccessor.getMapLatestVersion(mapFolderName)
        .map(MapBean::fromMapVersionDto)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public CompletableFuture<List<Player>> getPlayersByIds(Collection<Integer> playerIds) {
    return fafApiAccessor.getPlayersByIds(playerIds)
        .map(Player::fromDto)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<Optional<Player>> queryPlayerByName(String playerName) {
    return fafApiAccessor.queryPlayerByName(playerName)
        .map(Player::fromDto)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public CompletableFuture<Void> saveGameReview(Review review, int gameId) {
    GameReview gameReview = (GameReview) new GameReview()
        .setScore(review.getScore().byteValue())
        .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getPlayer(), "Player ID must be set");
      return fafApiAccessor.createGameReview(
          (GameReview) gameReview
              .setGame(new Game().setId(String.valueOf(gameId)))
              .setPlayer((com.faforever.commons.api.dto.Player) (new com.faforever.commons.api.dto.Player().setId(String.valueOf(review.getPlayer().getId()))))
      ).doOnNext(updatedReview -> review.setId(updatedReview.getId()))
          .toFuture()
          .thenRun(() -> {
          });
    } else {
      return fafApiAccessor.updateGameReview((GameReview) gameReview.setId(review.getId())).toFuture();
    }
  }

  public CompletableFuture<Void> saveModVersionReview(Review review, String modVersionId) {
    ModVersionReview modVersionReview = (ModVersionReview) new ModVersionReview()
        .setScore(review.getScore().byteValue())
        .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getPlayer(), "Player ID must be set");
      return fafApiAccessor.createModVersionReview(
          (ModVersionReview) modVersionReview
              .setModVersion((com.faforever.commons.api.dto.ModVersion) new com.faforever.commons.api.dto.ModVersion().setId(String.valueOf(modVersionId)))
              .setPlayer((com.faforever.commons.api.dto.Player) new com.faforever.commons.api.dto.Player().setId(String.valueOf(review.getPlayer().getId())))
              .setId(String.valueOf(review.getId()))
      ).doOnNext(updatedReview -> review.setId(updatedReview.getId()))
          .toFuture()
          .thenRun(() -> {
          });
    } else {
      return fafApiAccessor.updateModVersionReview((ModVersionReview) modVersionReview.setId(review.getId())).toFuture();
    }
  }

  public CompletableFuture<Void> saveMapVersionReview(Review review, String mapVersionId) {
    MapVersionReview mapVersionReview = (MapVersionReview) new MapVersionReview()
        .setScore(review.getScore().byteValue())
        .setText(review.getText());

    if (review.getId() == null) {
      Assert.notNull(review.getPlayer(), "Player ID must be set");
      return fafApiAccessor.createMapVersionReview(
          (MapVersionReview) mapVersionReview
              .setMapVersion((MapVersion) new MapVersion().setId(mapVersionId))
              .setPlayer((com.faforever.commons.api.dto.Player) new com.faforever.commons.api.dto.Player().setId(String.valueOf(review.getPlayer().getId())))
              .setId(String.valueOf(review.getId()))
      ).doOnNext(updatedReview -> review.setId(updatedReview.getId()))
          .toFuture()
          .thenRun(() -> {
          });
    } else {
      return fafApiAccessor.updateMapVersionReview((MapVersionReview) mapVersionReview.setId(review.getId())).toFuture();
    }
  }

  @Async
  public CompletableFuture<Optional<Replay>> getLastGameOnMap(int playerId, String mapVersionId) {
    return fafApiAccessor.getLastGamesOnMap(playerId, mapVersionId, 1)
        .next()
        .map(Replay::fromDto)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  @Async
  public CompletableFuture<Void> deleteGameReview(Review review) {
    return fafApiAccessor.deleteGameReview(review.getId()).toFuture();
  }

  @Async
  public CompletableFuture<Void> deleteMapVersionReview(Review review) {
    return fafApiAccessor.deleteMapVersionReview(review.getId()).toFuture();
  }

  @Async
  public CompletableFuture<Void> deleteModVersionReview(Review review) {
    return fafApiAccessor.deleteModVersionReview(review.getId()).toFuture();
  }

  public CompletableFuture<Optional<Replay>> findReplayById(int id) {
    return fafApiAccessor.findReplayById(id)
        .map(Replay::fromDto)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public CompletableFuture<List<IceServer>> getIceServers() {
    return fafServerAccessor.getIceServers();
  }

  public void restoreGameSession(int id) {
    fafServerAccessor.restoreGameSession(id);
  }

  @Async
  public CompletableFuture<Tuple2<List<ModVersion>, Integer>> findModsByQueryWithPageCount(SearchConfig query, int count, int page) {
    return fafApiAccessor.findModsByQueryWithTotalPages(query, count, page)
        .map(tuple -> tuple.mapT1(mods ->
            mods.stream().map(ModVersion::fromModDto).collect(toList())
        ))
        .toFuture();
  }

  @Async
  public CompletableFuture<Integer> getRecommendedModPageCount(int count) {
    return fafApiAccessor.getRecommendedModsWithTotalPages(count, 1).map(Tuple2::getT2).toFuture();
  }

  @Async
  public CompletableFuture<Tuple2<List<ModVersion>, Integer>> getRecommendedModsWithPageCount(int count, int page) {
    return fafApiAccessor.getRecommendedModsWithTotalPages(count, page)
        .map(tuple -> tuple.mapT1(mods ->
            mods.stream().map(ModVersion::fromModDto).collect(toList())
        ))
        .toFuture();
  }

  @Async
  public CompletableFuture<Tuple2<List<MapBean>, Integer>> getMatchmakerMapsWithPageCount(int matchmakerQueueId, float rating, int count, int page) {
    Flux<MapBean> matchmakerMapsFlux = fafApiAccessor.getMatchmakerPoolMaps(matchmakerQueueId, rating)
        .flatMap(mapPoolAssignment -> {
          if (mapPoolAssignment.getMapVersion() != null) {
            return Mono.just(MapBean.fromMapVersionDto(mapPoolAssignment.getMapVersion()));
          } else if (mapPoolAssignment.getMapParams() instanceof NeroxisGeneratorParams) {
            return Mono.just(MapBean.fromNeroxisGeneratedMapParams((NeroxisGeneratorParams) mapPoolAssignment.getMapParams()));
          } else {
            return Mono.empty();
          }
        })
        .distinct()
        .sort(Comparator.comparing(MapBean::getSize).thenComparing(MapBean::getDisplayName, String.CASE_INSENSITIVE_ORDER))
        .cache();
    return paginateResult(count, page, matchmakerMapsFlux).toFuture();
  }

  @NotNull
  private <T> Mono<Tuple2<List<T>, Integer>> paginateResult(int count, int page, Flux<T> resultsFlux) {
    return Mono.zip(
        resultsFlux.skip((long) (page - 1) * count)
            .takeLast(count).collectList(),
        resultsFlux.count().map(size -> (size - 1) / count + 1)
            .cast(Integer.class)
    );
  }

  @Async
  public CompletableFuture<Optional<MatchmakingQueue>> getMatchmakingQueue(String technicalName) {
    return fafApiAccessor.getMatchmakerQueue(technicalName)
        .map(MatchmakingQueue::fromDto)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public void acceptPartyInvite(Player player) {
    fafServerAccessor.acceptPartyInvite(player);
  }

  public void inviteToParty(Player player) {
    fafServerAccessor.inviteToParty(player);
  }

  public void kickPlayerFromParty(Player player) {
    fafServerAccessor.kickPlayerFromParty(player);
  }

  public void leaveParty() {
    fafServerAccessor.leaveParty();
  }

  public void readyParty() {
    fafServerAccessor.readyParty();
  }

  public void unreadyParty() {
    fafServerAccessor.unreadyParty();
  }

  public void setPartyFactions(List<Faction> factions) {
    fafServerAccessor.setPartyFactions(factions);
  }

  public void updateMatchmakerState(MatchmakingQueue queue, MatchmakerState state) {
    fafServerAccessor.gameMatchmaking(queue, state);
  }

  @Async
  public CompletableFuture<Optional<Clan>> getClanByTag(String tag) {
    return fafApiAccessor.getClanByTag(tag)
        .map(Clan::fromDto)
        .toFuture()
        .thenApply(Optional::ofNullable);
  }

  public void sendIceMessage(int remotePlayerId, Object message) {
    fafServerAccessor.sendGpgMessage(GpgGameOutboundMessage.Companion.iceMessage(remotePlayerId, message));
  }

  @Async
  public CompletableFuture<List<TournamentBean>> getAllTournaments() {
    return fafApiAccessor.getAllTournaments()
        .map(TournamentBean::fromTournamentDto)
        .collectList()
        .toFuture();
  }

  @Async
  public CompletableFuture<List<ModerationReport>> getAllModerationReports(int playerId) {
    return fafApiAccessor.getPlayerModerationReports(playerId)
        .map(ModerationReport::fromReportDto)
        .collectList()
        .toFuture();
  }

  public CompletableFuture<ModerationReport> postModerationReport(ModerationReport report) {
    return fafApiAccessor.postModerationReport(report).map(ModerationReport::fromReportDto).toFuture();
  }

  public CompletableFuture<Tuple2<List<MapBean>, Integer>> getOwnedMapsWithPageCount(int playerId, int loadMoreCount, int page) {
    return fafApiAccessor.getOwnedMapsWithTotalPages(playerId, loadMoreCount, page)
        .map(tuple -> tuple.mapT1(mapVersions -> mapVersions.stream().map(MapBean::fromMapVersionDto).collect(Collectors.toList())))
        .toFuture();
  }

  @Async
  public CompletableFuture<Void> hideMapVersion(MapBean map) {
    String id = map.getId();
    MapVersion mapVersion = new MapVersion();
    mapVersion.setHidden(true);
    mapVersion.setId(map.getId());
    return fafApiAccessor.updateMapVersion(id, mapVersion).toFuture();
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

  public CompletableFuture<List<TutorialCategory>> getTutorialCategories() {
    return fafApiAccessor.getTutorialCategories()
        .map(TutorialCategory::fromDto)
        .collectList()
        .toFuture();
  }
}
