package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.MatchmakerQueueBean.MatchingStatus;

import java.time.OffsetDateTime;


public class MatchmakerQueueBeanBuilder {
  public static MatchmakerQueueBeanBuilder create() {
    return new MatchmakerQueueBeanBuilder();
  }

  private final MatchmakerQueueBean matchmakerQueueBean = new MatchmakerQueueBean();

  public MatchmakerQueueBeanBuilder defaultValues() {
    technicalName("test");
    queuePopTime(OffsetDateTime.now());
    teamSize(1);
    playersInQueue(2);
    joined(false);
    matchingStatus(null);
    leaderboard(LeaderboardBeanBuilder.create().defaultValues().get());
    id(0);
    return this;
  }

  public MatchmakerQueueBeanBuilder technicalName(String technicalName) {
    matchmakerQueueBean.setTechnicalName(technicalName);
    return this;
  }

  public MatchmakerQueueBeanBuilder queuePopTime(OffsetDateTime queuePopTime) {
    matchmakerQueueBean.setQueuePopTime(queuePopTime);
    return this;
  }

  public MatchmakerQueueBeanBuilder teamSize(int teamSize) {
    matchmakerQueueBean.setTeamSize(teamSize);
    return this;
  }

  public MatchmakerQueueBeanBuilder playersInQueue(int playersInQueue) {
    matchmakerQueueBean.setPlayersInQueue(playersInQueue);
    return this;
  }

  public MatchmakerQueueBeanBuilder joined(boolean joined) {
    matchmakerQueueBean.setJoined(joined);
    return this;
  }

  public MatchmakerQueueBeanBuilder matchingStatus(MatchingStatus matchingStatus) {
    matchmakerQueueBean.setMatchingStatus(matchingStatus);
    return this;
  }

  public MatchmakerQueueBeanBuilder leaderboard(LeaderboardBean leaderboard) {
    matchmakerQueueBean.setLeaderboard(leaderboard);
    return this;
  }

  public MatchmakerQueueBeanBuilder id(Integer id) {
    matchmakerQueueBean.setId(id);
    return this;
  }

  public MatchmakerQueueBeanBuilder createTime(OffsetDateTime createTime) {
    matchmakerQueueBean.setCreateTime(createTime);
    return this;
  }

  public MatchmakerQueueBeanBuilder updateTime(OffsetDateTime updateTime) {
    matchmakerQueueBean.setUpdateTime(updateTime);
    return this;
  }

  public MatchmakerQueueBean get() {
    return matchmakerQueueBean;
  }

}

