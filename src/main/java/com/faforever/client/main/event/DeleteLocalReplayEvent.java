package com.faforever.client.main.event;

import javafx.event.Event;
import javafx.event.EventType;

public class DeleteLocalReplayEvent extends Event {
  public static final EventType<DeleteLocalReplayEvent> DELETE_LOCAL_REPLAY_EVENT_TYPE = new EventType<>(ANY);

  public DeleteLocalReplayEvent() {
    super(DELETE_LOCAL_REPLAY_EVENT_TYPE);
  }
}
