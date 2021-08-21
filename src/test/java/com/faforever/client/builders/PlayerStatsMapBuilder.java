package com.faforever.client.builders;

import com.faforever.client.domain.GamePlayerStatsBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerStatsMapBuilder {
  private final Map<String, List<GamePlayerStatsBean>> teamPlayerStats = new HashMap<>();

  public static PlayerStatsMapBuilder create() {
    return new PlayerStatsMapBuilder();
  }

  public PlayerStatsMapBuilder defaultValues() {
    GamePlayerStatsBean playerStats1 = GamePlayerStatsBeanBuilder.create().defaultValues().leaderboardRatingJournals(List.of(LeaderboardRatingJournalBeanBuilder.create().defaultValues().get())).get();
    appendToTeam("2", playerStats1);
    return this;
  }

  public PlayerStatsMapBuilder appendToTeam(String teamId, GamePlayerStatsBean playerStats) {
    if (!teamPlayerStats.containsKey(teamId)) {
      teamPlayerStats.put(teamId, new ArrayList<>());
    }
    teamPlayerStats.get(teamId).add(playerStats);
    return this;
  }

  public PlayerStatsMapBuilder replace(Map<String, List<GamePlayerStatsBean>> teamPlayerStats) {
    this.teamPlayerStats.clear();
    this.teamPlayerStats.putAll(teamPlayerStats);
    return this;
  }

  public Map<String, List<GamePlayerStatsBean>> get() {
    return teamPlayerStats;
  }
}
