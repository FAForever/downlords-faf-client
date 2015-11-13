package com.faforever.client.legacy;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.events.AchievementDefinition;
import com.faforever.client.events.AchievementUpdatesRequest;
import com.faforever.client.events.EventUpdatesRequest;
import com.faforever.client.events.PlayerAchievement;
import com.faforever.client.events.UpdatedAchievement;
import com.faforever.client.events.UpdatedEvent;

import java.util.Collections;
import java.util.List;

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
  public List<UpdatedAchievement> executeAchievementUpdates(AchievementUpdatesRequest achievementUpdatesRequest, int playerId) {
    return Collections.emptyList();
  }

  @Override
  public AchievementDefinition getAchievementDefinition(String achievementId) {
    return null;
  }

  @Override
  public List<UpdatedEvent> recordEvents(EventUpdatesRequest eventUpdatesRequest, int playerId) {
    return Collections.emptyList();
  }

  @Override
  public void authorize(int playerId) {

  }
}
