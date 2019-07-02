package com.faforever.client.chat.event;

import com.faforever.client.chat.Channel;
import lombok.Value;

@Value
public class ChannelTopicChangedEvent {
  private Channel channel;
}
