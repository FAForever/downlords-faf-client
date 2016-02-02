package com.faforever.client.legacy.domain;

public class InitSessionMessage extends ClientMessage {

  private String version;
  private String userAgent;

  public InitSessionMessage(String version) {
    super(ClientMessageType.ASK_SESSION);
    this.version = version;
    this.userAgent = "downlords-faf-client";
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
