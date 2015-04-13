package com.faforever.client.legacy.relay;

import java.io.IOException;

/**
 * A local relay server to which Forged Alliance can connect to. All data received from FA is transformed and forwarded
 * to the FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  int getPort();

  void startInBackground() throws IOException;
}
