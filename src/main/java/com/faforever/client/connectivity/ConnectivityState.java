package com.faforever.client.connectivity;

import java.util.HashMap;

public enum ConnectivityState {
  /**
   * The client is capable of receiving UDP messages a priori on the advertised game port.
   */
  PUBLIC("PUBLIC"),
  /**
   * The client is able to exchange UDP messages after having punched a hole through its NAT.
   */
  STUN("STUN"),
  /**
   * The client is incapable of sending/receiving UDP messages.
   */
  BLOCKED("BLOCKED"),
  /**
   * The connectivity check is currently running.
   */
  RUNNING("RUNNING"),
  /**
   * The connection has not yet been tested, or the test failed.
   */
  UNKNOWN(null);

  private static final HashMap<String, ConnectivityState> fromString;

  static {
    fromString = new HashMap<>();
    for (ConnectivityState connectivityState : values()) {
      fromString.put(connectivityState.string, connectivityState);
    }
  }

  private String string;

  ConnectivityState(String string) {
    this.string = string;
  }

  public static ConnectivityState fromString(String string) {
    return fromString.get(string);
  }

  public String getString() {
    return string;
  }
}
