package com.faforever.client.remote.domain.outbound.gpg;


import lombok.Builder;


public class JsonStatsMessage extends GpgOutboundMessage {
  public static final String COMMAND = "JsonStats";

  public JsonStatsMessage() {
    super(COMMAND);
  }
}
