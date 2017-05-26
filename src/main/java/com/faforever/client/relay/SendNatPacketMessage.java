package com.faforever.client.relay;

import com.faforever.client.fa.relay.GpgServerMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;
import com.faforever.client.net.SocketAddressUtil;
import com.faforever.client.remote.domain.MessageTarget;

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
    return getString(MESSAGE_INDEX);
  }

  public void setMessage(String message) {
    setValue(MESSAGE_INDEX, message);
  }
}
