package com.faforever.client.legacy.domain;

public class SessionInfo extends ServerMessage {

  private String session;

  private String email;

  private int id;

  /**
   * The session ID sent by the server. Yes, it's a number as a string.
   */
  public String getSession() {
    return session;
  }

  public void setSession(String session) {
    this.session = session;
  }

  /**
   * Only set on successful log-in. No clue why it is needed.
   */
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * UID assigned by the server.
   */
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
}
