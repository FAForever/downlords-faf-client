package com.faforever.client.teammatchmaking;

import com.faforever.client.leaderboard.Leaderboard;
import com.faforever.client.leaderboard.LeaderboardBuilder;
import com.faforever.client.teammatchmaking.MatchmakingQueue.MatchingStatus;

import java.time.Instant;

public class MatchmakingQueueBuilder {

  private final MatchmakingQueue matchmakingQueue = new MatchmakingQueue();

  public static MatchmakingQueueBuilder create() {
    return new MatchmakingQueueBuilder();
  }

  public MatchmakingQueueBuilder defaultValues() {
    queueId(1);
    technicalName("test_queue");
    queuePopTime(Instant.MAX);
    teamSize(1);
    partiesInQueue(0);
    playersInQueue(0);
    joined(false);
    matchingStatus(null);
    leaderboard(LeaderboardBuilder.create().defaultValues().get());
    return this;
  }

  public MatchmakingQueueBuilder queueId(int queueId) {
    matchmakingQueue.setQueueId(queueId);
    return this;
  }

  public MatchmakingQueueBuilder technicalName(String technicalName) {
    matchmakingQueue.setTechnicalName(technicalName);
    return this;
  }

  public MatchmakingQueueBuilder queuePopTime(Instant queuePopTime) {
    matchmakingQueue.setQueuePopTime(queuePopTime);
    return this;
  }

  public MatchmakingQueueBuilder teamSize(int teamSize) {
    matchmakingQueue.setTeamSize(teamSize);
    return this;
  }

  public MatchmakingQueueBuilder partiesInQueue(int partiesInQueue) {
    matchmakingQueue.setPartiesInQueue(partiesInQueue);
    return this;
  }

  public MatchmakingQueueBuilder playersInQueue(int playersInQueue) {
    matchmakingQueue.setPlayersInQueue(playersInQueue);
    return this;
  }

  public MatchmakingQueueBuilder joined(boolean joined) {
    matchmakingQueue.setJoined(joined);
    return this;
  }

  public MatchmakingQueueBuilder matchingStatus(MatchingStatus matchingStatus) {
    matchmakingQueue.setMatchingStatus(matchingStatus);
    return this;
  }

  public MatchmakingQueueBuilder leaderboard(Leaderboard leaderboard) {
    matchmakingQueue.setLeaderboard(leaderboard);
    return this;
  }

  public MatchmakingQueue get() {
    return matchmakingQueue;
  }
}
