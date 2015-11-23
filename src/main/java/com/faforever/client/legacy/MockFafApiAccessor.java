package com.faforever.client.legacy;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.events.AchievementDefinition;
import com.faforever.client.events.PlayerAchievement;
import com.faforever.client.mod.ModInfoBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MockFafApiAccessor implements FafApiAccessor {

  @Override
  public List<PlayerAchievement> getPlayerAchievements(int playerId) {
    return Collections.emptyList();
  }

  @Override
  public List<AchievementDefinition> getAchievementDefinitions() {
    return Collections.emptyList();
  }

  @Override
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    return null;
  }

  @Override
  public List<String> getModNames() {
    return Arrays.asList("Supreme Economy", "AZUI", "Terrain Deform for FA", "Economy Tools FA", "Range Tester",
        "Final Rush Pro v3", "UI Mass Fab Manager", "Blackops Special Weapons", "RAS_Notify", "Notify Enhanced",
        "Notify Enhanced 1.1");
  }

  @Override
  public void authorize(int playerId) {

  }

  @Override
  public CompletableFuture<List<ModInfoBean>> getMods() {
    return CompletableFuture.completedFuture(Arrays.asList(
        new ModInfoBean("1-1-1", "Mod Number One", "Mod description Apple", "Mock"),
        new ModInfoBean("2-2-2", "Mod Number Two", "Mod description Banana", "Mock"),
        new ModInfoBean("3-3-3", "Mod Number Three", "Mod description Citrus", "Mock"),
        new ModInfoBean("4-4-4", "Mod Number Four", "Mod description Date", "Mock"),
        new ModInfoBean("5-5-5", "Mod Number Five", "Mod description Elderberry", "Mock"),
        new ModInfoBean("6-6-6", "Mod Number Six", "Mod description Fig", "Mock"),
        new ModInfoBean("7-7-7", "Mod Number Seven", "Mod description Garlic", "Mock"),
        new ModInfoBean("8-8-8", "Mod Number Eight", "Mod description Haricot bean", "Mock")
    ));
  }

}
