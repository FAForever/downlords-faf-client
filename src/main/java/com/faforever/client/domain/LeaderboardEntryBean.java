package com.faforever.client.domain;

public record LeaderboardEntryBean(
    Integer id, double rating, int gamesPlayed, LeaderboardBean leaderboard
) {}
