package com.faforever.client.legacy.domain;

public class SessionInfo extends ServerObject {

  /**
   * The session ID sent by the server. Yes, it's a number as a string.
   */
  public String session;

  /**
   * Only set on successful log-in. No clue why it is needed.
   */
  public String email;

  /**
   * UID assigned by the server.
   */
  public int id;
}
