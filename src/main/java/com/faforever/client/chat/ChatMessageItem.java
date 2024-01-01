package com.faforever.client.chat;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatMessageItem {

  @EqualsAndHashCode.Include
  private final ChatMessage chatMessage;

  private final BooleanProperty head = new SimpleBooleanProperty();

  public ChatMessageItem(ChatMessage chatMessage) {
    this.chatMessage = chatMessage;
  }

  public boolean isHead() {
    return head.get();
  }

  public BooleanProperty headProperty() {
    return head;
  }

  public void setHead(boolean head) {
    this.head.set(head);
  }

  public ChatMessage getChatMessage() {
    return chatMessage;
  }
}
