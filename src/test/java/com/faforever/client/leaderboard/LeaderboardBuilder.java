package com.faforever.client.leaderboard;

import java.time.OffsetDateTime;

public class LeaderboardBuilder {
  private final Leaderboard leaderboard = new Leaderboard();

  public static LeaderboardBuilder create() {
    return new LeaderboardBuilder();
  }

  public LeaderboardBuilder defaultValues() {
    id(1);
    createTime(OffsetDateTime.MIN);
    updateTime(OffsetDateTime.MAX);
    descriptionKey("test_description");
    nameKey("test_name");
    technicalName("global");
    return this;
  }


  public LeaderboardBuilder id(Integer id) {
    leaderboard.setId(id);
    return this;
  }

  public LeaderboardBuilder createTime(OffsetDateTime createTime) {
    leaderboard.setCreateTime(createTime);
    return this;
  }

  public LeaderboardBuilder updateTime(OffsetDateTime updateTime) {
    leaderboard.setUpdateTime(updateTime);
    return this;
  }

  public LeaderboardBuilder descriptionKey(String descriptionKey) {
    leaderboard.setDescriptionKey(descriptionKey);
    return this;
  }

  public LeaderboardBuilder nameKey(String nameKey) {
    leaderboard.setNameKey(nameKey);
    return this;
  }

  public LeaderboardBuilder technicalName(String technicalName) {
    leaderboard.setTechnicalName(technicalName);
    return this;
  }

  public Leaderboard get() {
    return leaderboard;
  }
}
