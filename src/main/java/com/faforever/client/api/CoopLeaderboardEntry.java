package com.faforever.client.api;

import com.google.api.client.util.Key;

public class CoopLeaderboardEntry {
  @Key
  private String id;
  @Key
  private int duration;
  @Key("player_names")
  private String playerNames;
  @Key("secondary_objectives")
  private boolean secondaryObjectives;
  @Key
  private int ranking;
  @Key("player_count")
  private int playerCount;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public String getPlayerNames() {
    return playerNames;
  }

  public void setPlayerNames(String playerNames) {
    this.playerNames = playerNames;
  }

  public boolean isSecondaryObjectives() {
    return secondaryObjectives;
  }

  public void setSecondaryObjectives(boolean secondaryObjectives) {
    this.secondaryObjectives = secondaryObjectives;
  }

  public int getRanking() {
    return ranking;
  }

  public void setRanking(int ranking) {
    this.ranking = ranking;
  }

  public int getPlayerCount() {
    return playerCount;
  }

  public void setPlayerCount(int playerCount) {
    this.playerCount = playerCount;
  }
}
