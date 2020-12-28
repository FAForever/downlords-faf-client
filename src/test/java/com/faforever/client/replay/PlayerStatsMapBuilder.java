package com.faforever.client.replay;

import com.faforever.client.game.Faction;
import com.faforever.client.replay.Replay.PlayerStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerStatsMapBuilder {
  private final Map<String, List<PlayerStats>> teamPlayerStats = new HashMap<>();

  public static PlayerStatsMapBuilder create() {
    return new PlayerStatsMapBuilder();
  }

  public PlayerStatsMapBuilder defaultValues() {
    PlayerStats playerStats1 = PlayerStats.builder()
        .beforeDeviation(100)
        .beforeMean(1000)
        .afterDeviation(15.0)
        .afterMean(1100.0)
        .faction(Faction.UEF)
        .playerId(0)
        .score(1)
        .build();
    PlayerStats playerStats2 = PlayerStats.builder()
        .beforeDeviation(100)
        .beforeMean(1000)
        .afterDeviation(15.0)
        .afterMean(900.0)
        .faction(Faction.CYBRAN)
        .playerId(1)
        .score(-1)
        .build();
    appendToTeam("2", playerStats1);
    appendToTeam("3", playerStats2);
    return this;
  }

  public PlayerStatsMapBuilder appendToTeam(String teamId, PlayerStats playerStats) {
    if (!teamPlayerStats.containsKey(teamId)) {
      teamPlayerStats.put(teamId, new ArrayList<>());
    }
    teamPlayerStats.get(teamId).add(playerStats);
    return this;
  }

  public PlayerStatsMapBuilder replace(Map<String, List<PlayerStats>> teamPlayerStats) {
    this.teamPlayerStats.clear();
    this.teamPlayerStats.putAll(teamPlayerStats);
    return this;
  }

  public Map<String, List<PlayerStats>> get() {
    return teamPlayerStats;
  }
}
