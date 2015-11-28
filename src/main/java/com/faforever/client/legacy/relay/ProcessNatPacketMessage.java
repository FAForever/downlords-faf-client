package com.faforever.client.legacy.relay;

import java.util.Arrays;

public class ProcessNatPacketMessage extends GpgClientMessage {

  public ProcessNatPacketMessage(String address, String message) {
    super(GpgClientCommand.PROCESS_NAT_PACKET, Arrays.asList(address, message));
  }

}
