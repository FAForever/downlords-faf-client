package com.faforever.client.game;

public class NewGameInfo {

  public String title;
  public String password;
  public String mod;
  public String map;
  public int version;

  public NewGameInfo() {
  }

  public NewGameInfo(String title, String password, String mod, String map, int version) {
    this.title = title;
    this.password = password;
    this.mod = mod;
    this.map = map;
    this.version = version;
  }
}
