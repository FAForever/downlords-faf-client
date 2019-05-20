package com.faforever.client.main.event;

import lombok.Data;

@Data
public class JoinChannelEvent extends NavigateEvent {
  private final String channel;

  public JoinChannelEvent(String channel) {
    super(NavigationItem.CHAT);
    this.channel = channel;
  }
}
