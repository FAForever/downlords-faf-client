package com.faforever.client.remote;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.config.CacheNames;
import com.faforever.client.connectivity.ConnectivityService;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.legacy.domain.GameEndedMessage;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.relay.GpgClientMessage;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.springframework.cache.annotation.Cacheable;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class FafServiceImpl implements FafService {

  @Resource
  FafServerAccessor fafServerAccessor;
  @Resource
  FafApiAccessor fafApiAccessor;
  @Resource
  ConnectivityService connectivityService;
  @Resource
  Executor executor;

  @Override
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.addOnMessageListener(type, listener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafServerAccessor.removeOnMessageListener(type, listener);
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return fafServerAccessor.requestHostGame(newGameInfo,
        connectivityService.getRelayAddress(),
        connectivityService.getExternalSocketAddress().getPort()
    );
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafServerAccessor.connectionStateProperty();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return fafServerAccessor.requestJoinGame(gameId, password,
        connectivityService.getRelayAddress(),
        connectivityService.getExternalSocketAddress().getPort());
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port) {
    return fafServerAccessor.startSearchRanked1v1(faction, port);
  }

  @Override
  public void stopSearchingRanked() {
    fafServerAccessor.stopSearchingRanked();
  }

  @Override
  public void initConnectivityTest(int port) {
    fafServerAccessor.initConnectivityTest(port);
  }

  @Override
  public void sendGpgMessage(GpgClientMessage message) {
    fafServerAccessor.sendGpgMessage(message);
  }

  @Override
  public void expand1v1Search(float radius) {
    fafServerAccessor.expand1v1Search(radius);
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    return fafServerAccessor.connectAndLogIn(username, password);
  }

  @Override
  public void disconnect() {
    fafServerAccessor.disconnect();
  }

  @Override
  public void setFriends(List<String> friendList) {
    fafServerAccessor.setFriends(friendList);
  }

  @Override
  public void setFoes(List<String> foeList) {
    fafServerAccessor.setFoes(foeList);
  }

  @Override
  public Long getSessionId() {
    return fafServerAccessor.getSessionId();
  }

  @Override
  @Cacheable(CacheNames.LEADERBOARD)
  public CompletableFuture<List<Ranked1v1EntryBean>> getRanked1v1Entries() {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1Entries(), executor);
  }

  @Override
  public CompletableFuture<Ranked1v1Stats> getRanked1v1Stats() {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1Stats(), executor);
  }

  @Override
  public CompletableFuture<Ranked1v1EntryBean> getRanked1v1EntryForPlayer(int playerId) {
    return CompletableFuture.supplyAsync(() -> fafApiAccessor.getRanked1v1EntryForPlayer(playerId), executor);
  }

  @Override
  public void notifyGameEnded() {
    fafServerAccessor.sendGpgMessage(new GameEndedMessage());
  }

  @Override
  public CompletableFuture<GameLaunchMessage> expectRehostCommand() {
    return fafServerAccessor.expectRehostCommand();
  }
}
