package com.faforever.client.legacy.relay;

import java.io.IOException;

/**
 * A local relay server to which Forged Alliance can connect to. All data received from FA is transformed and forwarded
 * to the FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
// TODO this isn't yet the legacy part, move it to an outside package
public interface LocalRelayServer {

  void addOnReadyListener(OnReadyListener listener);

  void addOnConnectionAcceptedListener(OnConnectionAcceptedListener listener);

  int getPort();

  void startInBackground() throws IOException;
}
