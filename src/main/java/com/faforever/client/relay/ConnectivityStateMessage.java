package com.faforever.client.relay;

import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.util.SocketAddressUtil;

import java.net.InetSocketAddress;
import java.util.Collections;

public class ConnectivityStateMessage extends GpgServerMessage {

  private static final int STATE_INDEX = 0;
  private static final int ADDRESS_INDEX = 1;

  public ConnectivityStateMessage(ConnectivityState state) {
    super(GpgServerMessageType.CONNECTIVITY_STATE, Collections.singletonList(state.getString()));
    setTarget(MessageTarget.CONNECTIVITY);
  }

  public ConnectivityState getState() {
    return ConnectivityState.fromString((String) getArgs().get(STATE_INDEX));
  }

  public InetSocketAddress getSocketAddress() {
    return SocketAddressUtil.fromString(getString(ADDRESS_INDEX));
  }
}
