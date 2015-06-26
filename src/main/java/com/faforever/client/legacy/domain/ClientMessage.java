package com.faforever.client.legacy.domain;

import java.util.Collection;
import java.util.Collections;

public abstract class ClientMessage implements SerializableMessage {

  public String action;
  public String command;

  public Collection<String> getStringsToMask() {
    return Collections.emptyList();
  }
}
