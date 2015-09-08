package com.faforever.client.legacy.domain;

public class JoinGameMessage extends ClientMessage {

  private Integer uid;
  private String password;
  private Integer gameport;

  public JoinGameMessage(int uid, int port, String password) {
    super(ClientMessageType.JOIN_GAME);
    this.setUid(uid);
    this.setPassword(password);
    this.setGameport(port);
  }

  public Integer getUid() {
    return uid;
  }

  public void setUid(Integer uid) {
    this.uid = uid;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Integer getGameport() {
    return gameport;
  }

  public void setGameport(Integer gameport) {
    this.gameport = gameport;
  }
}
