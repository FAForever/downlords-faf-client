package com.faforever.client.remote.domain.outbound.gpg;




public class RehostMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Rehost";

  public RehostMessage() {
    super(COMMAND);
  }
}
