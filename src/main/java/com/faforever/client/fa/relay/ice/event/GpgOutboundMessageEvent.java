package com.faforever.client.fa.relay.ice.event;


import com.faforever.client.remote.domain.outbound.gpg.GpgOutboundMessage;
import lombok.Value;

@Value
public class GpgOutboundMessageEvent {
  GpgOutboundMessage gpgMessage;
}
