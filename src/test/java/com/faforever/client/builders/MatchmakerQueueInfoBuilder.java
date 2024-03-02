package com.faforever.client.builders;

import com.faforever.client.domain.api.Leaderboard;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.teammatchmaking.MatchingStatus;
import org.instancio.Instancio;

import java.time.OffsetDateTime;


public class MatchmakerQueueInfoBuilder {
  public static MatchmakerQueueInfoBuilder create() {
    return new MatchmakerQueueInfoBuilder();
  }

  private final MatchmakerQueueInfo matchmakerQueueInfo = new MatchmakerQueueInfo();

  public MatchmakerQueueInfoBuilder defaultValues() {
    technicalName("test");
    queuePopTime(OffsetDateTime.now());
    teamSize(1);
    playersInQueue(2);
    activeGames(0);
    joined(false);
    matchingStatus(null);
    leaderboard(Instancio.create(Leaderboard.class));
    id(0);
    return this;
  }

  public MatchmakerQueueInfoBuilder technicalName(String technicalName) {
    matchmakerQueueInfo.setTechnicalName(technicalName);
    return this;
  }

  public MatchmakerQueueInfoBuilder queuePopTime(OffsetDateTime queuePopTime) {
    matchmakerQueueInfo.setQueuePopTime(queuePopTime);
    return this;
  }

  public MatchmakerQueueInfoBuilder teamSize(int teamSize) {
    matchmakerQueueInfo.setTeamSize(teamSize);
    return this;
  }

  public MatchmakerQueueInfoBuilder playersInQueue(int playersInQueue) {
    matchmakerQueueInfo.setPlayersInQueue(playersInQueue);
    return this;
  }

  public MatchmakerQueueInfoBuilder activeGames(int activeGames) {
    matchmakerQueueInfo.setActiveGames(activeGames);
    return this;
  }

  public MatchmakerQueueInfoBuilder joined(boolean joined) {
    matchmakerQueueInfo.setSelected(joined);
    return this;
  }

  public MatchmakerQueueInfoBuilder matchingStatus(MatchingStatus matchingStatus) {
    matchmakerQueueInfo.setMatchingStatus(matchingStatus);
    return this;
  }

  public MatchmakerQueueInfoBuilder leaderboard(Leaderboard leaderboard) {
    matchmakerQueueInfo.setLeaderboard(leaderboard);
    return this;
  }

  public MatchmakerQueueInfoBuilder id(Integer id) {
    matchmakerQueueInfo.setId(id);
    return this;
  }

  public MatchmakerQueueInfo get() {
    return matchmakerQueueInfo;
  }

}

