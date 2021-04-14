package com.faforever.client.api;

import com.faforever.commons.api.dto.Event;
import com.faforever.commons.api.dto.PlayerEvent;

public class PlayerEventBuilder extends PlayerEvent {

  private final PlayerEvent playerEvent;

  private PlayerEventBuilder() {
    playerEvent = new PlayerEvent();
  }

  public static PlayerEventBuilder create() {
    return new PlayerEventBuilder();
  }

  public PlayerEventBuilder eventId(String eventId) {
    playerEvent.setEvent(new Event().setId(eventId));
    return this;
  }

  public PlayerEventBuilder count(int count) {
    playerEvent.setCurrentCount(count);
    return this;
  }

  public PlayerEvent get() {
    return playerEvent;
  }
}
