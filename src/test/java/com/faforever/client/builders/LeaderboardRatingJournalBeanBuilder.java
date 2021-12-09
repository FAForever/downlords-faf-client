package com.faforever.client.builders;

import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardRatingJournalBean;

import java.time.OffsetDateTime;


public class LeaderboardRatingJournalBeanBuilder {
  public static LeaderboardRatingJournalBeanBuilder create() {
    return new LeaderboardRatingJournalBeanBuilder();
  }

  private final LeaderboardRatingJournalBean leaderboardRatingJournalBean = new LeaderboardRatingJournalBean();

  public LeaderboardRatingJournalBeanBuilder defaultValues() {
    id(0);
    meanAfter(null);
    deviationAfter(null);
    meanBefore(100d);
    deviationBefore(10d);
    leaderboard(LeaderboardBeanBuilder.create().defaultValues().get());
    gamePlayerStats(GamePlayerStatsBeanBuilder.create().defaultValues().get());
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder meanAfter(Double meanAfter) {
    leaderboardRatingJournalBean.setMeanAfter(meanAfter);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder deviationAfter(Double deviationAfter) {
    leaderboardRatingJournalBean.setDeviationAfter(deviationAfter);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder meanBefore(Double meanBefore) {
    leaderboardRatingJournalBean.setMeanBefore(meanBefore);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder deviationBefore(Double deviationBefore) {
    leaderboardRatingJournalBean.setDeviationBefore(deviationBefore);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder gamePlayerStats(GamePlayerStatsBean gamePlayerStats) {
    leaderboardRatingJournalBean.setGamePlayerStats(gamePlayerStats);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder leaderboard(LeaderboardBean leaderboard) {
    leaderboardRatingJournalBean.setLeaderboard(leaderboard);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder id(Integer id) {
    leaderboardRatingJournalBean.setId(id);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder createTime(OffsetDateTime createTime) {
    leaderboardRatingJournalBean.setCreateTime(createTime);
    return this;
  }

  public LeaderboardRatingJournalBeanBuilder updateTime(OffsetDateTime updateTime) {
    leaderboardRatingJournalBean.setUpdateTime(updateTime);
    return this;
  }

  public LeaderboardRatingJournalBean get() {
    return leaderboardRatingJournalBean;
  }

}

