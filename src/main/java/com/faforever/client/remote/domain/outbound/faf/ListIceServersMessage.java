package com.faforever.client.remote.domain.outbound.faf;


import lombok.Builder;


public class ListIceServersMessage extends FafOutboundMessage {
  public static final String COMMAND = "ice_servers";
}
