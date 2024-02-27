package com.faforever.client.builders;

import com.faforever.client.domain.GamePlayerStatsBean;
import org.instancio.Instancio;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.instancio.Select.field;

public class PlayerStatsMapBuilder {
  private final Map<String, List<GamePlayerStatsBean>> teamPlayerStats = new HashMap<>();

  public static PlayerStatsMapBuilder create() {
    return new PlayerStatsMapBuilder();
  }

  public PlayerStatsMapBuilder defaultValues() {
    GamePlayerStatsBean playerStats1 = Instancio.of(GamePlayerStatsBean.class)
                                                .set(field(GamePlayerStatsBean::leaderboardRatingJournals), List.of(
                                                    LeaderboardRatingJournalBeanBuilder.create().defaultValues().get()))
                                                .create();
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
