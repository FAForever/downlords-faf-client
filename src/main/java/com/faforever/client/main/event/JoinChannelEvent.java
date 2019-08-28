package com.faforever.client.main.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JoinChannelEvent extends NavigateEvent {
  private final String channel;

  public JoinChannelEvent(String channel) {
    super(NavigationItem.CHAT);
    this.channel = channel;
  }
}
