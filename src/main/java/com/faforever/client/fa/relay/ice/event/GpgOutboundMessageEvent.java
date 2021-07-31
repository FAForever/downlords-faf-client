package com.faforever.client.fa.relay.ice.event;


import com.faforever.commons.lobby.GpgGameOutboundMessage;
import lombok.Value;

@Value
public class GpgOutboundMessageEvent {
  GpgGameOutboundMessage gpgMessage;
}
