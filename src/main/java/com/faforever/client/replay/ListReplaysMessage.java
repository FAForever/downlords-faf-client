package com.faforever.client.replay;

import com.faforever.client.legacy.domain.ClientMessage;

public class ListReplaysMessage extends ClientMessage {

  public ListReplaysMessage() {
    command = "list";
  }
}
