package com.faforever.client.main.event;

import lombok.Getter;

public class NavigateEvent {
  @Getter
  private final NavigationItem item;

  public NavigateEvent(NavigationItem item) {
    this.item = item;
  }
}
