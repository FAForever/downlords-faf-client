package com.faforever.client.legacy.relay;

public class SendNatPacketMessage extends GpgServerMessage {

  public static final int PUBLIC_ADDRESS_INDEX = 0;

  public SendNatPacketMessage() {
    super(GpgServerCommandServerCommand.SEND_NAT_PACKET);
  }

  public String getPublicAddress() {
    return getString(PUBLIC_ADDRESS_INDEX);
  }

  public void setPublicAddress(String addressString) {
    setValue(PUBLIC_ADDRESS_INDEX, addressString);
  }
}
