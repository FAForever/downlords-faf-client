package com.faforever.client.replay;

import com.faforever.client.legacy.domain.GameInfo;

import java.util.Map;

/**
 * This class is meant to be serialized/deserialized from/to JSON.
 */
public class ReplayInfo extends GameInfo {

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
