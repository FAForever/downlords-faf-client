package com.faforever.client.legacy.domain;

import com.faforever.client.game.GameVisibility;

public class HostGameMessage extends ClientMessage {

  private int gameport;
  private String mapname;
  private String title;
  private String mod;
  private boolean[] options;
  private GameAccess access;
  private Integer version;
  private String password;
  private GameVisibility visibility;

  public HostGameMessage(GameAccess gameAccess, String mapName, String title, int port, boolean[] options, String mod, String password, Integer version) {
    super(ClientMessageType.HOST_GAME);
    this.setAccess(gameAccess);
    this.setPassword(password);
    this.setVersion(version);
    this.setMod(mod);
    this.setTitle(title);
    this.setMapname(mapName);
    this.setGameport(port);
    this.setOptions(options);
  }

  public int getGameport() {
    return gameport;
  }

  public void setGameport(int gameport) {
    this.gameport = gameport;
  }

  public String getMapname() {
    return mapname;
  }

  public void setMapname(String mapname) {
    this.mapname = mapname;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMod() {
    return mod;
  }

  public void setMod(String mod) {
    this.mod = mod;
  }

  public boolean[] getOptions() {
    return options;
  }

  public void setOptions(boolean[] options) {
    this.options = options;
  }

  public GameAccess getAccess() {
    return access;
  }

  public void setAccess(GameAccess access) {
    this.access = access;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public GameVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(GameVisibility visibility) {
    this.visibility = visibility;
  }
}
