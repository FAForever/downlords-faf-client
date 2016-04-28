package com.faforever.client.replay;

import com.faforever.client.remote.domain.ClientMessage;
import com.faforever.client.remote.domain.ClientMessageType;

public class ListReplaysMessage extends ClientMessage {

  public ListReplaysMessage() {
    super(ClientMessageType.LIST_REPLAYS);
  }
}
