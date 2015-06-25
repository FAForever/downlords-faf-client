package com.faforever.client.legacy.domain;

public class HostGameMessage extends ClientMessage {

  public int gameport;
  public String mapname;
  public String title;
  public String mod;
  public boolean[] options;
  public GameAccess access;
  public Integer version;
  public String password;

  public HostGameMessage(GameAccess gameAccess, String mapName, String title, int port, boolean[] options, String mod, String password, int version) {
    this.command = "game_host";
    this.access = gameAccess;
    this.password = password;
    this.version = version;
    this.mod = mod;
    this.title = title;
    this.mapname = mapName;
    this.gameport = port;
    this.options = options;
  }
}
