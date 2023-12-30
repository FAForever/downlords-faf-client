package com.faforever.client.chat;

import java.time.Instant;

public record ChatMessage(Instant time, ChatChannelUser sender, String message, String id, boolean action) {

  public ChatMessage(Instant time, ChatChannelUser sender, String message, String id) {
    this(time, sender, message, id, false);
  }
}
