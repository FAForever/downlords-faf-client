package com.faforever.client.remote;

import com.faforever.client.api.Ranked1v1Stats;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.leaderboard.Ranked1v1EntryBean;
import com.faforever.client.legacy.ConnectionState;
import com.faforever.client.legacy.domain.GameLaunchMessage;
import com.faforever.client.legacy.domain.LoginMessage;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.relay.GpgClientMessage;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface FafService {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo);

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password);

  CompletableFuture<GameLaunchMessage> startSearchRanked1v1(Faction faction, int port);

  void stopSearchingRanked();

  void initConnectivityTest(int port);

  void sendGpgMessage(GpgClientMessage message);

  void expand1v1Search(float radius);

  CompletableFuture<LoginMessage> connectAndLogIn(String username, String password);

  void disconnect();

  void setFriends(List<String> friendList);

  void setFoes(List<String> foeList);

  Long getSessionId();

  CompletableFuture<List<Ranked1v1EntryBean>> getRanked1v1Entries();

  CompletableFuture<Ranked1v1Stats> getRanked1v1Stats();

  CompletableFuture<Ranked1v1EntryBean> getRanked1v1EntryForPlayer(int playerId);
}
