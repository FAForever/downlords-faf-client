package com.faforever.client.legacy.domain;

import java.util.Collection;

public class FriendsMessage extends ClientMessage {

  private Collection<String> friends;

  public FriendsMessage(Collection<String> friends) {
    setCommand(ClientMessageType.SOCIAL);
    this.setFriends(friends);
  }

  public Collection<String> getFriends() {
    return friends;
  }

  public void setFriends(Collection<String> friends) {
    this.friends = friends;
  }
}
