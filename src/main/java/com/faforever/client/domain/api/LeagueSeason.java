package com.faforever.client.domain.api;


import java.time.OffsetDateTime;

public record LeagueSeason(
    Integer id, League league, LeagueLeaderboard leagueLeaderboard,
    String nameKey,
    int seasonNumber,
    int placementGames,
    int placementGamesReturningPlayer,
    OffsetDateTime startDate,
    OffsetDateTime endDate
) {}
