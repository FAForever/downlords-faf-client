package com.faforever.client.legacy.domain;

public class
RemoveFriendMessage extends ClientMessage {

  private int friend;

  public RemoveFriendMessage(int playerId) {
    super(ClientMessageType.SOCIAL_REMOVE);
    this.friend = playerId;
  }

  public int getFriend() {
    return friend;
  }
}
