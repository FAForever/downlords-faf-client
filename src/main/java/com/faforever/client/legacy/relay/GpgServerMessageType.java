package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of known server commands (the "command" part of a server domain).
 */
public enum GpgServerMessageType implements ServerMessageType {
  HOST_GAME("HostGame", HostGameMessage.class),
  SEND_NAT_PACKET("SendNatPacket", SendNatPacketMessage.class),
  P2P_RECONNECT("P2pReconnect", P2pReconnectMessage.class),
  JOIN_GAME("JoinGame", JoinGameMessage.class),
  CONNECT_TO_PEER("ConnectToPeer", ConnectToPeerMessage.class),
  CREATE_LOBBY("CreateLobby", CreateLobbyServerMessage.class),
  DISCONNECT_FROM_PEER("DisconnectFromPeer", DisconnectFromPeerMessage.class),
  JOIN_PROXY("JoinProxy", JoinProxyMessage.class);

  private static final Map<String, GpgServerMessageType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (GpgServerMessageType gpgServerMessageType : values()) {
      fromString.put(gpgServerMessageType.string, gpgServerMessageType);
    }
  }

  private final String string;
  private Class<? extends GpgServerMessage> type;

  GpgServerMessageType(String string, Class<? extends GpgServerMessage> type) {
    this.string = string;
    this.type = type;
  }

  public String getString() {
    return string;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> Class<T> getType() {
    return (Class<T>) type;
  }

  public static GpgServerMessageType fromString(String string) {
    GpgServerMessageType gpgServerMessageType = fromString.get(string);
    if (gpgServerMessageType == null) {
      /*
       * If an unknown command is received, ignoring it would probably cause the application to enter an unknown state.
       * So it's better to crash right now so there's no doubt that something went wrong.
       */
      throw new IllegalArgumentException("Unknown relay server command: " + string);
    }
    return gpgServerMessageType;
  }
}
