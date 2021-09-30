package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeagueBean;
import com.faforever.client.domain.LeagueSeasonBean;

import java.time.OffsetDateTime;

public class LeagueSeasonBeanBuilder {
  private final LeagueSeasonBean leagueSeasonBean = new LeagueSeasonBean();

  public static LeagueSeasonBeanBuilder create() {
    return new LeagueSeasonBeanBuilder();
  }

  public LeagueSeasonBeanBuilder defaultValues() {
    league(LeagueBeanBuilder.create().defaultValues().get());
    leaderboard(LeaderboardBeanBuilder.create().defaultValues().get());
    nameKey("test_description");
    startDate(OffsetDateTime.now().minusDays(1));
    endDate(OffsetDateTime.now().plusDays(1));
    id(0);
    return this;
  }

  public LeagueSeasonBeanBuilder league(LeagueBean league) {
    leagueSeasonBean.setLeague(league);
    return this;
  }

  public LeagueSeasonBeanBuilder leaderboard(LeaderboardBean leaderboard) {
    leagueSeasonBean.setLeaderboard(leaderboard);
    return this;
  }

  public LeagueSeasonBeanBuilder nameKey(String nameKey) {
    leagueSeasonBean.setNameKey(nameKey);
    return this;
  }

  public LeagueSeasonBeanBuilder startDate(OffsetDateTime startDate) {
    leagueSeasonBean.setStartDate(startDate);
    return this;
  }

  public LeagueSeasonBeanBuilder endDate(OffsetDateTime endDate) {
    leagueSeasonBean.setEndDate(endDate);
    return this;
  }

  public LeagueSeasonBeanBuilder id(Integer id) {
    leagueSeasonBean.setId(id);
    return this;
  }

  public LeagueSeasonBeanBuilder createTime(OffsetDateTime createTime) {
    leagueSeasonBean.setCreateTime(createTime);
    return this;
  }

  public LeagueSeasonBeanBuilder updateTime(OffsetDateTime updateTime) {
    leagueSeasonBean.setUpdateTime(updateTime);
    return this;
  }

  public LeagueSeasonBean get() {
    return leagueSeasonBean;
  }

}
