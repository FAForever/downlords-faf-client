package com.faforever.client.remote;

import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.ServerMessage;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Entry class for all communication with the FAF server.
 */
public interface FafServerAccessor {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletableFuture<LoginMessage> connectAndLogIn(String username, String password);

  CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo);

  CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password);

  void disconnect();

  void reconnect();

  void addFriend(int playerId);

  void addFoe(int playerId);

  void requestMatchmakerInfo();

  CompletableFuture<GameLaunchMessage> startSearchLadder1v1(Faction faction);

  void stopSearchingRanked();

  void sendGpgMessage(GpgGameMessage message);

  void removeFriend(int playerId);

  void removeFoe(int playerId);

  void selectAvatar(URL url);

  List<Avatar> getAvailableAvatars();

  void banPlayer(int playerId, int duration, PeriodType periodType, String reason);

  void closePlayersGame(int playerId);

  void closePlayersLobby(int playerId);

  void broadcastMessage(String message);

  CompletableFuture<List<IceServer>> getIceServers();

  void restoreGameSession(int id);

  void ping();
}
