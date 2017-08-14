package com.faforever.client.remote.domain;

public class JoinGameMessage extends ClientMessage {

  private Integer uid;
  private String password;

  public JoinGameMessage(int uid, String password) {
    super(ClientMessageType.JOIN_GAME);
    this.setUid(uid);
    this.setPassword(password);
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
}
