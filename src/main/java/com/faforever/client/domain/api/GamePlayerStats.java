package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.commons.api.dto.Faction;

import java.time.OffsetDateTime;
import java.util.List;

public record GamePlayerStats(
    PlayerInfo player, byte score, byte team,
    Faction faction,
    OffsetDateTime scoreTime, Replay game, List<LeaderboardRatingJournal> leaderboardRatingJournals
) {

  public GamePlayerStats {
    leaderboardRatingJournals = leaderboardRatingJournals == null ? List.of() : List.copyOf(leaderboardRatingJournals);
  }
}
