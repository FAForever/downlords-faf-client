package com.faforever.client.domain.api;

/**
 * Represents a leaderboard rating
 */
public record LeaderboardRating(double deviation, double mean, int numberOfGames) {}
