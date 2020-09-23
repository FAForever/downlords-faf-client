package com.faforever.client.leaderboard;

import com.faforever.client.api.dto.DivisionName;
import lombok.Data;

@Data
public class Division {
  private final int leagueSeasonId;
  private final int majorDivisionIndex;
  private final int subDivisionIndex;
  private final DivisionName majorDivisionName;
  private final DivisionName subDivisionName;
  private final int highestScore;
}
