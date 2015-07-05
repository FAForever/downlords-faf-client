package com.faforever.client.replay;

import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.VictoryCondition;

import java.util.List;
import java.util.Map;

/**
 * This class is meant to be serialized/deserialized from/to JSON.
 */
public class LocalReplayInfo {

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
  public boolean complete;
  public String recorder;
  public Map<String, String> versionInfo;
  public double gameEnd;
  public double gameTime;

  public void updateFromGameInfo(GameInfo gameInfo) {
    host = gameInfo.host;
    uid = gameInfo.uid;
    title = gameInfo.title;
    access = gameInfo.access;
    mapname = gameInfo.mapname;
    state = gameInfo.state;
    options = gameInfo.options;
    gameType = gameInfo.gameType;
    featuredMod = gameInfo.featuredMod;
    maxPlayers = gameInfo.maxPlayers;
    numPlayers = gameInfo.numPlayers;
    simMods = gameInfo.simMods;
    teams = gameInfo.teams;
    featuredModVersions = gameInfo.featuredModVersions;
  }
}
