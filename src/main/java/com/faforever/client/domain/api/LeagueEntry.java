package com.faforever.client.domain.api;

import com.faforever.client.domain.server.PlayerInfo;

public record LeagueEntry(
    Integer id, PlayerInfo player,
    int gamesPlayed,
    Integer score,
    boolean returningPlayer, LeagueSeason leagueSeason, Subdivision subdivision,
    Long rank
) {}
