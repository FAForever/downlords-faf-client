package com.faforever.client.legacy.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface Proxy {

  /**
   * Closes all proxy connections.
   */
  void close() throws IOException;

  void updateConnectedState(int uid, boolean connected);

  void setGameLaunched(boolean gameLaunched);

  void setBottleneck(boolean bottleneck);

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

}
