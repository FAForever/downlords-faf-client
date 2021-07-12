package com.faforever.client.remote.domain.outbound.gpg;




public class StatsMessage extends GpgOutboundMessage {
  public static final String COMMAND = "Stats";

  public StatsMessage() {
    super(COMMAND);
  }
}
