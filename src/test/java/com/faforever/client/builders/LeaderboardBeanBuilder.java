package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardBean;

import java.time.OffsetDateTime;


public class LeaderboardBeanBuilder {
  public static LeaderboardBeanBuilder create() {
    return new LeaderboardBeanBuilder();
  }

  private final LeaderboardBean leaderboardBean = new LeaderboardBean();

  public LeaderboardBeanBuilder defaultValues() {
    descriptionKey("test_description");
    nameKey("test_name");
    technicalName("test");
    id(0);
    return this;
  }

  public LeaderboardBeanBuilder descriptionKey(String descriptionKey) {
    leaderboardBean.setDescriptionKey(descriptionKey);
    return this;
  }

  public LeaderboardBeanBuilder nameKey(String nameKey) {
    leaderboardBean.setNameKey(nameKey);
    return this;
  }

  public LeaderboardBeanBuilder technicalName(String technicalName) {
    leaderboardBean.setTechnicalName(technicalName);
    return this;
  }

  public LeaderboardBeanBuilder id(Integer id) {
    leaderboardBean.setId(id);
    return this;
  }

  public LeaderboardBeanBuilder createTime(OffsetDateTime createTime) {
    leaderboardBean.setCreateTime(createTime);
    return this;
  }

  public LeaderboardBeanBuilder updateTime(OffsetDateTime updateTime) {
    leaderboardBean.setUpdateTime(updateTime);
    return this;
  }

  public LeaderboardBean get() {
    return leaderboardBean;
  }

}

