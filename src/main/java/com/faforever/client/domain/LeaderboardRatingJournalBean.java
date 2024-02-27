package com.faforever.client.domain;

/**
 * Represents a leaderboard rating
 */
public record LeaderboardRatingJournalBean(
    Integer id,
    Double meanAfter,
    Double deviationAfter,
    Double meanBefore,
    Double deviationBefore,
    GamePlayerStatsBean gamePlayerStats,
    LeaderboardBean leaderboard
) {}
