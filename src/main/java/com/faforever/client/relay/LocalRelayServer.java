package com.faforever.client.relay;

import java.util.function.Consumer;

/**
 * A local relay server to which Forged Alliance can connect to. All data received from FA is transformed and forwarded
 * to the FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  void addOnReadyListener(OnReadyListener listener);

  void addOnConnectionAcceptedListener(Runnable listener);

  int getPort();

  void startInBackground();

  void close();

  void setGameLaunchedListener(Consumer<Void> gameLaunchedListener);
}
