package com.faforever.client.legacy.domain;

public class ClientMessage {

  public String action;
  public String avatar;
  public Integer version;
  public String command;
  public String login;
  public String password;
  public String uniqueId;
  public String localIp;
  public String session;
  public GameAccess access;
  public String mapname;
  public String title;
  public String state;
  public String league;
  public String division;
  public Integer gameport;
  public boolean[] options;
  public String mod;
  public Integer uid;
  private String type;
  private String player;

  public static ClientMessage askPlayerStats(String username) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "stats";
    clientMessage.type = "global_90_days";
    clientMessage.player = username;
    return clientMessage;
  }

  public static ClientMessage askLeagueTable(String league) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "stats";
    clientMessage.type = "league_table";
    clientMessage.league = league;
    return clientMessage;
  }

  public static ClientMessage gameTerminated() {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "fa_state";
    clientMessage.state = "off";
    return clientMessage;
  }

  public static ClientMessage gameStarted() {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "fa_state";
    clientMessage.state = "on";
    return clientMessage;
  }

  public static ClientMessage selectAvatar(String avatar) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.action = "select";
    clientMessage.command = "avatar";
    clientMessage.avatar = avatar;
    return clientMessage;
  }

  public static ClientMessage listAvatar() {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.action = "list_avatar";
    clientMessage.command = "avatar";
    return clientMessage;
  }

  public static ClientMessage login(String username, String password, String session, String uniqueId, String localIp, int version) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "hello";
    clientMessage.login = username;
    clientMessage.password = password;
    clientMessage.session = session;
    clientMessage.uniqueId = uniqueId;
    clientMessage.localIp = localIp;
    clientMessage.version = version;
    return clientMessage;
  }

  public static ClientMessage hostGame(GameAccess gameAccess, String mapName, String title, int port, boolean[] options, String mod, String password, int version) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "game_host";
    clientMessage.access = gameAccess;
    clientMessage.password = password;
    clientMessage.version = version;
    clientMessage.mod = mod;
    clientMessage.title = title;
    clientMessage.mapname = mapName;
    clientMessage.gameport = port;
    clientMessage.options = options;

    return clientMessage;
  }

  public static ClientMessage joinGame(int uid, int port, String password) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "game_join";
    clientMessage.uid = uid;
    clientMessage.password = password;
    clientMessage.gameport = port;

    return clientMessage;
  }

  public static ClientMessage askSession(String username) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "ask_session";
    return clientMessage;
  }
}
