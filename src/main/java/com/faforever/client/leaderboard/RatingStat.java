package com.faforever.client.leaderboard;

import lombok.Data;

@Data
public class RatingStat {
  private final int rating;
  private final int totalCount;
  private final int countWithEnoughGamesPlayed;
}
