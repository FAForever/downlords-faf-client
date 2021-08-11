package com.faforever.client.remote;


import com.faforever.client.game.NewGameInfo;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.inbound.InboundMessage;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage.IceServer;
import com.faforever.client.remote.domain.inbound.faf.LoginMessage;
import com.faforever.client.remote.domain.outbound.gpg.GpgOutboundMessage;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.commons.api.dto.Faction;
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
  <T extends InboundMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener);

  @SuppressWarnings("unchecked")
  <T extends InboundMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener);

  ConnectionState getConnectionState();

  ReadOnlyObjectProperty<ConnectionState> connectionStateProperty();

  CompletableFuture<LoginMessage> connectAndLogin();

  CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo);

  CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password);

  void disconnect();

  void reconnect();

  void addFriend(int playerId);

  void addFoe(int playerId);

  void requestMatchmakerInfo();

  CompletableFuture<GameLaunchMessage> startSearchMatchmaker();

  void stopSearchMatchmaker();

  void sendGpgMessage(GpgOutboundMessage message);

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

  void gameMatchmaking(MatchmakingQueue queue, MatchmakingState state);

  void gameMatchmakingReady();

  void inviteToParty(Player recipient);

  void acceptPartyInvite(Player sender);

  void kickPlayerFromParty(Player kickedPlayer);

  void readyParty();

  void unreadyParty();

  void leaveParty();

  void setPartyFactions(List<Faction> factions);
}
