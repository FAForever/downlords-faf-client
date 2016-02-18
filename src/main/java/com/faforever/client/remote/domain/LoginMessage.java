package com.faforever.client.remote.domain;

public class LoginMessage extends FafServerMessage {

  private int id;
  private String login;

  public LoginMessage() {
    super(FafServerMessageType.WELCOME);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }
}
