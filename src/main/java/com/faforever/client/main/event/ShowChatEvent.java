package com.faforever.client.main.event;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class ShowChatEvent extends OpenChatViewEvent {
  int playerId;
}
