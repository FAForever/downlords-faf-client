package com.faforever.client.legacy.relay;


import com.faforever.client.legacy.domain.SerializableMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.legacy.relay.LobbyAction.PONG;

public class LobbyMessage implements SerializableMessage {

  private LobbyAction action;
  // Because typos in protocols are cool (this class is JSON serialized).
  private List<Object> chuncks;

  public LobbyMessage(LobbyAction action, List<Object> chunks) {
    this.action = action;
    this.chuncks = chunks;
  }

  public List<Object> getChunks() {
    return chuncks;
  }

  public LobbyAction getAction() {
    return action;
  }

  public static LobbyMessage pong() {
    return new LobbyMessage(PONG, Collections.emptyList());
  }

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

}
