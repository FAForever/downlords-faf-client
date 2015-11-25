package com.faforever.client.legacy.relay;


import com.faforever.client.legacy.domain.SerializableMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.faforever.client.legacy.relay.GpgClientCommand.PONG;

public class GpgClientMessage implements SerializableMessage {

  private String action;
  private List<Object> chunks;

  public GpgClientMessage(GpgClientCommand action, List<Object> chunks) {
    this(action.getString(), chunks);
  }

  public GpgClientMessage(String action, List<Object> chunks) {
    this.action = action;
    this.chunks = chunks;
  }

  public List<Object> getChunks() {
    return chunks;
  }

  public GpgClientCommand getAction() {
    return GpgClientCommand.fromString(action);
  }

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }

  public static GpgClientMessage pong() {
    return new GpgClientMessage(PONG, Collections.emptyList());
  }

}
