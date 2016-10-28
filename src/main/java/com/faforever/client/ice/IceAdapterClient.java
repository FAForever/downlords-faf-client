package com.faforever.client.ice;

import java.util.List;

public interface IceAdapterClient {
  void quit();

  void hostGame(String mapName);

  void joinGame(String remotePlayerLogin, int remotePlayerId);

  void connectToPeer(String remotePlayerLogin, int remotePlayerId);

  void disconnectFromPeer(int remotePlayerId);

  void setSdp(int remotePlayerId, String sdp64);

  void sendToGpgNet(String header, List<Object> chunks);

  void status(String header, List<Object> chunks);
}
