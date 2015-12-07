package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.relay.GpgClientCommand;
import com.faforever.client.relay.GpgClientMessage;

import java.util.Collections;

public class InitConnectivityTestMessage extends GpgClientMessage {

  private static final int PORT_INDEX = 0;

  public InitConnectivityTestMessage(int port) {
    super(GpgClientCommand.INIT_CONNECTIVITY_TEST, Collections.singletonList(port));
    setTarget(MessageTarget.CONNECTIVITY);
  }

  public int getPort() {
    return getInt(PORT_INDEX);
  }
}
