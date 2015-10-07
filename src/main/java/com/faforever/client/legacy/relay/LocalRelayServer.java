package com.faforever.client.legacy.relay;

import com.faforever.client.stats.domain.GameStats;

import java.util.function.Consumer;

/**
 * A local relay server to which Forged Alliance can connect to. All data received from FA is transformed and forwarded
 * to the FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
// TODO this isn't yet the legacy part, move it to an outside package
public interface LocalRelayServer {

  void addOnReadyListener(OnReadyListener listener);

  void addOnConnectionAcceptedListener(OnConnectionAcceptedListener listener);

  int getPort();

  void startInBackground();

  void close();

  void setGameStatsListener(Consumer<GameStats> gameStatsListener);

  void setGameLaunchedListener(Consumer<Void> gameLaunchedListener);
}
