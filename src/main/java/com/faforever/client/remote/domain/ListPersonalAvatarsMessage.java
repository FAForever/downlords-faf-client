package com.faforever.client.remote.domain;

public class ListPersonalAvatarsMessage extends ClientMessage {

  private final String action;

  public ListPersonalAvatarsMessage() {
    super(ClientMessageType.AVATAR);
    this.action = "list_avatar";
  }
}
