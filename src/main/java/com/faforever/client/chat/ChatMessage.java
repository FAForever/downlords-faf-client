package com.faforever.client.chat;

import java.time.Instant;

public class ChatMessage {

  private final Instant time;
  private final String username;
  private final String message;
  private boolean action;

  public ChatMessage(Instant time, String username, String message) {
    this(time, username, message, false);
  }

  public ChatMessage(Instant time, String username, String message, boolean isAction) {
    this.time = time;
    this.username = username;
    this.message = message;
    this.action = isAction;
  }

  public Instant getTime() {
    return time;
  }

  public String getUsername() {
    return username;
  }

  public String getMessage() {
    return message;
  }

  public boolean isAction() {
    return action;
  }
}
