package com.faforever.client.domain;

public record LeagueEntryBean(
    Integer id,
    PlayerBean player,
    int gamesPlayed,
    Integer score,
    boolean returningPlayer,
    LeagueSeasonBean leagueSeason,
    SubdivisionBean subdivision,
    Long rank
) {}
