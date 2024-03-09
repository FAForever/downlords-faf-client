package com.faforever.client.player;

/**
 * Represents a leaderboard rating
 */
public record LeaderboardRating(double deviation, double mean, int numberOfGames) {}
