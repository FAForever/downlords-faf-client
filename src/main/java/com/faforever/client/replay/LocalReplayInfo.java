package com.faforever.client.replay;

import com.faforever.client.game.Game;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.VictoryCondition;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * This class is meant to be serialized/deserialized from/to JSON.
 */
@Data
public class LocalReplayInfo {
  private CompressionType compression;
  private String host;
  private Integer uid;
  private String title;
  private String mapname;
  private GameStatus state;
  //TODO what is this? not used by client.
  private Boolean[] options;
  // FAF calls this "game_type" but it's actually the victory condition.
  private VictoryCondition gameType;
  private String featuredMod;
  private Integer maxPlayers;
  private Integer numPlayers;
  private Map<String, String> simMods;
  private Map<String, List<String>> teams;
  private Map<String, Integer> featuredModVersions;
  private boolean complete;
  private String recorder;
  private Map<String, String> versionInfo;
  private double gameEnd;
  /**
   * Backwards compatibility: If 0.0, then {@code launchedAt} should be available instead.
   */
  private double gameTime;
  /**
   * Backwards compatibility: If 0.0, then {@code gameTime} should be available instead.
   */
  private double launchedAt;

  public void updateFromGameInfoBean(Game game) {
    host = game.getHost();
    uid = game.getId();
    title = game.getTitle();
    mapname = game.getMapFolderName();
    state = game.getStatus();
    gameType = game.getVictoryCondition();
    featuredMod = game.getFeaturedMod();
    maxPlayers = game.getMaxPlayers();
    numPlayers = game.getNumPlayers();
    simMods = game.getSimMods();
    // FIXME this (and all others here) should do a deep copy
    teams = game.getTeams();
    featuredModVersions = game.getFeaturedModVersions();
  }
}
