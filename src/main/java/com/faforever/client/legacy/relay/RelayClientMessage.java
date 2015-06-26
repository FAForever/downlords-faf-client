package com.faforever.client.legacy.relay;


import com.faforever.client.legacy.domain.SerializableMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.legacy.relay.RelayServerAction.PONG;

public class RelayClientMessage implements SerializableMessage {

  private RelayServerAction action;
  // Because typos in protocols are cool (this class is JSON serialized).
  private List<Object> chuncks;

  public RelayClientMessage(RelayServerAction action, List<Object> chunks) {
    this.action = action;
    this.chuncks = chunks;
  }

  public List<Object> getChunks() {
    return chuncks;
  }

  public RelayServerAction getAction() {
    return action;
  }

  public static RelayClientMessage pong() {
    return new RelayClientMessage(PONG, Collections.emptyList());
  }

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

}
