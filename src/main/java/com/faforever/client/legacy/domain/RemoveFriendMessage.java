package com.faforever.client.legacy.domain;

public class RemoveFriendMessage extends ClientMessage {

  private int friendId;

  public RemoveFriendMessage(int friendId) {
    super(ClientMessageType.SOCIAL_REMOVE);
    this.friendId = friendId;
  }

  public int getFriendId() {
    return friendId;
  }
}
