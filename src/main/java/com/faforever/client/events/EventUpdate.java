package com.faforever.client.events;

import com.google.api.client.util.Key;

public class EventUpdate {

  @Key("event_id")
  private final String eventId;
  @Key("update_count")
  private final long updateCount;

  public EventUpdate(String eventId, long updateCount) {
    this.eventId = eventId;
    this.updateCount = updateCount;
  }

  public String getEventId() {
    return eventId;
  }

  public long getCount() {
    return updateCount;
  }
}
