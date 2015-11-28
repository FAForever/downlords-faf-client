package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

public class LoginClientMessage extends ClientMessage {

  private String login;
  private String password;
  private long session;
  private String uniqueId;
  private String localIp;
  private String version;
  private String userAgent;

  public LoginClientMessage(String username, String password, long session, String uniqueId, String localIp, String version) {
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

  public long getSession() {
    return session;
  }

  public void setSession(long session) {
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

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}
