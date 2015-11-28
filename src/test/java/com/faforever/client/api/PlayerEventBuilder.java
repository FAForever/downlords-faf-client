package com.faforever.client.api;

public class PlayerEventBuilder extends PlayerEvent {

  private final PlayerEvent playerEvent;

  private PlayerEventBuilder() {
    playerEvent = new PlayerEvent();
  }

  public PlayerEventBuilder eventId(String eventId) {
    playerEvent.setEventId(eventId);
    return this;
  }

  public PlayerEventBuilder count(int count) {
    playerEvent.setCount(count);
    return this;
  }

  public PlayerEvent get() {
    return playerEvent;
  }

  public static PlayerEventBuilder create() {
    return new PlayerEventBuilder();
  }
}
