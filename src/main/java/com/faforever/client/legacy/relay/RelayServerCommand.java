package com.faforever.client.legacy.relay;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of known server commands (the "command" part of a server domain).
 */
enum RelayServerCommand {
  PING("ping", 0),
  HOST_GAME("HostGame", 0),
  SEND_NAT_PACKET("SendNatPacket", 1),
  P2P_RECONNECT("P2pReconnect", 0),
  JOIN_GAME("JoinGame", 3),
  CONNECT_TO_PEER("ConnectToPeer", 3),
  CREATE_LOBBY("CreateLobby", 5),
  DISCONNECT_FROM_PEER("DisconnectFromPeer", 0),
  CONNECT_TO_PROXY("ConnectToProxy", 4),
  JOIN_PROXY("JoinProxy", 4);

  private static final Map<String, RelayServerCommand> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (RelayServerCommand relayServerCommand : values()) {
      fromString.put(relayServerCommand.string, relayServerCommand);
    }
  }

  private final int numberOfArgs;

  private String string;

  RelayServerCommand(String string, int numberOfArgs) {
    this.string = string;
    this.numberOfArgs = numberOfArgs;
  }

  public String getString() {
    return string;
  }

  public int getNumberOfArgs() {
    return numberOfArgs;
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
