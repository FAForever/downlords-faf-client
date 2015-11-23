package com.faforever.client.achievements;

public class AchievementDefinitionBuilder {

  private final AchievementDefinition achievementDefinition;

  private AchievementDefinitionBuilder() {
    achievementDefinition = new AchievementDefinition();
  }

  public AchievementDefinitionBuilder defaultValues() {
    achievementDefinition.setId("1-2-3");
    achievementDefinition.setName("Name");
    achievementDefinition.setDescription("Description");
    achievementDefinition.setInitialState(AchievementState.REVEALED);
    achievementDefinition.setUnlockedIconUrl("http://www.example.com/unlocked.png");
    achievementDefinition.setRevealedIconUrl("http://www.example.com/revealed.png");
    achievementDefinition.setExperiencePoints(10);
    achievementDefinition.setTotalSteps(100);
    achievementDefinition.setType(AchievementType.INCREMENTAL);
    return this;
  }

  public AchievementDefinitionBuilder type(AchievementType type) {
    achievementDefinition.setType(type);
    return this;
  }

  public AchievementDefinition get() {
    return achievementDefinition;
  }

  public static AchievementDefinitionBuilder create() {
    return new AchievementDefinitionBuilder();
  }
}
