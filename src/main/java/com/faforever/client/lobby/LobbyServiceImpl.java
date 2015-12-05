package com.faforever.client.lobby;

import com.faforever.client.game.Faction;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.ConnectionState;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import javafx.beans.property.ObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LobbyServiceImpl implements LobbyService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  LobbyServerAccessor lobbyServerAccessor;

  @Override
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    lobbyServerAccessor.addOnMessageListener(type, listener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    lobbyServerAccessor.removeOnMessageListener(type, listener);
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestNewGame(NewGameInfo newGameInfo) {
    return lobbyServerAccessor.requestNewGame(newGameInfo);
  }

  @Override
  public ObjectProperty<ConnectionState> connectionStateProperty() {
    return lobbyServerAccessor.connectionStateProperty();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(GameInfoBean gameInfoBean, String password) {
    return lobbyServerAccessor.requestJoinGame(gameInfoBean, password);
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port) {
    return lobbyServerAccessor.startSearchRanked1v1(faction, port);
  }

  @Override
  public void stopSearchingRanked() {
    lobbyServerAccessor.stopSearchingRanked();
  }
}
