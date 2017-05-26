package com.faforever.client.remote.gson;

import com.faforever.client.fa.relay.GpgClientCommand;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.remote.domain.MessageTarget;

import java.util.Collections;

public class InitConnectivityTestMessage extends GpgGameMessage {

  private static final int PORT_INDEX = 0;

  public InitConnectivityTestMessage(int port) {
    super(GpgClientCommand.INIT_CONNECTIVITY_TEST, Collections.singletonList(port));
    setTarget(MessageTarget.CONNECTIVITY);
  }

  public int getPort() {
    return getInt(PORT_INDEX);
  }
}
