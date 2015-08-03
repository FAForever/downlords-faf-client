package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

public class ClientMessage implements SerializableMessage {

  public String command;

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }
}
