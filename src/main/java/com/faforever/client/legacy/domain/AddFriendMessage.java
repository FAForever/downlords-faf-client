package com.faforever.client.legacy.domain;

public class AddFriendMessage extends ClientMessage {

  private int friendId;

  protected AddFriendMessage(int friendId) {
    super(ClientMessageType.SOCIAL_ADD);
    this.friendId = friendId;
  }

  public int getFriendId() {
    return friendId;
  }
}
