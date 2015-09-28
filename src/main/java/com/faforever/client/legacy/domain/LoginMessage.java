package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

public class LoginMessage extends ClientMessage {

  private String login;
  private String password;
  private String session;
  private String uniqueId;
  private String localIp;
  private Integer version;
  private String userAgent;

  public LoginMessage(String username, String password, String session, String uniqueId, String localIp, int version) {
    super(ClientMessageType.LOGIN);
    this.setLogin(username);
    this.setPassword(password);
    this.setSession(session);
    this.setUniqueId(uniqueId);
    this.setLocalIp(localIp);
    this.setVersion(version);
    setUserAgent("downlords-faf-client");
  }

  @Override
  public Collection<String> getStringsToMask() {
    return Collections.singletonList(getPassword());
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = login;
  }

  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  public String getUniqueId() {
    return uniqueId;
  }

  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
  }

  public String getLocalIp() {
    return localIp;
  }

  public void setLocalIp(String localIp) {
    this.localIp = localIp;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}
