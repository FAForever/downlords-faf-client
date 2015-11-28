package com.faforever.client.api;

import com.google.api.client.util.Key;

public class PlayerEvent {

  @Key
  private String id;
  @Key("event_id")
  private String eventId;
  @Key("count")
  private int count;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }
}
