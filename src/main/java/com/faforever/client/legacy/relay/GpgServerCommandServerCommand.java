package com.faforever.client.legacy.relay;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of known server commands (the "command" part of a server domain).
 */
enum GpgServerCommandServerCommand {
  PING("ping", 0),
  HOST_GAME("HostGame", 0),
  SEND_NAT_PACKET("SendNatPacket", 1),
  P2P_RECONNECT("P2pReconnect", 0),
  JOIN_GAME("JoinGame", 3),
  CONNECT_TO_PEER("ConnectToPeer", 3),
  CREATE_LOBBY("CreateLobby", 5),
  DISCONNECT_FROM_PEER("DisconnectFromPeer", 0),
  CONNECT_TO_PROXY("ConnectToProxy", 4),
  JOIN_PROXY("JoinProxy", 4),
  CONNECTIVITY_STATE("ConnectivityState", 2);

  private static final Map<String, GpgServerCommandServerCommand> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (GpgServerCommandServerCommand gpgServerCommandServerCommand : values()) {
      fromString.put(gpgServerCommandServerCommand.string, gpgServerCommandServerCommand);
    }
  }

  private final int numberOfArgs;

  private final String string;

  GpgServerCommandServerCommand(String string, int numberOfArgs) {
    this.string = string;
    this.numberOfArgs = numberOfArgs;
  }

  public String getString() {
    return string;
  }

  public int getNumberOfArgs() {
    return numberOfArgs;
  }

  public static GpgServerCommandServerCommand fromString(String string) {
    GpgServerCommandServerCommand gpgServerCommandServerCommand = fromString.get(string);
    if (gpgServerCommandServerCommand == null) {
      /*
       * If an unknown command is received, ignoring it would probably cause the application to enter an unknown state.
       * So it's better to crash right now so there's no doubt that something went wrong.
       */
      throw new IllegalArgumentException("Unknown relay server command: " + string);
    }
    return gpgServerCommandServerCommand;
  }
}
