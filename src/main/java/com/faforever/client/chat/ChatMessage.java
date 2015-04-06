package com.faforever.client.chat;

import java.time.Instant;

public class ChatMessage {

  private final Instant time;
  private final String login;
  private final String message;

  public ChatMessage(Instant time, String login, String message) {
    this.time = time;
    this.login = login;
    this.message = message;
  }

  public Instant getTime() {
    return time;
  }

  public String getLogin() {
    return login;
  }

  public String getMessage() {
    return message;
  }
}
