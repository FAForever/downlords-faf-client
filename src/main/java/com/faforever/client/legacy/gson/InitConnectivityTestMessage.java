package com.faforever.client.legacy.gson;

import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.relay.GpgClientCommand;
import com.faforever.client.legacy.relay.GpgClientMessage;

import java.util.Collections;

public class InitConnectivityTestMessage extends GpgClientMessage {

  private MessageTarget target = MessageTarget.CONNECTIVITY;

  public InitConnectivityTestMessage(int port) {
    super(GpgClientCommand.INIT_CONNECTIVITY_TEST, Collections.singletonList(port));
  }
}
