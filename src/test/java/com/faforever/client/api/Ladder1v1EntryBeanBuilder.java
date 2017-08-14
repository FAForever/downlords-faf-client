package com.faforever.client.api;

import com.faforever.client.leaderboard.LeaderboardEntry;

public class Ladder1v1EntryBeanBuilder {

  private LeaderboardEntry leaderboardEntry;

  private Ladder1v1EntryBeanBuilder() {
    leaderboardEntry = new LeaderboardEntry();
  }

  public static Ladder1v1EntryBeanBuilder create() {
    return new Ladder1v1EntryBeanBuilder();
  }

  public Ladder1v1EntryBeanBuilder username(String username) {
    leaderboardEntry.setUsername(username);
    return this;
  }

  public Ladder1v1EntryBeanBuilder defaultValues() {
    return this;
  }

  public LeaderboardEntry get() {
    return leaderboardEntry;
  }
}
