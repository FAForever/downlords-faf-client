package com.faforever.client.chat.event;

import com.faforever.client.chat.ChatMessage;
import lombok.Data;

@Data
public class UnreadPrivateMessageEvent {
  private final ChatMessage message;
}
