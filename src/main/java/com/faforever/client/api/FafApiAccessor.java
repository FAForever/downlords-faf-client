package com.faforever.client.api;

import com.faforever.client.events.AchievementDefinition;
import com.faforever.client.events.AchievementUpdatesRequest;
import com.faforever.client.events.EventUpdatesRequest;
import com.faforever.client.events.PlayerAchievement;
import com.faforever.client.events.UpdatedAchievement;
import com.faforever.client.events.UpdatedEvent;

import java.util.List;

public interface FafApiAccessor {

  List<PlayerAchievement> getPlayerAchievements(int playerId);

  List<AchievementDefinition> getAchievementDefinitions();

  List<UpdatedAchievement> executeAchievementUpdates(AchievementUpdatesRequest achievementUpdatesRequest, int playerId);

  AchievementDefinition getAchievementDefinition(String achievementId);

  List<UpdatedEvent> recordEvents(EventUpdatesRequest eventUpdatesRequest, int playerId);

  void authorize(int playerId);
}
