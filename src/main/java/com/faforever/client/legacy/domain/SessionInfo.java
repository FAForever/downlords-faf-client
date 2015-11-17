package com.faforever.client.legacy.domain;

public class SessionInfo extends ServerMessage {

  private long session;

  public SessionInfo() {
    super(ServerMessageType.SESSION);
  }

  public long getSession() {
    return session;
  }

  public void setSession(long session) {
    this.session = session;
  }
}
