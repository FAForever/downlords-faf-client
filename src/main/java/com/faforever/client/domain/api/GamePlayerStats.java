package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.commons.api.dto.Faction;

import java.time.OffsetDateTime;
import java.util.List;

public record GamePlayerStats(
    boolean ai,
    Faction faction,
    byte color,
    byte team,
    byte startSpot,
    byte score,
    OffsetDateTime scoreTime,
    GameOutcome outcome,
    PlayerInfo player,
    List<LeaderboardRatingJournal> leaderboardRatingJournals
) {

  public GamePlayerStats {
    leaderboardRatingJournals = leaderboardRatingJournals == null ? List.of() : List.copyOf(leaderboardRatingJournals);
  }
}
