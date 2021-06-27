package com.faforever.client.remote.domain.outbound.gpg;

import lombok.Builder;


public class ClearSlotMessage extends GpgOutboundMessage {
  public static final String COMMAND = "ClearSlot";

  public ClearSlotMessage() {
    super(COMMAND);
  }
}
