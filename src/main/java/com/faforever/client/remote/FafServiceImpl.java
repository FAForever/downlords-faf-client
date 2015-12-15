package com.faforever.client.remote;

import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.LeaderboardEntryBean;
import com.faforever.client.legacy.ConnectionState;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.relay.GpgClientMessage;
import javafx.beans.property.ReadOnlyObjectProperty;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FafServiceImpl implements FafService {

  @Resource
  FafClient fafClient;

  @Override
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafClient.addOnMessageListener(type, listener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    fafClient.removeOnMessageListener(type, listener);
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo, @Nullable InetSocketAddress relayAddress, int externalPort) {
    return fafClient.requestHostGame(newGameInfo, relayAddress, externalPort);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return fafClient.connectionStateProperty();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password, @Nullable InetSocketAddress relayAddress, int externalPort) {
    return fafClient.requestJoinGame(gameId, password, relayAddress, externalPort);
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port) {
    return fafClient.startSearchRanked1v1(faction, port);
  }

  @Override
  public void stopSearchingRanked() {
    fafClient.stopSearchingRanked();
  }

  @Override
  public void initConnectivityTest(int port) {
    fafClient.initConnectivityTest(port);
  }

  @Override
  public void sendGpgMessage(GpgClientMessage message) {
    fafClient.sendGpgMessage(message);
  }

  @Override
  public void expand1v1Search(float radius) {
    fafClient.expand1v1Search(radius);
  }

  @Override
  public CompletableFuture<List<LeaderboardEntryBean>> requestLeaderboardEntries() {
    return fafClient.requestLeaderboardEntries();
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    return fafClient.connectAndLogIn(username, password);
  }

  @Override
  public void disconnect() {
    fafClient.disconnect();
  }

  @Override
  public void setFriends(List<String> friendList) {
    fafClient.setFriends(friendList);
  }

  @Override
  public void setFoes(List<String> foeList) {
    fafClient.setFoes(foeList);
  }

  @Override
  public Long getSessionId() {
    return fafClient.getSessionId();
  }
}
