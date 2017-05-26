package com.faforever.client.relay;

import com.faforever.client.connectivity.ConnectivityState;
import com.faforever.client.fa.relay.GpgServerMessage;
import com.faforever.client.fa.relay.GpgServerMessageType;
import com.faforever.client.net.SocketAddressUtil;
import com.faforever.client.remote.domain.MessageTarget;

import java.net.InetSocketAddress;

public class ConnectivityStateMessage extends GpgServerMessage {

  private static final int STATE_INDEX = 0;
  private static final int ADDRESS_INDEX = 1;

  public ConnectivityStateMessage(ConnectivityState state, InetSocketAddress socketAddress) {
    super(GpgServerMessageType.CONNECTIVITY_STATE, 2);
    setValue(STATE_INDEX, state.getString());
    setValue(ADDRESS_INDEX, new Object[]{socketAddress.getHostString(), socketAddress.getPort()});
    setTarget(MessageTarget.CONNECTIVITY);
  }

  public ConnectivityState getState() {
    return ConnectivityState.fromString(getString(STATE_INDEX));
  }

  public InetSocketAddress getSocketAddress() {
    // TODO remove when sheeo finally decided how to implement it ;-)
    if (getArgs().get(ADDRESS_INDEX) instanceof String) {
      return SocketAddressUtil.fromString(getString(ADDRESS_INDEX));
    }
    @SuppressWarnings("unchecked")
    Object[] hostPort = getObject(ADDRESS_INDEX);
    return new InetSocketAddress((String) hostPort[0], (int) hostPort[1]);
  }
}
