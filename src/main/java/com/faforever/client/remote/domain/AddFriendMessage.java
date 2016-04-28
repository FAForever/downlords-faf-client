package com.faforever.client.remote.domain;

public class AddFriendMessage extends ClientMessage {

  private int friend;

  public AddFriendMessage(int playerId) {
    super(ClientMessageType.SOCIAL_ADD);
    this.friend = playerId;
  }

  public int getFriend() {
    return friend;
  }
}
