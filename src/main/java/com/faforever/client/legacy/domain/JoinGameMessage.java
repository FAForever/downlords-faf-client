package com.faforever.client.legacy.domain;

public class JoinGameMessage extends ClientMessage {

  public Integer uid;
  public String password;
  public Integer gameport;

  public JoinGameMessage(int uid, int port, String password) {
    this.command = "game_join";
    this.uid = uid;
    this.password = password;
    this.gameport = port;
  }
}
