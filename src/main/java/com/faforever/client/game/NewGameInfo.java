package com.faforever.client.game;

public class NewGameInfo {

  private String title;
  private String password;
  private String mod;
  private String map;
  private int version;

  public NewGameInfo() {
  }

  public NewGameInfo(String title, String password, String mod, String map, int version) {
    this.setTitle(title);
    this.setPassword(password);
    this.setMod(mod);
    this.setMap(map);
    this.setVersion(version);
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

  public String getMod() {
    return mod;
  }

  public void setMod(String mod) {
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
}
