package com.faforever.client.remote.domain;

import java.util.Collection;

public interface SerializableMessage {

  /**
   * Returns a list of strings that should be masked when it's logged, like the password. It would be better to define
   * the fields to mask but that implementation would be slightly more complex to do in a safe way.
   */
  Collection<String> getStringsToMask();
}
