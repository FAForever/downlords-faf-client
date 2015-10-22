package com.faforever.client.play;

public class EventUpdate {

  private final String eventId;
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
