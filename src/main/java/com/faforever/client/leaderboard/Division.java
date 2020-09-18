package com.faforever.client.leaderboard;

import lombok.Data;

@Data
public class Division {
  private final int leagueSeasonId;
  private final int majorDivisionIndex;
  private final int subDivisionIndex;
  private final String majorDivisionName;
  private final String subDivisionName;
  private final int highestScore;
}
