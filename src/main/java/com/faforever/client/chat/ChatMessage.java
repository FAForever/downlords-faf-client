package com.faforever.client.chat;

import lombok.Data;

import java.time.Instant;
import java.util.Objects;

@Data
public class ChatMessage {

  private final String source;
  // TODO change to LocalTime?
  private final Instant time;
  private final String username;
  private final String message;
  private boolean action;

  /**
   * @param source the name of the message source/target - either a channel or an username.
   * @param username the user who sent the message
   */
  public ChatMessage(String source, Instant time, String username, String message) {
    this(source, time, username, message, false);
  }

  public ChatMessage(String source, Instant time, String username, String message, boolean isAction) {
    this.source = source;
    this.time = time;
    this.username = username;
    this.message = message;
    this.action = isAction;
  }

  public boolean isPrivate() {
    return !Objects.toString(source, "").startsWith("#");
  }

}
