package com.faforever.client.game;

import java.util.Set;

public class NewGameInfo {

  private String title;
  private String password;
  private String mod;
  private String map;
  private int version;
  private Set<String> simMods;

  public NewGameInfo() {
  }

  public NewGameInfo(String title, String password, String mod, String map, int version, Set<String> simMods) {
    this.title = title;
    this.password = password;
    this.mod = mod;
    this.map = map;
    this.version = version;
    this.simMods = simMods;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getGameType() {
    return mod;
  }

  public void setGameType(String mod) {
    this.mod = mod;
  }

  public String getMap() {
    return map;
  }

  public void setMap(String map) {
    this.map = map;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public String getMod() {
    return mod;
  }

  public void setMod(String mod) {
    this.mod = mod;
  }

  public Set<String> getSimModUidsToVersions() {
    return simMods;
  }

  public void setSimModUidsToVersions(Set<String> simMods) {
    this.simMods = simMods;
  }
}
