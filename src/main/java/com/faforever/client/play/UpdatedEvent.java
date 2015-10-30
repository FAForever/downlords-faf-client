package com.faforever.client.play;

import com.google.api.client.util.Key;

public class UpdatedEvent {

  @Key("event_id")
  private String eventId;
  @Key("count")
  private long count;

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }
}
