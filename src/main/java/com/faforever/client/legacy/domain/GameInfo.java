package com.faforever.client.legacy.domain;

import java.util.List;
import java.util.Map;

public class GameInfo extends ServerObject {

  public String host;
  public Integer uid;
  public String title;
  public GameAccess access;
  public String mapname;
  public GameState state;
  public Boolean[] options;
  // FAF calls this "game_type" but it's actually the victory condition.
  public VictoryCondition gameType;
  public String featuredMod;
  public Integer maxPlayers;
  public Integer numPlayers;
  public Map<String, String> simMods;
  public Map<String, List<String>> teams;
  public Map<String, Integer> featuredModVersions;

  @Override
  public String toString() {
    return "GameInfo{" +
        "uid=" + uid +
        ", title='" + title + '\'' +
        ", state=" + state +
        '}';
  }
}
