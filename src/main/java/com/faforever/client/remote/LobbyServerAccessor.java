package com.faforever.client.remote;

import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.legacy.UidService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage.IceServer;
import com.faforever.client.remote.domain.outbound.gpg.GpgOutboundMessage;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.update.Version;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.FafLobbyClient;
import com.faforever.commons.lobby.FafLobbyClient.Config;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameVisibility;
import com.faforever.commons.lobby.IrcPasswordInfo;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.ServerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.github.nocatch.NoCatch.noCatch;

@Service
@Slf4j
@RequiredArgsConstructor
public class LobbyServerAccessor implements InitializingBean, DisposableBean {

  private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

  private final ClientProperties clientProperties;
  private final PreferencesService preferencesService;
  private final UidService uidService;
  private final TokenService tokenService;
  private final EventBus eventBus;
  private final ObjectMapper objectMapper;

  private FafLobbyClient lobbyClient;

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
    addEventListener(IrcPasswordInfo.class, this::onIrcPassword);
  }

  public <T extends ServerMessage> void addEventListener(Class<T> type, Consumer<T> listener) {
    lobbyClient.getEvents().filter(serverMessage -> serverMessage.getClass() == type)
        .cast(type)
        .doOnNext(listener)
        .onErrorContinue((throwable, message) -> log.warn("Could not process listener for `{}`", message, throwable))
        .subscribe();
  }

  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  public CompletableFuture<LoginSuccessResponse> connectAndLogIn() {
    connectionState.setValue(ConnectionState.CONNECTING);

    Config config = new Config(
        Version.getCurrentVersion(),
        "downlords-faf-client",
        clientProperties.getServer().getHost(),
        clientProperties.getServer().getPort() + 1,
        sessionId -> noCatch(() -> uidService.generate(String.valueOf(sessionId), preferencesService.getFafDataDirectory().resolve("uid.log"))),
        1024 * 1024,
        false
    );
    lobbyClient = new FafLobbyClient(config, objectMapper);

    return lobbyClient.connectAndLogin(tokenService.getRefreshedTokenValue())
        .doOnNext(loginMessage -> connectionState.setValue(ConnectionState.CONNECTED))
        .toFuture();
  }

  public CompletableFuture<GameLaunchResponse> requestHostGame(NewGameInfo newGameInfo) {
    return lobbyClient.requestHostGame(
        newGameInfo.getTitle(),
        newGameInfo.getMap(),
        newGameInfo.getFeaturedMod().getTechnicalName(),
        GameVisibility.valueOf(newGameInfo.getGameVisibility().name()),
        newGameInfo.getPassword(),
        newGameInfo.getRatingMin(),
        newGameInfo.getRatingMax(),
        newGameInfo.getEnforceRatingRange()
    )
        .toFuture();
  }

  public CompletableFuture<GameLaunchResponse> requestJoinGame(int gameId, String password) {
    return lobbyClient.requestJoinGame(gameId, password)
        .toFuture();
  }

  public void disconnect() {
    lobbyClient.disconnect();
    connectionState.setValue(ConnectionState.DISCONNECTED);
  }

  public void reconnect() {
    lobbyClient.connectAndLogin(tokenService.getRefreshedTokenValue()).subscribe();
  }

  public void addFriend(int playerId) {
    lobbyClient.addFriend(playerId);
  }

  public void addFoe(int playerId) {
    lobbyClient.addFoe(playerId);
  }

  public void requestMatchmakerInfo() {
    lobbyClient.requestMatchmakerInfo();
  }

  public CompletableFuture<GameLaunchResponse> startSearchMatchmaker() {
    return lobbyClient.getEvents()
        .filter(event -> event instanceof GameLaunchResponse)
        .next()
        .cast(GameLaunchResponse.class)
        .toFuture();
  }

  public void stopSearchMatchmaker() {
    // Not implemented
  }

  public void sendGpgMessage(GpgOutboundMessage message) {
    lobbyClient.sendGpgGameMessage(
        new com.faforever.commons.lobby.GpgGameOutboundMessage(
            message.getCommand(),
            message.getArgs()
        )
    );
  }

  public void removeFriend(int playerId) {
    lobbyClient.removeFriend(playerId);
  }

  public void removeFoe(int playerId) {
    lobbyClient.removeFoe(playerId);
  }

  public void selectAvatar(URL url) {
    lobbyClient.selectAvatar(Optional.ofNullable(url).map(URL::toString).orElse(null));
  }

  public CompletableFuture<List<Avatar>> getAvailableAvatars() {
    return lobbyClient.getAvailableAvatars()
        .map(avatars -> avatars.stream()
            .map(avatar ->
                new Avatar(avatar.getUrl(), avatar.getDescription()))
            .collect(Collectors.toList()))
        .toFuture();
  }

  public void closePlayersGame(int playerId) {
    lobbyClient.closePlayerGame(playerId);
  }

  public void closePlayersLobby(int playerId) {
    lobbyClient.closePlayerLobby(playerId);
  }

  public void broadcastMessage(String message) {
    lobbyClient.broadcastMessage(message);
  }

  public CompletableFuture<List<IceServer>> getIceServers() {
    return lobbyClient.getIceServers()
        .map(iceServers -> iceServers.stream()
            .map(iceServer ->
                new IceServer(
                    iceServer.getUrl(),
                    (List<String>) iceServer.getUrls(),
                    iceServer.getUsername(),
                    iceServer.getCredentialType(),
                    iceServer.getCredential()
                ))
            .collect(Collectors.toList())
        )
        .toFuture();
  }

  private void onIrcPassword(IrcPasswordInfo ircPasswordInfo) {
    eventBus.post(ircPasswordInfo);
  }

  public void restoreGameSession(int id) {
    lobbyClient.restoreGameSession(id);
  }

  public void gameMatchmaking(MatchmakingQueue queue, MatchmakingState state) {
    lobbyClient.gameMatchmaking(queue.getTechnicalName(), MatchmakerState.valueOf(state.name()));
  }

  public void inviteToParty(Player recipient) {
    lobbyClient.inviteToParty(recipient.getId());
  }

  public void acceptPartyInvite(Player sender) {
    lobbyClient.acceptPartyInvite(sender.getId());
  }

  public void kickPlayerFromParty(Player kickedPlayer) {
    lobbyClient.kickPlayerFromParty(kickedPlayer.getId());
  }

  public void readyParty() {
    lobbyClient.readyParty();
  }

  public void unreadyParty() {
    lobbyClient.unreadyParty();
  }

  public void leaveParty() {
    lobbyClient.leaveParty();
  }

  public void setPartyFactions(List<Faction> factions) {
    lobbyClient.setPartyFactions(new HashSet<>(factions));
  }

  @Override
  public void destroy() {
    disconnect();
  }

}
