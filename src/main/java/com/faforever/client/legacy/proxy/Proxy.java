package com.faforever.client.legacy.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

public interface Proxy {

  interface OnP2pProxyInitializedListener {

    void onP2pProxyInitialized();
  }

  /**
   * Closes all proxy connections.
   */
  void close() throws IOException;

  void updateConnectedState(int uid, boolean connected);

  void setGameLaunched(boolean gameLaunched);

  void setBottleneck(boolean bottleneck);

  /**
   * Translates a local socket address of a player (e.g. 127.0.0.1:51234) to its public socket address (e.g.
   * 84.53.132.41:6112).
   */
  String translateToPublic(String localAddress);

  /**
   * Translates a public socket address of a player (e.g. 84.53.132.41:6112) to its local socket address (e.g.
   * 127.0.0.1:51234).
   */
  String translateToLocal(String publicAddress);

  void registerP2pPeerIfNecessary(String publicAddress);

  void initializeP2pProxy() throws SocketException;

  void setUidForPeer(String peerAddress, int peerUid);

  void setUid(int uid);

  int getPort();

  /**
   * Opens a local UDP socket that serves as a proxy for a player to FA. In other words, instead of connecting to player
   * X directly, a local port is opened that represents that player. FA will then connect to that port, thinking it's
   * player X. All data from FA is then forwarded to the FA proxy server, which again forwards it to the user X.
   *
   * @return the proxy socket address this player has been bound to
   */
  InetSocketAddress bindAndGetProxySocketAddress(int playerNumber, int uid) throws IOException;

  void addOnP2pProxyInitializedListener(OnP2pProxyInitializedListener listener);
}
