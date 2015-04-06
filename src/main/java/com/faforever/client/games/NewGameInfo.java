package com.faforever.client.games;

public class NewGameInfo {

  private String title;
  private String password;
  private String mod;
  private String map;

  public NewGameInfo(String title, String password, String mod, String map) {
    this.title = title;
    this.password = password;
    this.mod = mod;
    this.map = map;
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
}
