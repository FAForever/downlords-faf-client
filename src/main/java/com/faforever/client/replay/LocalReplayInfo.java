package com.faforever.client.replay;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.VictoryCondition;

import java.util.List;
import java.util.Map;

/**
 * This class is meant to be serialized/deserialized from/to JSON.
 */
public class LocalReplayInfo {

  private String host;
  private Integer uid;
  private String title;
  private String mapname;
  private GameState state;
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
  private double gameTime;

  public void updateFromGameInfoBean(GameInfoBean gameInfoBean) {
    host = gameInfoBean.getHost();
    uid = gameInfoBean.getUid();
    title = gameInfoBean.getTitle();
    mapname = gameInfoBean.getMapTechnicalName();
    state = gameInfoBean.getStatus();
    gameType = gameInfoBean.getVictoryCondition();
    featuredMod = gameInfoBean.getFeaturedMod();
    maxPlayers = gameInfoBean.getMaxPlayers();
    numPlayers = gameInfoBean.getNumPlayers();
    simMods = gameInfoBean.getSimMods();
    teams = gameInfoBean.getTeams();
    featuredModVersions = gameInfoBean.getFeaturedModVersions();
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public Integer getUid() {
    return uid;
  }

  public void setUid(Integer uid) {
    this.uid = uid;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMapname() {
    return mapname;
  }

  public void setMapname(String mapname) {
    this.mapname = mapname;
  }

  public GameState getState() {
    return state;
  }

  public void setState(GameState state) {
    this.state = state;
  }

  public Boolean[] getOptions() {
    return options;
  }

  public void setOptions(Boolean[] options) {
    this.options = options;
  }

  public VictoryCondition getGameType() {
    return gameType;
  }

  public void setGameType(VictoryCondition gameType) {
    this.gameType = gameType;
  }

  public String getFeaturedMod() {
    return featuredMod;
  }

  public void setFeaturedMod(String featuredMod) {
    this.featuredMod = featuredMod;
  }

  public Integer getMaxPlayers() {
    return maxPlayers;
  }

  public void setMaxPlayers(Integer maxPlayers) {
    this.maxPlayers = maxPlayers;
  }

  public Integer getNumPlayers() {
    return numPlayers;
  }

  public void setNumPlayers(Integer numPlayers) {
    this.numPlayers = numPlayers;
  }

  public Map<String, String> getSimMods() {
    return simMods;
  }

  public void setSimMods(Map<String, String> simMods) {
    this.simMods = simMods;
  }

  public Map<String, List<String>> getTeams() {
    return teams;
  }

  public void setTeams(Map<String, List<String>> teams) {
    this.teams = teams;
  }

  public Map<String, Integer> getFeaturedModVersions() {
    return featuredModVersions;
  }

  public void setFeaturedModVersions(Map<String, Integer> featuredModVersions) {
    this.featuredModVersions = featuredModVersions;
  }

  public boolean isComplete() {
    return complete;
  }

  public void setComplete(boolean complete) {
    this.complete = complete;
  }

  public String getRecorder() {
    return recorder;
  }

  public void setRecorder(String recorder) {
    this.recorder = recorder;
  }

  public Map<String, String> getVersionInfo() {
    return versionInfo;
  }

  public void setVersionInfo(Map<String, String> versionInfo) {
    this.versionInfo = versionInfo;
  }

  public double getGameEnd() {
    return gameEnd;
  }

  public void setGameEnd(double gameEnd) {
    this.gameEnd = gameEnd;
  }

  public double getGameTime() {
    return gameTime;
  }

  public void setGameTime(double gameTime) {
    this.gameTime = gameTime;
  }
}
