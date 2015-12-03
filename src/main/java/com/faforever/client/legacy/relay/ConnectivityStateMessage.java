package com.faforever.client.legacy.relay;

import com.faforever.client.portcheck.ConnectivityState;

import java.util.Collections;

public class ConnectivityStateMessage extends GpgServerMessage {

  private static final int STATE_INDEX = 0;

  public ConnectivityStateMessage(ConnectivityState state) {
    super(GpgServerMessageType.CONNECTIVITY_STATE, Collections.singletonList(state.getString()));
  }

  public ConnectivityState getState() {
    return ConnectivityState.fromString((String) getArgs().get(STATE_INDEX));
  }
}
