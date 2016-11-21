package com.faforever.client.remote.domain;

public class InitSessionMessage extends ClientMessage {

  private String version;

  public InitSessionMessage(String version) {
    super(ClientMessageType.ASK_SESSION);
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
