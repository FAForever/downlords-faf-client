package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.domain.MessageTarget;

import java.util.Arrays;

public class ProcessNatPacketMessage extends GpgClientMessage {

  private MessageTarget target;

  public ProcessNatPacketMessage(String address, String message) {
    super(GpgClientCommand.PROCESS_NAT_PACKET, Arrays.asList(address, message));
  }

  public void setTarget(MessageTarget target) {
    this.target = target;
  }
}
