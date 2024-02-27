package com.faforever.client.domain;


import java.time.OffsetDateTime;

public record LeagueSeasonBean(
    Integer id,
    LeagueBean league,
    LeagueLeaderboardBean leagueLeaderboard,
    String nameKey,
    int seasonNumber,
    int placementGames,
    int placementGamesReturningPlayer,
    OffsetDateTime startDate,
    OffsetDateTime endDate
) {}
