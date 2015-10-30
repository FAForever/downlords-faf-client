package com.faforever.client.play;

import com.google.api.client.util.Key;

import java.util.List;

public class EventUpdatesResponse {

  @Key("updated_events")
  private List<UpdatedEvent> updatedEvents;

  public List<UpdatedEvent> getUpdatedEvents() {
    return updatedEvents;
  }

  public void setUpdatedEvents(List<UpdatedEvent> updatedEvents) {
    this.updatedEvents = updatedEvents;
  }
}
