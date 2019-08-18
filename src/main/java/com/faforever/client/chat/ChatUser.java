package com.faforever.client.chat;

import lombok.Data;

@Data
public class ChatUser {
  
  private final String username;
  private final boolean isModerator;

  public ChatUser(String username, boolean isModerator) {
    this.username = username;
    this.isModerator = isModerator;
  }
}
