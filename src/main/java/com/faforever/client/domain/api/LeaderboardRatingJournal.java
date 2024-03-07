package com.faforever.client.domain.api;

import java.time.OffsetDateTime;

/**
 * Represents a leaderboard rating
 */
public record LeaderboardRatingJournal(
    Integer id,
    Double meanAfter,
    Double deviationAfter,
    Double meanBefore, Double deviationBefore, OffsetDateTime scoreTime, Leaderboard leaderboard
) {}
