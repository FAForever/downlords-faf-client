package com.faforever.client.chat;

public class InitiatePrivateChatEvent {
  private final String username;

  public InitiatePrivateChatEvent(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }
}
