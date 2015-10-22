package com.faforever.client.relay;

import com.faforever.client.stats.domain.GameStats;

import java.util.List;
import java.util.function.Consumer;

/**
 * A local relay server to which Forged Alliance can connect to. All data received from FA is transformed and forwarded
 * to the FAF relay server, and vice-versa. To FA, this looks like a GPG server.
 */
public interface LocalRelayServer {

  void addOnReadyListener(OnReadyListener listener);

  void addOnConnectionAcceptedListener(OnConnectionAcceptedListener listener);

  int getPort();

  void startInBackground();

  void close();

  void setGameStatsListener(Consumer<GameStats> gameStatsListener);

  void setGameLaunchedListener(Consumer<Void> gameLaunchedListener);

  void setGameOptionListener(Consumer<List<Object>> gameOptionListener);
}
