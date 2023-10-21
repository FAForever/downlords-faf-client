package com.faforever.client.builders;

import com.faforever.client.domain.LeagueScoreJournalBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.SubdivisionBean;

public class LeagueScoreJournalBeanBuilder {
  private final LeagueScoreJournalBean leagueScoreJournalBean = new LeagueScoreJournalBean();
  
  public static LeagueScoreJournalBeanBuilder create() {
    return new LeagueScoreJournalBeanBuilder();
  }

  public LeagueScoreJournalBeanBuilder defaultValues() {
    gameId(1);
    loginId(100);
    gameCount(11);
    scoreBefore(5);
    scoreAfter(6);
    season(LeagueSeasonBeanBuilder.create().defaultValues().get());
    divisionBefore(SubdivisionBeanBuilder.create().defaultValues().get());
    divisionAfter(SubdivisionBeanBuilder.create().defaultValues().get());
    return this;
  }

  public LeagueScoreJournalBeanBuilder gameId(int gameId) {
    leagueScoreJournalBean.setGameId(gameId);
    return this;
  }

  public LeagueScoreJournalBeanBuilder loginId(int loginId) {
    leagueScoreJournalBean.setLoginId(loginId);
    return this;
  }

  public LeagueScoreJournalBeanBuilder gameCount(int gameCount) {
    leagueScoreJournalBean.setGameCount(gameCount);
    return this;
  }

  public LeagueScoreJournalBeanBuilder scoreBefore(int scoreBefore) {
    leagueScoreJournalBean.setScoreBefore(scoreBefore);
    return this;
  }

  public LeagueScoreJournalBeanBuilder scoreAfter(int scoreAfter) {
    leagueScoreJournalBean.setScoreAfter(scoreAfter);
    return this;
  }

  public LeagueScoreJournalBeanBuilder season(LeagueSeasonBean season) {
    leagueScoreJournalBean.setSeason(season);
    return this;
  }

  public LeagueScoreJournalBeanBuilder divisionBefore(SubdivisionBean divisionBefore) {
    leagueScoreJournalBean.setDivisionBefore(divisionBefore);
    return this;
  }

  public LeagueScoreJournalBeanBuilder divisionAfter(SubdivisionBean divisionAfter) {
    leagueScoreJournalBean.setDivisionAfter(divisionAfter);
    return this;
  }

  public LeagueScoreJournalBean get() {
    return leagueScoreJournalBean;
  }
}
