package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.PlayerBean;

import java.time.OffsetDateTime;


public class LeaderboardEntryBeanBuilder {
  public static LeaderboardEntryBeanBuilder create() {
    return new LeaderboardEntryBeanBuilder();
  }

  private final LeaderboardEntryBean leaderboardEntryBean = new LeaderboardEntryBean();

  public LeaderboardEntryBeanBuilder defaultValues() {
    leaderboard(LeaderboardBeanBuilder.create().defaultValues().get());
    id(0);
    player(PlayerBeanBuilder.create().defaultValues().get());
    rating(100);
    gamesPlayed(100);
    winLossRatio(.5f);
    return this;
  }

  public LeaderboardEntryBeanBuilder player(PlayerBean player) {
    leaderboardEntryBean.setPlayer(player);
    return this;
  }

  public LeaderboardEntryBeanBuilder rating(double rating) {
    leaderboardEntryBean.setRating(rating);
    return this;
  }

  public LeaderboardEntryBeanBuilder gamesPlayed(int gamesPlayed) {
    leaderboardEntryBean.setGamesPlayed(gamesPlayed);
    return this;
  }

  public LeaderboardEntryBeanBuilder winLossRatio(float winLossRatio) {
    leaderboardEntryBean.setWinLossRatio(winLossRatio);
    return this;
  }

  public LeaderboardEntryBeanBuilder leaderboard(LeaderboardBean leaderboard) {
    leaderboardEntryBean.setLeaderboard(leaderboard);
    return this;
  }

  public LeaderboardEntryBeanBuilder id(Integer id) {
    leaderboardEntryBean.setId(id);
    return this;
  }

  public LeaderboardEntryBeanBuilder createTime(OffsetDateTime createTime) {
    leaderboardEntryBean.setCreateTime(createTime);
    return this;
  }

  public LeaderboardEntryBeanBuilder updateTime(OffsetDateTime updateTime) {
    leaderboardEntryBean.setUpdateTime(updateTime);
    return this;
  }

  public LeaderboardEntryBean get() {
    return leaderboardEntryBean;
  }

}

