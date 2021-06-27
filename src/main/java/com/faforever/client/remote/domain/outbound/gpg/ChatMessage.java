package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;

public class ChatMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Chat";


  public ChatMessage() {
    super(COMMAND);
  }
}
