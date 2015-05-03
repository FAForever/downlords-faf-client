package com.faforever.client.legacy.relay;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of known server commands (the "command" part of a server message).
 */
public enum RelayServerCommand {
  PING("ping"),
  SEND_NAT_PACKET("SendNatPacket"),
  P2P_RECONNECT("P2pReconnect"),
  JOIN_GAME("JoinGame"),
  CONNECT_TO_PEER("ConnectToPeer"),
  CREATE_LOBBY("CreateLobby"),
  CONNECT_TO_PROXY("ConnectToProxy"),
  JOIN_PROXY("JoinProxy");

  private static final Map<String, RelayServerCommand> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (RelayServerCommand relayServerCommand : values()) {
      fromString.put(relayServerCommand.string, relayServerCommand);
    }
  }

  private String string;

  RelayServerCommand(String string) {
    this.string = string;
  }

  public String getString() {
    return string;
  }

  public static RelayServerCommand fromString(String string) {
    return fromString.get(string);
  }

}
