package com.faforever.client.play;

import java.util.ArrayList;
import java.util.Collection;

public class EventUpdatesRequest {

  private Collection<EventUpdate> updates;

  public EventUpdatesRequest() {
    updates = new ArrayList<>();
  }

  public EventUpdatesRequest record(String eventId, long updateCount) {
    updates.add(new EventUpdate(eventId, updateCount));
    return this;
  }

  public Collection<EventUpdate> getUpdates() {
    return updates;
  }
}
