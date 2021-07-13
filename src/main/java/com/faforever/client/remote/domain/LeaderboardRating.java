package com.faforever.client.remote.domain;

import lombok.Value;

/**
 * Rating as received from the FAF server in a player message.
 */
@Value
public class LeaderboardRating {
  Integer numberOfGames;
  float[] rating;
}
