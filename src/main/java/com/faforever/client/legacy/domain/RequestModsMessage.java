package com.faforever.client.legacy.domain;

public class RequestModsMessage extends ClientMessage {

  private final String type;

  public RequestModsMessage() {
    super(ClientMessageType.MOD_VAULT);
    this.type = "start";
  }

  public String getType() {
    return type;
  }
}
