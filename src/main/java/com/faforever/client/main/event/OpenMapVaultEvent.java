package com.faforever.client.main.event;

public sealed class OpenMapVaultEvent extends NavigateEvent permits ShowMapPoolEvent {
  public OpenMapVaultEvent() {
    super(NavigationItem.MAP);
  }
}
