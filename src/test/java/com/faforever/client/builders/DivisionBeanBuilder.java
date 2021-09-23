package com.faforever.client.builders;

import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.LeagueSeasonBean;

import java.time.OffsetDateTime;

public class DivisionBeanBuilder {
  public static DivisionBeanBuilder create() {
    return new DivisionBeanBuilder();
  }

  private final DivisionBean divisionBean = new DivisionBean();

  public DivisionBeanBuilder defaultValues() {
    index(1);
    nameKey("test_description");
    descriptionKey("test_name");
    leagueSeason(LeagueSeasonBeanBuilder.create().defaultValues().get());
    return this;
  }

  public DivisionBeanBuilder index(int index) {
    divisionBean.setIndex(index);
    return this;
  }

  public DivisionBeanBuilder nameKey(String nameKey) {
    divisionBean.setNameKey(nameKey);
    return this;
  }

  public DivisionBeanBuilder descriptionKey(String descriptionKey) {
    divisionBean.setDescriptionKey(descriptionKey);
    return this;
  }

  public DivisionBeanBuilder leagueSeason(LeagueSeasonBean leagueSeason) {
    divisionBean.setLeagueSeason(leagueSeason);
    return this;
  }

  public DivisionBeanBuilder id(Integer id) {
    divisionBean.setId(id);
    return this;
  }

  public DivisionBeanBuilder createTime(OffsetDateTime createTime) {
    divisionBean.setCreateTime(createTime);
    return this;
  }

  public DivisionBeanBuilder updateTime(OffsetDateTime updateTime) {
    divisionBean.setUpdateTime(updateTime);
    return this;
  }

  public DivisionBean get() {
    return divisionBean;
  }

}
