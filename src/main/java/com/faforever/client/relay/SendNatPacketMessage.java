package com.faforever.client.relay;

import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.util.SocketAddressUtil;

import java.net.InetSocketAddress;

public class SendNatPacketMessage extends GpgServerMessage {

  public static final int PUBLIC_ADDRESS_INDEX = 0;
  public static final int MESSAGE_INDEX = 1;

  public SendNatPacketMessage() {
    super(GpgServerMessageType.SEND_NAT_PACKET, 2);
    setTarget(MessageTarget.GAME);
  }

  public InetSocketAddress getPublicAddress() {
    return SocketAddressUtil.fromString(getString(PUBLIC_ADDRESS_INDEX));
  }

  public void setPublicAddress(InetSocketAddress addressString) {
    setValue(PUBLIC_ADDRESS_INDEX, SocketAddressUtil.toString(addressString));
  }

  public String getMessage() {
    return (String) getArgs().get(MESSAGE_INDEX);
  }

  public void setMessage(String message) {
    setValue(MESSAGE_INDEX, message);
  }
}
