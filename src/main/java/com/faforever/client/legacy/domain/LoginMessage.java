package com.faforever.client.legacy.domain;

public class LoginMessage extends ClientMessage {

  public String login;
  public String password;
  public String session;
  public String uniqueId;
  public String localIp;
  public Integer version;

  public LoginMessage(String username, String password, String session, String uniqueId, String localIp, int version) {
    this.command = "hello";
    this.login = username;
    this.password = password;
    this.session = session;
    this.uniqueId = uniqueId;
    this.localIp = localIp;
    this.version = version;
  }
}
