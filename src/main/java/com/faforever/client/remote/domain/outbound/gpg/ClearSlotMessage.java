package com.faforever.client.remote.domain.outbound.gpg;




public class ClearSlotMessage extends GpgOutboundMessage {
  public static final String COMMAND = "ClearSlot";

  public ClearSlotMessage() {
    super(COMMAND);
  }
}
