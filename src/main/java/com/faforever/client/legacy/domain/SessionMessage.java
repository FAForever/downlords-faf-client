package com.faforever.client.legacy.domain;

public class SessionMessage extends FafServerMessage {

  private long session;

  public SessionMessage() {
    super(FafServerMessageType.SESSION);
  }

  public long getSession() {
    return session;
  }

  public void setSession(long session) {
    this.session = session;
  }
}
