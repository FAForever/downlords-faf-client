package com.faforever.client.remote.domain.inbound.faf;

import com.faforever.client.remote.domain.inbound.InboundMessage;

/**
 * Superclass for all server objects. Server objects are deserialized from a JSON-like string, therefore all field names
 * and types must exactly match to what the server sends.. A server object's concrete type is derived by its command
 * field.
 */

public abstract class FafInboundMessage extends InboundMessage {

  protected FafInboundMessage() {
    super(null);
  }
}
