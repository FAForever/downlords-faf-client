package com.faforever.client.domain.api;

public record LeaderboardEntry(
    Integer id, double rating, int gamesPlayed, Leaderboard leaderboard
) {}
