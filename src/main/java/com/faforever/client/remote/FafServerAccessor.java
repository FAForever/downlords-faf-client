package com.faforever.client.remote;


import com.faforever.client.game.NewGameInfo;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.IceServer;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.Player.Avatar;
import com.faforever.commons.lobby.ServerMessage;
import javafx.beans.property.ReadOnlyObjectProperty;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Entry class for all communication with the FAF server.
 */
public interface FafServerAccessor {

  @SuppressWarnings("unchecked")
  <T extends ServerMessage> void addEventListener(Class<T> type, Consumer<T> listener);

  ConnectionState getConnectionState();

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletableFuture<LoginSuccessResponse> connectAndLogIn();

  CompletableFuture<GameLaunchResponse> requestHostGame(NewGameInfo newGameInfo);

  CompletableFuture<GameLaunchResponse> requestJoinGame(int gameId, String password);

  void disconnect();

  void reconnect();

  void addFriend(int playerId);

  void addFoe(int playerId);

  void requestMatchmakerInfo();

  CompletableFuture<GameLaunchResponse> startSearchMatchmaker();

  void sendGpgMessage(GpgGameOutboundMessage message);

  void removeFriend(int playerId);

  void removeFoe(int playerId);

  void selectAvatar(URL url);

  CompletableFuture<Collection<Avatar>> getAvailableAvatars();

  void closePlayersGame(int playerId);

  void closePlayersLobby(int playerId);

  void broadcastMessage(String message);

  CompletableFuture<Collection<IceServer>> getIceServers();

  void restoreGameSession(int id);

  void gameMatchmaking(MatchmakingQueue queue, MatchmakerState state);

  void inviteToParty(Player recipient);

  void acceptPartyInvite(Player sender);

  void kickPlayerFromParty(Player kickedPlayer);

  void readyParty();

  void unreadyParty();

  void leaveParty();

  void setPartyFactions(List<Faction> factions);
}
