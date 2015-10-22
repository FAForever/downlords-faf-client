package com.faforever.client.play;

import java.util.List;

public class EventUpdatesResponse {

  private List<UpdatedEvent> updatedEvents;

  public List<UpdatedEvent> getUpdatedEvents() {
    return updatedEvents;
  }

  public void setUpdatedEvents(List<UpdatedEvent> updatedEvents) {
    this.updatedEvents = updatedEvents;
  }
}
