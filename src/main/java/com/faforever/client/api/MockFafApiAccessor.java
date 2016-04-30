package com.faforever.client.api;

import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.mod.ModInfoBean;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    return null;
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
  public void uploadMod(InputStream inputStream) {

  }
}
