package com.faforever.client.remote;

import com.faforever.client.remote.domain.SerializableMessage;

import java.util.Collection;
import java.util.Collections;

public class PongMessage implements SerializableMessage {

  private static final String PONG = "PONG";

  public String getString() {
    return PONG;
  }

  @Override
  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }
}
