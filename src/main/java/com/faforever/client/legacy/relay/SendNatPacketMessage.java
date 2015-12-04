package com.faforever.client.legacy.relay;

import java.net.InetSocketAddress;

public class SendNatPacketMessage extends GpgServerMessage {

  public static final int PUBLIC_ADDRESS_INDEX = 0;
  public static final int MESSAGE_ADDRESS_INDEX = 1;

  public SendNatPacketMessage() {
    super(GpgServerMessageType.SEND_NAT_PACKET, 1);
  }

  public InetSocketAddress getPublicAddress() {
    String[] split = ((String) getArgs().get(PUBLIC_ADDRESS_INDEX)).split(":");
    return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
  }

  public void setPublicAddress(String addressString) {
    setValue(PUBLIC_ADDRESS_INDEX, addressString);
  }

  public String getMessage() {
    return (String) getArgs().get(MESSAGE_ADDRESS_INDEX);
  }
}
