package com.faforever.client.chat;

import java.time.Instant;

public record ChatMessage(String source, Instant time, String username, String message, boolean action) {

  public ChatMessage(String source, Instant time, String username, String message) {
    this(source, time, username, message, false);
  }

  public boolean isPrivate() {
    return !source.startsWith("#");
  }

}
