package com.faforever.client.relay;

import com.faforever.client.util.SocketAddressUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class ProcessNatPacketMessage extends GpgClientMessage {

  private static final int ADDRESS_INDEX = 0;
  private static final int PORT_INDEX = 1;

  public ProcessNatPacketMessage(InetSocketAddress address, String message) {
    super(GpgClientCommand.PROCESS_NAT_PACKET, Arrays.asList(
        SocketAddressUtil.toString(address), message
    ));
  }

  public InetAddress getAddress() {
    try {
      return InetAddress.getByName(getString(ADDRESS_INDEX));
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  public int getPort() {
    return getInt(PORT_INDEX);
  }
}
