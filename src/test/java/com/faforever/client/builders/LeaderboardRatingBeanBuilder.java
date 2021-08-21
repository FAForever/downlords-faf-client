package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardRatingBean;


public class LeaderboardRatingBeanBuilder {
  public static LeaderboardRatingBeanBuilder create() {
    return new LeaderboardRatingBeanBuilder();
  }

  private final LeaderboardRatingBean leaderboardRatingBean = new LeaderboardRatingBean();

  public LeaderboardRatingBeanBuilder defaultValues() {
    deviation(10);
    mean(100);
    numberOfGames(100);
    return this;
  }

  public LeaderboardRatingBeanBuilder deviation(float deviation) {
    leaderboardRatingBean.setDeviation(deviation);
    return this;
  }

  public LeaderboardRatingBeanBuilder mean(float mean) {
    leaderboardRatingBean.setMean(mean);
    return this;
  }

  public LeaderboardRatingBeanBuilder numberOfGames(int numberOfGames) {
    leaderboardRatingBean.setNumberOfGames(numberOfGames);
    return this;
  }

  public LeaderboardRatingBean get() {
    return leaderboardRatingBean;
  }

}

