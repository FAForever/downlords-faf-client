package com.faforever.client.builders;

import com.faforever.commons.api.dto.AchievementDefinition;
import com.faforever.commons.api.dto.AchievementState;
import com.faforever.commons.api.dto.AchievementType;

public class AchievementDefinitionBuilder {

  private final AchievementDefinition achievementDefinition;

  private AchievementDefinitionBuilder() {
    achievementDefinition = new AchievementDefinition();
  }

  public static AchievementDefinitionBuilder create() {
    return new AchievementDefinitionBuilder();
  }

  public AchievementDefinitionBuilder defaultValues() {
    achievementDefinition.setId("1-2-3");
    achievementDefinition.setName("Name");
    achievementDefinition.setDescription("Description");
    achievementDefinition.setInitialState(AchievementState.REVEALED);
    achievementDefinition.setUnlockedIconUrl("http://127.0.0.1:65354/unlocked/1-2-3.png");
    achievementDefinition.setRevealedIconUrl("http://127.0.0.1:65354/revealed/1-2-3.png");
    achievementDefinition.setExperiencePoints(10);
    achievementDefinition.setTotalSteps(100);
    achievementDefinition.setType(AchievementType.INCREMENTAL);
    return this;
  }

  public AchievementDefinitionBuilder id(String id) {
    achievementDefinition.setId(id);
    return this;
  }

  public AchievementDefinitionBuilder type(AchievementType type) {
    achievementDefinition.setType(type);
    return this;
  }

  public AchievementDefinition get() {
    return achievementDefinition;
  }
}
