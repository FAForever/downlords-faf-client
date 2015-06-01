package com.faforever.client.legacy.relay;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of known server commands (the "command" part of a server domain).
 */
public enum RelayServerCommand {
  PING("ping"),
  HOST_GAME("HostGame"),
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
    RelayServerCommand relayServerCommand = fromString.get(string);
    if (relayServerCommand == null) {
      /*
       * If an unknown command is received, ignoring it would probably cause the application to enter an unknown state.
       * So it's better to crash right now so there's no doubt that something went wrong.
       */
      throw new IllegalArgumentException("Unknown relay server command: " + string);
    }
    return relayServerCommand;
  }
}
