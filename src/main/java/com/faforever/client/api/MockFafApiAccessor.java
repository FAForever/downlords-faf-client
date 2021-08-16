package com.faforever.client.api;

import com.faforever.client.FafClientApplication;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementType;
import com.faforever.commons.api.dto.Clan;
import com.faforever.commons.api.dto.CoopMission;
import com.faforever.commons.api.dto.CoopResult;
import com.faforever.commons.api.dto.FeaturedModFile;
import com.faforever.commons.api.dto.Game;
import com.faforever.commons.api.dto.GameReview;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
public class MockFafApiAccessor implements FafApiAccessor {

  @Override
  public Flux<PlayerAchievement> getPlayerAchievements(int playerId) {
    return Flux.empty();
  }

  @Override
  public Flux<PlayerEvent> getPlayerEvents(int playerId) {
    return Flux.empty();
  }

  @Override
  public Flux<AchievementDefinition> getAchievementDefinitions() {
    return Flux.empty();
  }

  @Override
  public Mono<AchievementDefinition> getAchievementDefinition(String achievementId) {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setName("Mock achievement");
    achievementDefinition.setDescription("Congratulations! You read this text.");
    achievementDefinition.setType(AchievementType.STANDARD);
    return Mono.just(achievementDefinition);
  }

  @Override
  public void authorize() {
    //do nothing
  }

  @Override
  public Flux<Mod> getMods() {
    return Flux.empty();
  }

  @Override
  public Flux<Leaderboard> getLeaderboards() {
    return Flux.empty();
  }

  @Override
  public Flux<LeaderboardEntry> getAllLeaderboardEntries(String leaderboardTechnicalName) {
    return Flux.empty();
  }

  @Override
  public Mono<Tuple2<List<LeaderboardEntry>, Integer>> getLeaderboardEntriesWithTotalPages(String leaderboardTechnicalName, int count, int page) {
    return Mono.empty();
  }

  @Override
  public Flux<LeaderboardEntry> getLeaderboardEntriesForPlayer(int playerId) {
    return Flux.empty();
  }

  @Override
  public Flux<com.faforever.commons.api.dto.FeaturedMod> getFeaturedMods() {
    com.faforever.commons.api.dto.FeaturedMod featuredMod = new com.faforever.commons.api.dto.FeaturedMod();
    featuredMod.setDisplayName("Forged Alliance Forever");
    featuredMod.setTechnicalName("faf");
    featuredMod.setVisible(true);
    featuredMod.setDescription("Description");

    return Flux.just(featuredMod);
  }

  @Override
  public Flux<LeaderboardRatingJournal> getRatingJournal(int playerId, int leaderboardId) {
    return Flux.empty();
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getMapsByIdWithTotalPages(List<Integer> mapIdList, int count, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getRecommendedMapsWithTotalPages(int count, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getMostPlayedMapsWithTotalPages(int count, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getHighestRatedMapsWithTotalPages(int count, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> getNewestMapsWithTotalPages(int count, int page) {
    return Mono.empty();
  }

  @Override
  public Flux<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
    return Flux.empty();
  }

  @Override
  public Mono<Void> uploadMod(Path file, ByteCountListener listener) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> uploadMap(Path file, boolean isRanked, ByteCountListener listener) {
    return Mono.empty();
  }

  @Override
  public Flux<CoopMission> getCoopMissions() {
    return Flux.empty();
  }

  @Override
  public Mono<ModVersion> getModVersion(String uid) {
    return Mono.empty();
  }

  @Override
  public Flux<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return Flux.empty();
  }

  @Override
  public Mono<Tuple2<List<Game>, Integer>> getNewestReplaysWithTotalPages(int count, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Game>, Integer>> getHighestRatedReplaysWithTotalPages(int count, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Game>, Integer>> findReplaysByQueryWithTotalPages(String query, int maxResults, int page, SortConfig sortConfig) {
    return Mono.empty();
  }

  @Override
  public Mono<MapVersion> findMapByFolderName(String folderName) {
    return Mono.empty();
  }

  @Override
  public Mono<MapVersion> getMapLatestVersion(String mapFolderName) {
    return Mono.empty();
  }

  @Override
  public Flux<Player> getPlayersByIds(Collection<Integer> playerIds) {
    return Flux.empty();
  }

  @Override
  public Mono<Player> queryPlayerByName(String playerName) {
    return Mono.empty();
  }

  @Override
  public Mono<GameReview> createGameReview(GameReview review) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> updateGameReview(GameReview review) {
    return Mono.empty();
  }

  @Override
  public Mono<ModVersionReview> createModVersionReview(ModVersionReview review) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> updateModVersionReview(ModVersionReview review) {
    return Mono.empty();
  }

  @Override
  public Mono<MapVersionReview> createMapVersionReview(MapVersionReview review) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> updateMapVersionReview(MapVersionReview review) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> deleteGameReview(String id) {
    return Mono.empty();
  }

  @Override
  public Flux<TutorialCategory> getTutorialCategories() {
    return Flux.empty();
  }

  @Override
  public Mono<Clan> getClanByTag(String tag) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Map>, Integer>> findMapsByQueryWithTotalPages(SearchConfig searchConfig, int page, int count) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> deleteMapVersionReview(String id) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> deleteModVersionReview(String id) {
    return Mono.empty();
  }

  @Override
  public Mono<Game> findReplayById(int id) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Mod>, Integer>> findModsByQueryWithTotalPages(SearchConfig query, int maxResults, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<Mod>, Integer>> getRecommendedModsWithTotalPages(int count, int page) {
    return Mono.empty();
  }


  @Override
  public Flux<MapPoolAssignment> getMatchmakerPoolMaps(int matchmakerQueueId, float rating) {
    return Flux.empty();
  }

  @Override
  public Mono<MatchmakerQueue> getMatchmakerQueue(String technicalName) {
    return Mono.empty();
  }

  @Override
  public Flux<Tournament> getAllTournaments() {
    return Flux.empty();
  }

  @Override
  public Flux<ModerationReport> getPlayerModerationReports(int playerId) {
    return null;
  }

  @Override
  public Mono<Void> postModerationReport(com.faforever.client.reporting.ModerationReport report) {
    return Mono.empty();
  }

  @Override
  public Mono<Tuple2<List<MapVersion>, Integer>> getOwnedMapsWithTotalPages(int playerId, int loadMoreCount, int page) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> updateMapVersion(String id, MapVersion mapVersion) {
    return Mono.empty();
  }

  @Override
  public Mono<MeResult> getMe() {
    return Mono.empty();
  }

  @Override
  public Flux<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return Flux.empty();
  }
}
