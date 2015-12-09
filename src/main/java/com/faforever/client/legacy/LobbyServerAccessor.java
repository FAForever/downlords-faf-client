package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.GpgClientMessage;
import javafx.beans.property.ObjectProperty;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Entry class for all communication with the FAF lobby server, be it reading or writing. This class should only be
 * called from within services.
 */
public interface LobbyServerAccessor {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  ObjectProperty<ConnectionState> connectionStateProperty();

  /**
   * Connects to the FAF server and logs in using the credentials from {@link PreferencesService}.
   */
  CompletableFuture<LoginMessage> connectAndLogIn(String username, String password);

  CompletableFuture<GameLaunchMessage> requestNewGame(NewGameInfo newGameInfo);

  CompletableFuture<GameLaunchMessage> requestJoinGame(GameInfoBean gameInfoBean, String password);

  void disconnect();

  void setFriends(Collection<String> friends);

  void setFoes(Collection<String> foes);

  CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int gamePort);

  void stopSearchingRanked();

  void expand1v1Search(float radius);

  @Nullable
  Long getSessionId();

  void sendGpgMessage(GpgClientMessage message);

  void initConnectivityTest(int port);
}
