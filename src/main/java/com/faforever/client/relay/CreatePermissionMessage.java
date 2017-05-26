package com.faforever.client.relay;

import com.faforever.client.fa.relay.GpgServerMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;

import java.net.InetSocketAddress;

public class CreatePermissionMessage extends GpgServerMessage {

  private static final int ADDRESS_INDEX = 0;
  private static final int PORT_INDEX = 1;

  public CreatePermissionMessage() {
    super(GpgServerMessageType.CREATE_PERMISSION, 2);
  }

  public InetSocketAddress getAddress() {
    return new InetSocketAddress(getString(ADDRESS_INDEX), getInt(PORT_INDEX));
  }

  public void setAddress(InetSocketAddress address) {
    setValue(ADDRESS_INDEX, address.getHostString());
    setValue(PORT_INDEX, address.getPort());
  }
}
