package com.faforever.client.domain;

import com.faforever.commons.api.dto.Faction;

import java.time.OffsetDateTime;
import java.util.List;

public record GamePlayerStatsBean(
    PlayerBean player,
    int score,
    int team,
    Faction faction,
    OffsetDateTime scoreTime,
    ReplayBean game,
    List<LeaderboardRatingJournalBean> leaderboardRatingJournals
) {

  public GamePlayerStatsBean {
    leaderboardRatingJournals = leaderboardRatingJournals == null ? List.of() : List.copyOf(leaderboardRatingJournals);
  }
}
