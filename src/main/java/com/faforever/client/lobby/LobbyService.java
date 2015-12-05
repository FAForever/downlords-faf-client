package com.faforever.client.lobby;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.ConnectionState;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import javafx.beans.property.ObjectProperty;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface LobbyService {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  CompletableFuture<GameLaunchMessage> requestNewGame(NewGameInfo newGameInfo);

  ObjectProperty<ConnectionState> connectionStateProperty();

  CompletableFuture<GameLaunchMessage> requestJoinGame(GameInfoBean gameInfoBean, String password);

  CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port);

  void stopSearchingRanked();
}
