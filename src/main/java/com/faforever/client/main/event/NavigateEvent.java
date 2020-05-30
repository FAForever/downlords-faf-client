package com.faforever.client.main.event;

import lombok.Data;

@Data
public class NavigateEvent {
  private final NavigationItem item;
}
