package com.faforever.client.api;

import com.faforever.client.FafClientApplication;
import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.AchievementType;
import com.faforever.client.api.dto.Clan;
import com.faforever.client.api.dto.CoopMission;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GameReview;
import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.MatchmakerQueue;
import com.faforever.client.api.dto.MatchmakerQueueMapPool;
import com.faforever.client.api.dto.MeResult;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.ModReviewsSummary;
import com.faforever.client.api.dto.ModVersion;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.Player;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.api.dto.Tournament;
import com.faforever.client.api.dto.TutorialCategory;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.FeaturedMod;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController.SearchConfig;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.commons.io.ByteCountListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
// NOSONAR
public class MockFafApiAccessor implements FafApiAccessor {

  @Override
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    return List.of();
  }

  @Override
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    return null;
  }

  @Override
  public List<AchievementDefinition> getAchievementDefinitions() {
    return List.of();
  }

  @Override
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    AchievementDefinition achievementDefinition = new AchievementDefinition();
    achievementDefinition.setName("Mock achievement");
    achievementDefinition.setDescription("Congratulations! You read this text.");
    achievementDefinition.setType(AchievementType.STANDARD);
    return achievementDefinition;
  }

  @Override
  public void authorize(int playerId, String username, String password) {

  }

  @Override
  public List<Mod> getMods() {
    Player uploader = new Player();
    return Arrays.asList(
        new com.faforever.client.api.dto.Mod("1", "Mod Number One", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary()),
        new com.faforever.client.api.dto.Mod("2", "Mod Number Two", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary()),
        new com.faforever.client.api.dto.Mod("3", "Mod Number Three", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary()),
        new com.faforever.client.api.dto.Mod("4", "Mod Number Four", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary()),
        new com.faforever.client.api.dto.Mod("5", "Mod Number Five", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary()),
        new com.faforever.client.api.dto.Mod("6", "Mod Number Six", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary()),
        new com.faforever.client.api.dto.Mod("7", "Mod Number Seven", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary()),
        new com.faforever.client.api.dto.Mod("8", "Mod Number Eight", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, List.of(), mod(), modReviewsSummary())
    );
  }

  private ModVersion mod() {
    return new ModVersion();
  }

  private ModReviewsSummary modReviewsSummary() {
    return new ModReviewsSummary();
  }

  @Override
  public List<com.faforever.client.api.dto.FeaturedMod> getFeaturedMods() {
    com.faforever.client.api.dto.FeaturedMod featuredMod = new com.faforever.client.api.dto.FeaturedMod();
    featuredMod.setDisplayName("Forged Alliance Forever");
    featuredMod.setTechnicalName("faf");
    featuredMod.setVisible(true);
    featuredMod.setDescription("Description");

    return Collections.singletonList(featuredMod);
  }

  @Override
  public List<Ladder1v1LeaderboardEntry> getLadder1v1Leaderboard() {
    return List.of();
  }

  @Override
  public List<GlobalLeaderboardEntry> getGlobalLeaderboard() {
    return List.of();
  }

  @Override
  public Ladder1v1LeaderboardEntry getLadder1v1EntryForPlayer(int playerId) {
    return null;
  }

  @Override
  public List<GamePlayerStats> getGamePlayerStats(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return List.of();
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getMapsByIdWithMeta(List<Integer> mapIdList, int count, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getMostPlayedMapsWithMeta(int count, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getHighestRatedMapsWithMeta(int count, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> getNewestMapsWithMeta(int count, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
    return List.of();
  }

  @Override
  public void uploadMod(Path file, ByteCountListener listener) {

  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ByteCountListener listener) {

  }

  @Override
  public List<CoopMission> getCoopMissions() {
    return List.of();
  }

  @Override
  public ModVersion getModVersion(String uid) {
    return null;
  }

  @Override
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return List.of();
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> getNewestReplaysWithMeta(int count, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> getHighestRatedReplaysWithMeta(int count, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public Tuple<List<Game>, java.util.Map<String, ?>> findReplaysByQueryWithMeta(String query, int maxResults, int page, SortConfig sortConfig) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public Optional<MapVersion> findMapByFolderName(String folderName) {
    return Optional.empty();
  }

  @Override
  public List<com.faforever.client.api.dto.Player> getPlayersByIds(Collection<Integer> playerIds) {
    return List.of();
  }

  @Override
  public GameReview createGameReview(GameReview review) {
    return null;
  }

  @Override
  public void updateGameReview(GameReview review) {

  }

  @Override
  public ModVersionReview createModVersionReview(ModVersionReview review) {
    return null;
  }

  @Override
  public void updateModVersionReview(ModVersionReview review) {

  }

  @Override
  public MapVersionReview createMapVersionReview(MapVersionReview review) {
    return null;
  }

  @Override
  public void updateMapVersionReview(MapVersionReview review) {

  }

  @Override
  public void deleteGameReview(String id) {

  }

  @Override
  public List<TutorialCategory> getTutorialCategories() {
    return List.of();
  }

  @Override
  public Optional<Clan> getClanByTag(String tag) {
    return Optional.empty();
  }

  @Override
  public Tuple<List<Map>, java.util.Map<String, ?>> findMapsByQueryWithMeta(SearchConfig searchConfig, int page, int count) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public Optional<MapVersion> findMapVersionById(String id) {
    return Optional.empty();
  }

  @Override
  public void deleteMapVersionReview(String id) {

  }

  @Override
  public void deleteModVersionReview(String id) {

  }

  @Override
  public Optional<Game> findReplayById(int id) {
    return Optional.empty();
  }

  @Override
  public Tuple<List<Mod>, java.util.Map<String, ?>> findModsByQueryWithMeta(SearchConfig query, int maxResults, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public List<MatchmakerQueueMapPool> getMatchmakerPools(int matchmakerQueueId) {
    return List.of();
  }

  @Override
  public Optional<MatchmakerQueue> getMatchmakerQueue(String technicalName) {
    return Optional.empty();
  }

  @Override
  public List<Tournament> getAllTournaments() {
    return List.of();
  }

  @Override
  public Tuple<List<MapVersion>, java.util.Map<String, ?>> getOwnedMapsWithMeta(int playerId, int loadMoreCount, int page) {
    return new Tuple(List.of(), Collections.emptyMap());
  }

  @Override
  public void updateMapVersion(String id, MapVersion mapVersion) {
  }

  @Override
  public MeResult getOwnPlayer() {
    return null;
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) {

  }

  @Override
  public List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return List.of();
  }
}
