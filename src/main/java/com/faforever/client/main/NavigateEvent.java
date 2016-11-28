package com.faforever.client.main;

public class NavigateEvent {
  private final NavigationItem item;

  public NavigateEvent(NavigationItem item) {
    this.item = item;
  }

  public NavigationItem getItem() {
    return item;
  }
}
