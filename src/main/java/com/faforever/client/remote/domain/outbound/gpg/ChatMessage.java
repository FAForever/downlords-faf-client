package com.faforever.client.remote.domain.outbound.gpg;



public class ChatMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Chat";


  public ChatMessage() {
    super(COMMAND);
  }
}
