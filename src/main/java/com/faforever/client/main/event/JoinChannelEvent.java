package com.faforever.client.main.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class JoinChannelEvent extends NavigateEvent {
  String channel;

  public JoinChannelEvent(String channel) {
    super(NavigationItem.CHAT);
    this.channel = channel;
  }
}
