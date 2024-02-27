package com.faforever.client.domain;

/**
 * Represents a leaderboard rating
 */
public record LeaderboardRatingBean(double deviation, double mean, int numberOfGames) {}
