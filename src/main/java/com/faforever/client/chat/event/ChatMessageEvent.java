package com.faforever.client.chat.event;

import com.faforever.client.chat.ChatMessage;

public class ChatMessageEvent {

  private final ChatMessage message;

  public ChatMessageEvent(ChatMessage message) {
    this.message = message;
  }

  public ChatMessage getMessage() {
    return message;
  }

}
