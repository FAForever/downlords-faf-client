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
import com.faforever.client.api.dto.Ladder1v1Map;
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.MapVersion;
import com.faforever.client.api.dto.MapVersionReview;
import com.faforever.client.api.dto.MeResult;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.ModVersion;
import com.faforever.client.api.dto.ModVersionReview;
import com.faforever.client.api.dto.Player;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.api.dto.Tournament;
import com.faforever.client.api.dto.TutorialCategory;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.FeaturedMod;
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
    return Collections.emptyList();
  }

  @Override
  public List<PlayerEvent> getPlayerEvents(int playerId) {
    return null;
  }

  @Override
  public List<AchievementDefinition> getAchievementDefinitions() {
    return Collections.emptyList();
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
  public String authorize(String username, String password) {
    return null;
  }

  @Override
  public void authorize(String refreshToken) {

  }

  @Override
  public List<Mod> getMods() {
    Player uploader = new Player();
    return Arrays.asList(
        new com.faforever.client.api.dto.Mod("1", "Mod Number One", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
        new com.faforever.client.api.dto.Mod("2", "Mod Number Two", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
        new com.faforever.client.api.dto.Mod("3", "Mod Number Three", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
        new com.faforever.client.api.dto.Mod("4", "Mod Number Four", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
        new com.faforever.client.api.dto.Mod("5", "Mod Number Five", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
        new com.faforever.client.api.dto.Mod("6", "Mod Number Six", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
        new com.faforever.client.api.dto.Mod("7", "Mod Number Seven", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod()),
        new com.faforever.client.api.dto.Mod("8", "Mod Number Eight", "Mock", OffsetDateTime.now(), OffsetDateTime.now(), uploader, Collections.emptyList(), mod())
    );
  }

  private ModVersion mod() {
    return new ModVersion();
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
    return Collections.emptyList();
  }

  @Override
  public List<GlobalLeaderboardEntry> getGlobalLeaderboard() {
    return Collections.emptyList();
  }

  @Override
  public Ladder1v1LeaderboardEntry getLadder1v1EntryForPlayer(int playerId) {
    return null;
  }

  @Override
  public List<GamePlayerStats> getGamePlayerStats(int playerId, KnownFeaturedMod knownFeaturedMod) {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getMapsById(List<Integer> mapIdList, int count, int page) {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getMostPlayedMaps(int count, int page) {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getHighestRatedMaps(int count, int page) {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getNewestMaps(int count, int page) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> getLastGamesOnMap(int playerId, String mapVersionId, int count) {
    return Collections.emptyList();
  }

  @Override
  public void uploadMod(Path file, ByteCountListener listener) {

  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ByteCountListener listener) {

  }

  @Override
  public List<CoopMission> getCoopMissions() {
    return Collections.emptyList();
  }

  @Override
  public ModVersion getModVersion(String uid) {
    return null;
  }

  @Override
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedMod featuredMod, Integer version) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> getNewestReplays(int count, int page) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> getHighestRatedReplays(int count, int page) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> findReplaysByQuery(String query, int maxResults, int page, SortConfig sortConfig) {
    return Collections.emptyList();
  }

  @Override
  public Optional<MapVersion> findMapByFolderName(String folderName) {
    return Optional.empty();
  }

  @Override
  public List<com.faforever.client.api.dto.Player> getPlayersByIds(Collection<Integer> playerIds) {
    return Collections.emptyList();
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
    return Collections.emptyList();
  }

  @Override
  public Optional<Clan> getClanByTag(String tag) {
    return Optional.empty();
  }

  @Override
  public List<Map> findMapsByQuery(SearchConfig searchConfig, int page, int count) {
    return Collections.emptyList();
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
  public List<Mod> findModsByQuery(SearchConfig query, int page, int maxResults) {
    return Collections.emptyList();
  }

  @Override
  public List<Ladder1v1Map> getLadder1v1Maps(int count, int page) {
    return Collections.emptyList();
  }

  @Override
  public List<Tournament> getAllTournaments() {
    return Collections.emptyList();
  }

  @Override
  public List<MapVersion> getOwnedMaps(int playerId, int loadMoreCount, int page) {
    return Collections.emptyList();
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
    return Collections.emptyList();
  }
}
