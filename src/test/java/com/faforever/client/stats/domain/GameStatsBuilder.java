package com.faforever.client.stats.domain;

import java.util.ArrayList;

public class GameStatsBuilder {

  private final GameStats gameStats;

  private GameStatsBuilder() {
    gameStats = new GameStats();
    gameStats.setArmies(new ArrayList<>());
  }

  public GameStatsBuilder army(Army army) {
    gameStats.getArmies().add(army);
    return this;
  }

  public GameStats get() {
    return gameStats;
  }

  public static GameStatsBuilder create() {
    return new GameStatsBuilder();
  }
}
