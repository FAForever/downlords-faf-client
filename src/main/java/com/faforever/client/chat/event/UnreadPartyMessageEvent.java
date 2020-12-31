package com.faforever.client.chat.event;

import com.faforever.client.chat.ChatMessage;
import lombok.Data;

@Data
public class UnreadPartyMessageEvent {
  private final ChatMessage message;
}