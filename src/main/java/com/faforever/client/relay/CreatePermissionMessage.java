package com.faforever.client.relay;

import com.faforever.client.util.SocketAddressUtil;

import java.net.InetSocketAddress;

public class CreatePermissionMessage extends GpgServerMessage {

  private static final int ADDRESS_INDEX = 0;

  public CreatePermissionMessage() {
    super(GpgServerMessageType.CREATE_PERMISSION, 1);
  }

  public InetSocketAddress getAddress() {
    return SocketAddressUtil.fromString(getString(ADDRESS_INDEX));
  }

  public void setAddress(InetSocketAddress address) {
    setValue(ADDRESS_INDEX, SocketAddressUtil.toString(address));
  }
}
