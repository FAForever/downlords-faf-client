package com.faforever.client.fa.relay.ice;

import java.util.List;

/**
 * API functions provided by the ICE adapter process.
 */
interface IceAdapterApi {
  void quit();

  void hostGame(String mapName);

  void joinGame(String remotePlayerLogin, int remotePlayerId);

  void connectToPeer(String remotePlayerLogin, int remotePlayerId, boolean offer);

  void disconnectFromPeer(int remotePlayerId);

  void iceMsg(int remotePlayerId, Object message);

  void sendToGpgNet(String header, List<Object> chunks);

  void status(String header, List<Object> chunks);
}
