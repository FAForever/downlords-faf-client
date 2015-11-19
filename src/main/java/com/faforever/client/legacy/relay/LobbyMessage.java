package com.faforever.client.legacy.relay;


import com.faforever.client.legacy.domain.SerializableMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.legacy.relay.LobbyAction.PONG;

public class LobbyMessage implements SerializableMessage {

  private String action;
  // Because typos in protocols are cool (this class is JSON serialized).
  private List<Object> chunks;

  public LobbyMessage(LobbyAction action, List<Object> chunks) {
    this(action.getString(), chunks);
  }

  public LobbyMessage(String action, List<Object> chunks) {
    this.action = action;
    this.chunks = chunks;
  }

  public List<Object> getChunks() {
    return chunks;
  }

  public LobbyAction getAction() {
    return LobbyAction.fromString(action);
  }

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  public static LobbyMessage pong() {
    return new LobbyMessage(PONG, Collections.emptyList());
  }

}
