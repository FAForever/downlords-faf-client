package com.faforever.client.builders;

import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;

import java.time.OffsetDateTime;

public class LeagueEntryBeanBuilder {
  private final LeagueEntryBean leagueEntryBean = new LeagueEntryBean();

  public static LeagueEntryBeanBuilder create() {
    return new LeagueEntryBeanBuilder();
  }

  public LeagueEntryBeanBuilder defaultValues() {
    player(PlayerBeanBuilder.create().defaultValues().get());
    gamesPlayed(100);
    score(5);
    leagueSeason(LeagueSeasonBeanBuilder.create().defaultValues().get());
    subdivision(SubdivisionBeanBuilder.create().defaultValues().get());
    return this;
  }

  public LeagueEntryBeanBuilder player(PlayerBean player) {
    leagueEntryBean.setPlayer(player);
    return this;
  }

  public LeagueEntryBeanBuilder gamesPlayed(int gamesPlayed) {
    leagueEntryBean.setGamesPlayed(gamesPlayed);
    return this;
  }

  public LeagueEntryBeanBuilder leagueSeason(LeagueSeasonBean leagueSeason) {
    leagueEntryBean.setLeagueSeason(leagueSeason);
    return this;
  }

  public LeagueEntryBeanBuilder score(int score) {
    leagueEntryBean.setScore(score);
    return this;
  }

  public LeagueEntryBeanBuilder rank(int rank) {
    leagueEntryBean.setRank(rank);
    return this;
  }

  public LeagueEntryBeanBuilder subdivision(SubdivisionBean subdivisionBean) {
    leagueEntryBean.setSubdivision(subdivisionBean);
    return this;
  }

  public LeagueEntryBeanBuilder id(Integer id) {
    leagueEntryBean.setId(id);
    return this;
  }

  public LeagueEntryBeanBuilder createTime(OffsetDateTime createTime) {
    leagueEntryBean.setCreateTime(createTime);
    return this;
  }

  public LeagueEntryBeanBuilder updateTime(OffsetDateTime updateTime) {
    leagueEntryBean.setUpdateTime(updateTime);
    return this;
  }

  public LeagueEntryBean get() {
    return leagueEntryBean;
  }

}
