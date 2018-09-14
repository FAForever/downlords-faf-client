package com.faforever.client.fa.relay.ice;

import java.util.List;
import java.util.Map;

/**
 * API functions provided by the ICE adapter process.
 */
interface IceAdapterApi {

  /** Gracefully shuts down the. */
  void quit();

  /** Tell the game to create the lobby and host game on Lobby-State. */
  void hostGame(String mapName);

  /** Tell the game to create the Lobby, create a PeerRelay in answer mode and join the remote game. */
  void joinGame(String remotePlayerLogin, int remotePlayerId);

  /** Create a PeerRelay and tell the game to connect to the remote peer with offer/answer mode. */
  void connectToPeer(String remotePlayerLogin, int remotePlayerId, boolean offer);

  /** Destroy PeerRelay and tell the game to disconnect from the remote peer. */
  void disconnectFromPeer(int remotePlayerId);

  /**
   * Set the lobby mode the game will use. Supported values are "normal" for normal lobby and "auto" for automatch lobby
   * (aka ladder).
   */
  void setLobbyInitMode(String mode);

  /** Add the remote ICE message to the PeerRelay to establish a connection. */
  void iceMsg(int remotePlayerId, Object message);

  /** Send an arbitrary message to the game. */
  void sendToGpgNet(String header, List<Object> chunks);

  /** Polls the current status of the faf-ice-adapter. */
  Map<String, Object> status();

  /**
   * ICE server array for use in webrtc. Must be called before joinGame/connectToPeer. See <a
   * href="https://developer.mozilla.org/en-US/docs/Web/API/RTCIceServer">https://developer.mozilla.org/en-US/docs/Web/API/RTCIceServer</a>.
   */
  void setIceServers(List<Map<String, Object>> iceServers);
}
