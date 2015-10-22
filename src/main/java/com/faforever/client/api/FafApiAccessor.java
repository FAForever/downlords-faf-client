package com.faforever.client.api;

import com.faforever.client.play.AchievementDefinition;
import com.faforever.client.play.AchievementUpdatesRequest;
import com.faforever.client.play.EventUpdatesRequest;
import com.faforever.client.play.PlayerAchievement;
import com.faforever.client.play.UpdatedAchievement;
import com.faforever.client.play.UpdatedEvent;

import java.util.List;

public interface FafApiAccessor {

  List<PlayerAchievement> getPlayerAchievements(int playerId);

  List<AchievementDefinition> getAchievementDefinitions();

  List<UpdatedAchievement> executeAchievementUpdates(AchievementUpdatesRequest achievementUpdatesRequest, int playerId);

  AchievementDefinition getAchievementDefinition(String achievementId);

  List<UpdatedEvent> recordEvents(EventUpdatesRequest eventUpdatesRequest, int playerId);

  void authorize(int playerId);
}
