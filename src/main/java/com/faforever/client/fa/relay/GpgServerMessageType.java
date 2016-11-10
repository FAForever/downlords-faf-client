package com.faforever.client.fa.relay;

import com.faforever.client.remote.domain.SdpRecordServerMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.remote.domain.ServerMessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of known server commands (the "command" part of a server domain).
 */
public enum GpgServerMessageType implements ServerMessageType {
  HOST_GAME("HostGame", HostGameMessage.class),
  JOIN_GAME("JoinGame", JoinGameMessage.class),
  CONNECT_TO_PEER("ConnectToPeer", ConnectToPeerMessage.class),
  SDP_RECORD("SdpRecord", SdpRecordServerMessage.class),
  DISCONNECT_FROM_PEER("DisconnectFromPeer", DisconnectFromPeerMessage.class);


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

  public String getString() {
    return string;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> Class<T> getType() {
    return (Class<T>) type;
  }
}
