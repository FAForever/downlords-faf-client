package com.faforever.client.builders;

import com.faforever.client.domain.LeagueBean;

import java.time.OffsetDateTime;

public class LeagueBeanBuilder {
  public static LeagueBeanBuilder create() {
    return new LeagueBeanBuilder();
  }

  private final LeagueBean leagueBean = new LeagueBean();

  public LeagueBeanBuilder defaultValues() {
    technicalName("test");
    nameKey("test_description");
    descriptionKey("test_name");
    id(0);
    return this;
  }

  public LeagueBeanBuilder technicalName(String technicalName) {
    leagueBean.setTechnicalName(technicalName);
    return this;
  }

  public LeagueBeanBuilder nameKey(String nameKey) {
    leagueBean.setNameKey(nameKey);
    return this;
  }

  public LeagueBeanBuilder descriptionKey(String descriptionKey) {
    leagueBean.setDescriptionKey(descriptionKey);
    return this;
  }

  public LeagueBeanBuilder id(Integer id) {
    leagueBean.setId(id);
    return this;
  }

  public LeagueBeanBuilder createTime(OffsetDateTime createTime) {
    leagueBean.setCreateTime(createTime);
    return this;
  }

  public LeagueBeanBuilder updateTime(OffsetDateTime updateTime) {
    leagueBean.setUpdateTime(updateTime);
    return this;
  }

  public LeagueBean get() {
    return leagueBean;
  }

}
