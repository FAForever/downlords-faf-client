package com.faforever.client.chat;

import java.time.Instant;

public record ChatMessage(Instant time, String username, String message, boolean action) {

  public ChatMessage(Instant time, String username, String message) {
    this(time, username, message, false);
  }
}
