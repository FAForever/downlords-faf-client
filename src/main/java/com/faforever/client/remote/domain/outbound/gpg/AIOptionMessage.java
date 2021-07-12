package com.faforever.client.remote.domain.outbound.gpg;




public class AIOptionMessage extends GpgOutboundMessage {
  public static final String COMMAND = "AIOption";

  public AIOptionMessage() {
    super(COMMAND);
  }
}
