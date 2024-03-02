package com.faforever.client.domain.api;

/**
 * Represents a leaderboard rating
 */
public record LeaderboardRatingJournal(
    Integer id,
    Double meanAfter,
    Double deviationAfter,
    Double meanBefore,
    Double deviationBefore, GamePlayerStats gamePlayerStats, Leaderboard leaderboard
) {}
