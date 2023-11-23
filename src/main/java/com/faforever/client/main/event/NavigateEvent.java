package com.faforever.client.main.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NavigateEvent {
  private final NavigationItem item;
}
