package com.faforever.client.api;

import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.PlayerBean;

public class Ladder1v1EntryBeanBuilder {

  private final LeaderboardEntryBean leaderboardEntry = new LeaderboardEntryBean();

  public static Ladder1v1EntryBeanBuilder create() {
    return new Ladder1v1EntryBeanBuilder();
  }

  public Ladder1v1EntryBeanBuilder username(PlayerBean player) {
    leaderboardEntry.setPlayer(player);
    return this;
  }

  public Ladder1v1EntryBeanBuilder defaultValues() {
    return this;
  }

  public LeaderboardEntryBean get() {
    return leaderboardEntry;
  }
}
