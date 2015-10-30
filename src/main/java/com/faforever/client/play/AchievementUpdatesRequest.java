package com.faforever.client.play;

import com.google.api.client.util.Key;

import java.util.ArrayList;
import java.util.Collection;

public class AchievementUpdatesRequest {

  @Key("updates")
  private Collection<AchievementUpdate> updates;

  public AchievementUpdatesRequest() {
    updates = new ArrayList<>();
  }

  public AchievementUpdatesRequest unlock(String achievementId) {
    updates.add(new AchievementUpdate(AchievementUpdateType.UNLOCK, achievementId, null));
    return this;
  }

  public AchievementUpdatesRequest increment(String achievementId, int steps) {
    updates.add(new AchievementUpdate(AchievementUpdateType.INCREMENT, achievementId, steps));
    return this;
  }

  public AchievementUpdatesRequest setStepsAtLeast(String achievementId, int steps) {
    updates.add(new AchievementUpdate(AchievementUpdateType.SET_STEPS_AT_LEAST, achievementId, steps));
    return this;
  }

  public Collection<AchievementUpdate> getUpdates() {
    return updates;
  }

}
