package com.faforever.client.remote;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.Faction;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.UidService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.remote.gson.ServerMessageMapper;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.commons.lobby.FafLobbyClient;
import com.faforever.commons.lobby.FafLobbyClient.Config;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameVisibility;
import com.faforever.commons.lobby.MatchmakerState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class ReactiveFafServerAccessor implements FafServerAccessor {

  private final HashMap<Class<? extends ServerMessage>, Collection<Consumer<ServerMessage>>> messageListeners = new HashMap<>();

  private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

  private final ServerMessageMapper serverMessageMapper;
  private final ObjectMapper objectMapper;
  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final UidService uidService;

  private FafLobbyClient lobbyClient;

  @Override
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    if (!messageListeners.containsKey(type)) {
      messageListeners.put(type, new LinkedList<>());
    }
    messageListeners.get(type).add((Consumer<ServerMessage>) listener);
  }

  @Override
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.getOrDefault(type, List.of()).remove(listener);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    connectionState.setValue(ConnectionState.CONNECTING);
    password = Hashing.sha256().hashString(password, UTF_8).toString();

    FafLobbyClient.Config config = new Config(
//        clientProperties.getServer().getHost(),
//        clientProperties.getServer().getPort(),
        "test.faforever.com",
        8002,
        username,
        password,
        "127.0.0.1",
        sessionId -> noCatch(() -> uidService.generate(String.valueOf(sessionId), preferencesService.getFafDataDirectory().resolve("uid.log"))),
        8096,
        false
    );
    lobbyClient = new FafLobbyClient(config, objectMapper);

    // Emulate the legacy server messages for backwards compatibility
    lobbyClient.getEvents()
        .flatMap(message -> {
          try {
            return Mono.just(serverMessageMapper.jackson2Gson(message));
          } catch (Exception e) {
            log.error("Failed to map jackson message: {}", message);
            // swallow the error and keep the Flux alive
            return Mono.empty();
          }
        })
        .cast(ServerMessage.class)
        .doOnNext(legacyLobbyMessage ->
            messageListeners.getOrDefault(legacyLobbyMessage.getClass(), Collections.emptyList())
                .forEach(consumer -> consumer.accept(legacyLobbyMessage)))
        .onErrorResume(throwable -> {
          log.error("Failed processing lobby event", throwable);
          // swallow the error and keep the Flux alive
          return Mono.empty();
        })
        .subscribe();

    return lobbyClient.connectAndLogin()
        .map(serverMessageMapper::<LoginMessage>jackson2Gson)
        .doOnNext(loginMessage -> connectionState.setValue(ConnectionState.CONNECTED))
        .toFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return lobbyClient.requestHostGame(
        newGameInfo.getTitle(),
        newGameInfo.getMap(),
        newGameInfo.getFeaturedMod().getTechnicalName(),
        GameVisibility.valueOf(newGameInfo.getGameVisibility().name()),
        newGameInfo.getPassword()
    )
        .map(serverMessageMapper::<GameLaunchMessage>jackson2Gson)
        .toFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return lobbyClient.requestJoinGame(gameId, password)
        .map(serverMessageMapper::<GameLaunchMessage>jackson2Gson)
        .toFuture();
  }

  @Override
  public void disconnect() {
    lobbyClient.disconnect();
    connectionState.setValue(ConnectionState.DISCONNECTED);
  }

  @Override
  public void reconnect() {
    lobbyClient.connectAndLogin().subscribe();
  }

  @Override
  public void addFriend(int playerId) {
    lobbyClient.addFriend(playerId);
  }

  @Override
  public void addFoe(int playerId) {
    lobbyClient.addFoe(playerId);
  }

  @Override
  public void requestMatchmakerInfo() {
    lobbyClient.requestMatchmakerInfo();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchMatchmaker() {
    return lobbyClient.getEvents()
        .filter(event -> event instanceof GameLaunchResponse)
        .next()
        .cast(GameLaunchResponse.class)
        .map(serverMessageMapper::<GameLaunchMessage>jackson2Gson)
        .toFuture();
  }

  @Override
  public void stopSearchMatchmaker() {
    // Not implemented
  }

  @Override
  public void sendGpgMessage(GpgGameMessage message) {
    lobbyClient.sendGpgGameMessage(
        new com.faforever.commons.lobby.GpgGameOutboundMessage(
            message.getCommand().getString(),
            message.getArgs()
        )
    );
  }

  @Override
  public void removeFriend(int playerId) {
    lobbyClient.removeFriend(playerId);
  }

  @Override
  public void removeFoe(int playerId) {
    lobbyClient.removeFoe(playerId);
  }

  @Override
  public void selectAvatar(URL url) {
    // Not implemented, use API instead
  }

  @Override
  public List<Avatar> getAvailableAvatars() {
    // Not implemented
    return List.of();
  }

  @Override
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {
    // Not implemented, use API instead
  }

  @Override
  public void closePlayersGame(int playerId) {
    lobbyClient.closePlayerGame(playerId);
  }

  @Override
  public void closePlayersLobby(int playerId) {
    lobbyClient.closePlayerLobby(playerId);
  }

  @Override
  public void broadcastMessage(String message) {
    lobbyClient.broadcastMessage(message);
  }

  @Override
  public CompletableFuture<List<IceServer>> getIceServers() {
    return lobbyClient.getIceServers()
        .map(iceServers -> iceServers.stream()
            .map(iceServer ->
                new IceServer()
                    .setUrl(iceServer.getUrl())
                    .setUrls((List<String>) iceServer.getUrls())
                    .setUsername(iceServer.getUsername())
                    .setCredentialType(iceServer.getCredentialType())
                    .setCredential(iceServer.getCredential())
            )
            .collect(Collectors.toList())
        )
        .toFuture();
  }

  @Override
  public void restoreGameSession(int id) {
    // Not implemented
  }

  @Override
  public void ping() {
    // Not implemented
  }

  @Override
  public void gameMatchmaking(MatchmakingQueue queue, MatchmakingState state) {
    lobbyClient.gameMatchmaking(queue.getQueueName(), MatchmakerState.valueOf(state.name()));
  }

  @Override
  public void inviteToParty(Player recipient) {
    lobbyClient.inviteToParty(recipient.getId());
  }

  @Override
  public void acceptPartyInvite(Player sender) {
    lobbyClient.acceptPartyInvite(sender.getId());
  }

  @Override
  public void kickPlayerFromParty(Player kickedPlayer) {
    lobbyClient.kickPlayerFromParty(kickedPlayer.getId());
  }

  @Override
  public void readyParty() {
    lobbyClient.readyParty(true);
  }

  @Override
  public void unreadyParty() {
    lobbyClient.readyParty(false);
  }

  @Override
  public void leaveParty() {
    lobbyClient.leaveParty();
  }

  @Override
  public void setPartyFactions(List<Faction> factions) {
    lobbyClient.setPartyFactions(
        factions.stream()
            .map(faction -> com.faforever.commons.lobby.Faction.valueOf(faction.name()))
            .collect(Collectors.toSet())
    );
  }

}
