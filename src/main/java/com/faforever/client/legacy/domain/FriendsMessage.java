package com.faforever.client.legacy.domain;

import java.util.Collection;

public class FriendsMessage extends ClientMessage {

  public Collection<String> friends;

  public FriendsMessage(Collection<String> friends) {
    command = "social";
    this.friends = friends;
  }
}
