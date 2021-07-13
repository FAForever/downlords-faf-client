package com.faforever.client.remote.domain.outbound.faf;

import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.outbound.OutboundMessage;

public abstract class FafOutboundMessage extends OutboundMessage {

  public FafOutboundMessage() {
    super(MessageTarget.LOBBY);
  }

}
