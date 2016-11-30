package com.faforever.client.api;

import com.faforever.client.coop.CoopMission;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.io.ByteCountListener;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.map.MapBean;
import com.faforever.client.mod.ModInfoBean;
import com.faforever.client.replay.ReplayInfoBean;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
  public void authorize(int playerId) {

  }

  @Override
  public List<ModInfoBean> getMods() {
    return Arrays.asList(
        ModInfoBean.fromModInfo(new Mod("1-1-1", "Mod Number One", "Mod description Apple", "Mock", LocalDateTime.now())),
        ModInfoBean.fromModInfo(new Mod("2-2-2", "Mod Number Two", "Mod description Banana", "Mock", LocalDateTime.now())),
        ModInfoBean.fromModInfo(new Mod("3-3-3", "Mod Number Three", "Mod description Citrus", "Mock", LocalDateTime.now())),
        ModInfoBean.fromModInfo(new Mod("4-4-4", "Mod Number Four", "Mod description Date", "Mock", LocalDateTime.now())),
        ModInfoBean.fromModInfo(new Mod("5-5-5", "Mod Number Five", "Mod description Elderberry", "Mock", LocalDateTime.now())),
        ModInfoBean.fromModInfo(new Mod("6-6-6", "Mod Number Six", "Mod description Fig", "Mock", LocalDateTime.now())),
        ModInfoBean.fromModInfo(new Mod("7-7-7", "Mod Number Seven", "Mod description Garlic", "Mock", LocalDateTime.now())),
        ModInfoBean.fromModInfo(new Mod("8-8-8", "Mod Number Eight", "Mod description Haricot bean", "Mock", LocalDateTime.now()))
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
  public MapBean findMapByName(String mapId) {
    return null;
  }

  @Override
  public List<Ranked1v1EntryBean> getRanked1v1Entries() {
    return null;
  }

  @Override
  public Ranked1v1Stats getRanked1v1Stats() {
    return null;
  }

  @Override
  public Ranked1v1EntryBean getRanked1v1EntryForPlayer(int playerId) {
    return null;
  }

  @Override
  public History getRatingHistory(RatingType ratingType, int playerId) {
    return new History();
  }

  @Override
  public List<MapBean> getMaps() {
    return Collections.emptyList();
  }

  @Override
  public List<MapBean> getMostDownloadedMaps(int count) {
    return null;
  }

  @Override
  public List<MapBean> getMostPlayedMaps(int count) {
    return null;
  }

  @Override
  public List<MapBean> getBestRatedMaps(int count) {
    return null;
  }

  @Override
  public List<MapBean> getNewestMaps(int count) {
    return null;
  }

  @Override
  public void uploadMod(Path file, ByteCountListener listener) throws IOException {

  }

  @Override
  public void uploadMap(Path file, boolean isRanked, ByteCountListener listener) {

  }

  @Override
  public ModInfoBean getMod(String uid) {
    return null;
  }

  @Override
  public CompletionStage<List<ReplayInfoBean>> getOnlineReplays() {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public List<FeaturedModFile> getFeaturedModFiles(FeaturedModBean featuredModBean, Integer version) {
    return null;
  }

  @Override
  public void changePassword(String currentPasswordHash, String newPasswordHash) {

  }

  @Override
  public List<CoopMission> getCoopMissions() {
    return Collections.emptyList();
  }

  @Override
  public List<CoopLeaderboardEntry> getCoopLeaderboard(String missionId, int numberOfPlayers) {
    return Collections.emptyList();
  }

}
