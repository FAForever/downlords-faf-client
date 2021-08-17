package com.faforever.client.remote;

import com.faforever.client.FafClientApplication;
import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.relay.event.CloseGameEvent;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.UidService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.update.Version;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.FafLobbyClient;
import com.faforever.commons.lobby.FafLobbyClient.Config;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameVisibility;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.IceServer;
import com.faforever.commons.lobby.IrcPasswordInfo;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.MessageTarget;
import com.faforever.commons.lobby.NoticeInfo;
import com.faforever.commons.lobby.Player.Avatar;
import com.faforever.commons.lobby.ServerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.nocatch.NoCatch.noCatch;

@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@Slf4j
public class FafServerAccessorImpl implements FafServerAccessor, InitializingBean, DisposableBean {

  private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

  private final NotificationService notificationService;
  private final I18n i18n;
  private final TaskScheduler taskScheduler;
  private final TokenService tokenService;
  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private final UidService uidService;
  private final PreferencesService preferencesService;

  private final FafLobbyClient lobbyClient;

  public FafServerAccessorImpl(NotificationService notificationService, I18n i18n, TaskScheduler taskScheduler, ClientProperties clientProperties, PreferencesService preferencesService, UidService uidService,
                               TokenService tokenService, EventBus eventBus, ObjectMapper objectMapper) {
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.taskScheduler = taskScheduler;
    this.tokenService = tokenService;
    this.eventBus = eventBus;
    this.clientProperties = clientProperties;
    this.preferencesService = preferencesService;
    this.uidService = uidService;

    lobbyClient = new FafLobbyClient(objectMapper);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    eventBus.register(this);
    addEventListener(IrcPasswordInfo.class, this::onIrcPassword);
    addEventListener(NoticeInfo.class, this::onNotice);
  }

  public <T extends ServerMessage> void addEventListener(Class<T> type, Consumer<T> listener) {
    lobbyClient.getEvents().filter(serverMessage -> type.isAssignableFrom(serverMessage.getClass()))
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
    FafLobbyClient.Config config = new Config(
        tokenService.getRefreshedTokenValue(),
        Version.getCurrentVersion(),
        "downlords-faf-client",
        clientProperties.getServer().getHost(),
        clientProperties.getServer().getPort() + 1,
        sessionId -> noCatch(() -> uidService.generate(String.valueOf(sessionId), preferencesService.getFafDataDirectory().resolve("uid.log"))),
        1024 * 1024,
        false
    );

    return lobbyClient.connectAndLogin(config)
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
    log.info("Closing lobby server connection");
    lobbyClient.disconnect();
    connectionState.setValue(ConnectionState.DISCONNECTED);
  }

  public void reconnect() {
    disconnect();
    connectAndLogIn();
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

  public void sendGpgMessage(GpgGameOutboundMessage message) {
    lobbyClient.sendGpgGameMessage(new GpgGameOutboundMessage(message.getCommand(), message.getArgs(), MessageTarget.GAME));
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
    return lobbyClient.getAvailableAvatars().collectList().toFuture();
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
        .collectList()
        .toFuture();
  }

  private void onNotice(NoticeInfo noticeMessage) {
    if (Objects.equals(noticeMessage.getStyle(), "kill")) {
      log.warn("Game close requested by server...");
      notificationService.addNotification(new ImmediateNotification(i18n.get("game.kicked.title"), i18n.get("game.kicked.message", clientProperties.getLinks().get("linksRules")), Severity.WARN, Collections.singletonList(new DismissAction(i18n))));
      eventBus.post(new CloseGameEvent());
    }

    if (Objects.equals(noticeMessage.getStyle(), "kick")) {
      log.warn("Kicked from lobby, client closing after delay...");
      notificationService.addNotification(new ImmediateNotification(i18n.get("server.kicked.title"), i18n.get("server.kicked.message", clientProperties.getLinks().get("linksRules")), Severity.WARN, Collections.singletonList(new DismissAction(i18n))));
      taskScheduler.scheduleWithFixedDelay(Platform::exit, Duration.ofSeconds(10));
    }

    if (noticeMessage.getText() == null) {
      return;
    }

    Severity severity;
    String style = noticeMessage.getStyle();
    if (style == null) {
      severity = Severity.INFO;
    } else {
      severity = switch (style) {
        case "error" -> Severity.ERROR;
        case "warning" -> Severity.WARN;
        default -> Severity.INFO;
      };
    }
    notificationService.addServerNotification(new ImmediateNotification(i18n.get("messageFromServer"), noticeMessage.getText(), severity,
        Collections.singletonList(new DismissAction(i18n))));
  }

  private void onIrcPassword(IrcPasswordInfo ircPasswordInfo) {
    eventBus.post(ircPasswordInfo);
  }

  public void restoreGameSession(int id) {
    lobbyClient.restoreGameSession(id);
  }

  public void gameMatchmaking(MatchmakingQueue queue, MatchmakerState state) {
    lobbyClient.gameMatchmaking(queue.getTechnicalName(), state);
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
