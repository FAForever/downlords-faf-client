package com.faforever.client.legacy.domain;

public class SessionMessageLobby extends FafServerMessage {

  private long session;

  public SessionMessageLobby() {
    super(FafServerMessageType.SESSION);
  }

  public long getSession() {
    return session;
  }

  public void setSession(long session) {
    this.session = session;
  }
}
