package com.faforever.client.chat;

import java.time.Instant;

public record ChatMessage(Instant time, ChatChannelUser sender, String message, boolean action) {

  public ChatMessage(Instant time, ChatChannelUser sender, String message) {
    this(time, sender, message, false);
  }
}
