package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.SerializableMessage;

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
