package com.faforever.client.game;

public class NewGameInfo {

  private String title;
  private String password;
  private String mod;
  private String map;
  private int version;

  public NewGameInfo(String title, String password, String mod, String map, int version) {
    this.title = title;
    this.password = password;
    this.mod = mod;
    this.map = map;
    this.version = version;
  }

  public String getTitle() {
    return title;
  }

  public String getPassword() {
    return password;
  }

  public String getMod() {
    return mod;
  }

  public String getMap() {
    return map;
  }

  public int getVersion() {
    return version;
  }
}
