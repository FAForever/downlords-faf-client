package com.faforever.client.legacy.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

public interface Proxy {

  interface OnProxyInitializedListener {

    void onProxyInitialized();
  }

  void updateConnectedState(int uid, boolean connected);

  void setGameLaunched(boolean gameLaunched);

  void setBottleneck(boolean bottleneck);

  String translateToPublic(String localAddress);

  String translateToLocal(String publicAddress);

  void registerPeerIfNecessary(String publicAddress);

  void initialize() throws SocketException;

  void setUidForPeer(String peerAddress, int peerUid);

  void setUid(int uid);

  int getPort();

  InetSocketAddress bindSocket(int port, int uid) throws IOException;

  void addOnProxyInitializedListener(OnProxyInitializedListener listener);
}
