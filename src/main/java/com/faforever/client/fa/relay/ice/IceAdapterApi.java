package com.faforever.client.fa.relay.ice;

import java.util.List;

/**
 * API functions provided by the ICE adapter binary.
 */
interface IceAdapterApi {
  void quit();

  void hostGame(String mapName);

  void joinGame(String remotePlayerLogin, int remotePlayerId);

  void connectToPeer(String remotePlayerLogin, int remotePlayerId);

  void disconnectFromPeer(int remotePlayerId);

  void setSdp(int remotePlayerId, String sdp64);

  void sendToGpgNet(String header, List<Object> chunks);

  void status(String header, List<Object> chunks);
}
