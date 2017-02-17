package com.faforever.client.api;

import com.faforever.client.FafClientApplication;
import com.faforever.client.api.dto.AchievementDefinition;
import com.faforever.client.api.dto.AchievementType;
import com.faforever.client.api.dto.CoopMission;
import com.faforever.client.api.dto.CoopResult;
import com.faforever.client.api.dto.FeaturedMod;
import com.faforever.client.api.dto.FeaturedModFile;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.GamePlayerStats;
import com.faforever.client.api.dto.GlobalLeaderboardEntry;
import com.faforever.client.api.dto.Ladder1v1LeaderboardEntry;
import com.faforever.client.api.dto.Map;
import com.faforever.client.api.dto.Mod;
import com.faforever.client.api.dto.PlayerAchievement;
import com.faforever.client.api.dto.PlayerEvent;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.io.ProgressListener;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.replay.Replay;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Lazy
@Component
@Profile(FafClientApplication.POFILE_OFFLINE)
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
  public void authorize(int playerId, String username, String password) {

  }

  @Override
  public List<Mod> getMods() {
    return Arrays.asList(
        new com.faforever.client.api.dto.Mod("1-1-1", "Mod Number One", "Mock", OffsetDateTime.now()),
        new com.faforever.client.api.dto.Mod("2-2-2", "Mod Number Two", "Mock", OffsetDateTime.now()),
        new com.faforever.client.api.dto.Mod("3-3-3", "Mod Number Three", "Mock", OffsetDateTime.now()),
        new com.faforever.client.api.dto.Mod("4-4-4", "Mod Number Four", "Mock", OffsetDateTime.now()),
        new com.faforever.client.api.dto.Mod("5-5-5", "Mod Number Five", "Mock", OffsetDateTime.now()),
        new com.faforever.client.api.dto.Mod("6-6-6", "Mod Number Six", "Mock", OffsetDateTime.now()),
        new com.faforever.client.api.dto.Mod("7-7-7", "Mod Number Seven", "Mock", OffsetDateTime.now()),
        new com.faforever.client.api.dto.Mod("8-8-8", "Mod Number Eight", "Mock", OffsetDateTime.now())
    );
  }

  @Override
  public List<FeaturedMod> getFeaturedMods() {
    FeaturedMod featuredMod = new FeaturedMod();
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
  public List<Map> getAllMaps() {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getMostDownloadedMaps(int count) {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getMostPlayedMaps(int count) {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getHighestRatedMaps(int count) {
    return Collections.emptyList();
  }

  @Override
  public List<Map> getNewestMaps(int count) {
    return Collections.emptyList();
  }

  @Override
  public void uploadMod(Path file, ProgressListener listener) {

  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ProgressListener listener) {

  }

  @Override
  public List<CoopMission> getCoopMissions() {
    return Collections.emptyList();
  }

  @Override
  public Mod getMod(String uid) {
    return null;
  }

  @Override
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedModBean featuredModBean, Integer version) {
    return Collections.emptyList();
  }

  @Override
  public List<Replay> searchReplayByPlayerName(String playerName) {
    return Collections.emptyList();
  }

  @Override
  public List<Replay> searchReplayByMapName(String mapName) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> searchReplayByMod(FeaturedMod featuredMod) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> getNewestReplays(int count) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> getHighestRatedReplays(int count) {
    return Collections.emptyList();
  }

  @Override
  public List<Game> getMostWatchedReplays(int count) {
    return Collections.emptyList();
  }

  @Override
  public void changePassword(String username, String currentPasswordHash, String newPasswordHash) {

  }

  @Override
  public List<CoopResult> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return Collections.emptyList();
  }
}
