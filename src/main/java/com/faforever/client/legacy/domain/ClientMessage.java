package com.faforever.client.legacy.domain;

public class ClientMessage {

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

  public static ClientMessage hostGame(GameAccess gameAccess, String mapName, String title, int port, boolean[] options, String mod, String password) {
    ClientMessage clientMessage = new ClientMessage();
    clientMessage.command = "game_host";
    clientMessage.access = gameAccess.getString();
    clientMessage.mapname = mapName;
    clientMessage.title = title;
    clientMessage.gameport = port;
    clientMessage.mod = mod;
    clientMessage.password = password;
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

  public String action;
  public String avatar;
  public Integer version;
  public String command;
  public String login;
  public String password;
  public String uniqueId;
  public String localIp;
  public String session;
  public String access;
  public String mapname;
  public String title;
  public String state;
  public Integer gameport;
  public boolean[] options;
  public String mod;
  public int uid;
}
