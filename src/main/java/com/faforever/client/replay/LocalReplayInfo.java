package com.faforever.client.replay;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.legacy.domain.GameAccess;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.VictoryCondition;
import javafx.collections.ObservableList;

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

  public void updateFromGameInfoBean(GameInfoBean gameInfoBean) {
    host = gameInfoBean.getHost();
    uid = gameInfoBean.getUid();
    title = gameInfoBean.getTitle();
    access = gameInfoBean.getAccess();
    mapname = gameInfoBean.getMapName();
    state = gameInfoBean.getStatus();
    gameType = gameInfoBean.getGameType();
    featuredMod = gameInfoBean.getFeaturedMod();
    maxPlayers = gameInfoBean.getMaxPlayers();
    numPlayers = gameInfoBean.getNumPlayers();
    simMods = gameInfoBean.getSimMods();
    teams = gameInfoBean.getTeams();
    featuredModVersions = gameInfoBean.getFeaturedModVersions();

    ObservableList<Boolean> options = gameInfoBean.getOptions();
    this.options = options.toArray(new Boolean[options.size()]);
  }
}
