package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.portcheck.ConnectivityState;

public class ConnectivityStateMessage extends FafServerMessage {

  private ConnectivityState state;

  public ConnectivityStateMessage() {
    super(FafServerMessageType.CONNECTIVITY_STATE);
  }

  public ConnectivityState getState() {
    return state;
  }
}
