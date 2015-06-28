package com.faforever.client.legacy.domain;

import java.util.Collection;

public class FriendsMessage extends ClientMessage {

  private final Collection<String> friends;

  public FriendsMessage(Collection<String> friends) {
    command = "social";
    this.friends = friends;
  }
}
