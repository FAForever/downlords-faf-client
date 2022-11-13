package com.faforever.client.remote;

import com.faforever.client.api.TokenService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.UIDException;
import com.faforever.client.fa.relay.event.CloseGameEvent;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.UidService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.Version;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.commons.lobby.ConnectionStatus;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.FafLobbyClient;
import com.faforever.commons.lobby.FafLobbyClient.Config;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameVisibility;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
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
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Lazy
@Component
@Slf4j
public class FafServerAccessor implements InitializingBean, DisposableBean {

  private final ReadOnlyObjectWrapper<ConnectionState> connectionState = new ReadOnlyObjectWrapper<>(ConnectionState.DISCONNECTED);

  private final NotificationService notificationService;
  private final I18n i18n;
  private final TaskScheduler taskScheduler;
  private final TokenService tokenService;
  private final EventBus eventBus;
  private final ClientProperties clientProperties;
  private final UidService uidService;
  private final PreferencesService preferencesService;

  private final FafLobbyClient lobbyClient;

  private CompletableFuture<LoginSuccessResponse> loginFuture;

  public FafServerAccessor(NotificationService notificationService, I18n i18n, TaskScheduler taskScheduler, ClientProperties clientProperties, PreferencesService preferencesService, UidService uidService,
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

    setPingIntervalSeconds(25);

    lobbyClient.getConnectionStatus().doOnNext(connectionStatus -> {
      switch (connectionStatus) {
        case DISCONNECTED -> connectionState.set(ConnectionState.DISCONNECTED);
        case CONNECTING -> connectionState.set(ConnectionState.CONNECTING);
        case CONNECTED -> connectionState.set(ConnectionState.CONNECTED);
      }
    }).subscribe();
  }

  private void onInternalLoginFailed(Throwable throwable) {
    eventBus.post(new LogOutRequestEvent());
    throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);

    log.error("Could not reconnect to server", throwable);
    notificationService.addImmediateErrorNotification(throwable, "login.failed");
  }

  public <T extends ServerMessage> void addEventListener(Class<T> type, Consumer<T> listener) {
    lobbyClient.getEvents().filter(serverMessage -> type.isAssignableFrom(serverMessage.getClass()))
        .cast(type)
        .flatMap(message -> Mono.fromRunnable(() -> listener.accept(message))
            .onErrorResume(throwable -> {
              log.error("Could not process listener for `{}`", message, throwable);
              return Mono.empty();
            }))
        .subscribe();
  }

  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState.getReadOnlyProperty();
  }

  public CompletableFuture<LoginSuccessResponse> connectAndLogIn() {
    if (loginFuture == null || (loginFuture.isDone() && connectionState.get() != ConnectionState.CONNECTED)) {
      lobbyClient.setAutoReconnect(false);
      Server server = clientProperties.getServer();
      Config config = new Config(
          tokenService.getRefreshedTokenValue(),
          Version.getCurrentVersion(),
          clientProperties.getUserAgent(),
          clientProperties.getServer().getHost(),
          clientProperties.getServer().getPort() + 1,
          sessionId -> {
            try {
              return uidService.generate(String.valueOf(sessionId));
            } catch (IOException e) {
              throw new UIDException("Cannot generate UID", e, "uid.generate.error");
            }
          },
          1024 * 1024,
          false,
          server.getRetryAttempts(),
          server.getRetryDelaySeconds()
      );

      loginFuture = lobbyClient.connectAndLogin(config)
          .doOnNext(loginMessage -> lobbyClient.setAutoReconnect(true)).toFuture();
    }
    return loginFuture;
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
    return lobbyClient.requestJoinGame(gameId, password).toFuture();
  }

  public void disconnect() {
    loginFuture.cancel(true);
    log.info("Closing lobby server connection");
    lobbyClient.disconnect();
  }

  public void reconnect() {
    lobbyClient.getConnectionStatus()
        .filter(ConnectionStatus.DISCONNECTED::equals)
        .next()
        .take(Duration.ofSeconds(5))
        .doOnSuccess(ignored -> connectAndLogIn().exceptionally(throwable -> {
          onInternalLoginFailed(throwable);
          return null;
        })).subscribe();
    disconnect();
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

  private void onNotice(NoticeInfo noticeMessage) {
    if (Objects.equals(noticeMessage.getStyle(), "kill")) {
      log.info("Game close requested by server");
      notificationService.addNotification(new ImmediateNotification(i18n.get("game.kicked.title"), i18n.get("game.kicked.message", clientProperties.getLinks().get("linksRules")), Severity.WARN, Collections.singletonList(new DismissAction(i18n))));
      eventBus.post(new CloseGameEvent());
    }

    if (Objects.equals(noticeMessage.getStyle(), "kick")) {
      log.info("Kicked from lobby, client closing after delay");
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

  public void gameMatchmaking(MatchmakerQueueBean queue, MatchmakerState state) {
    lobbyClient.gameMatchmaking(queue.getTechnicalName(), state);
  }

  public void inviteToParty(PlayerBean recipient) {
    lobbyClient.inviteToParty(recipient.getId());
  }

  public void acceptPartyInvite(PlayerBean sender) {
    lobbyClient.acceptPartyInvite(sender.getId());
  }

  public void kickPlayerFromParty(PlayerBean kickedPlayer) {
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

  public void notifyGameEnded() {
    sendGpgMessage(GpgGameOutboundMessage.Companion.gameStateMessage("Ended"));
  }

  public void sendIceMessage(int remotePlayerId, Object message) {
    sendGpgMessage(GpgGameOutboundMessage.Companion.iceMessage(remotePlayerId, message));
  }

  public void setPingIntervalSeconds(int pingIntervalSeconds) {
    lobbyClient.setMinPingIntervalSeconds(pingIntervalSeconds);
  }

  @Override
  public void destroy() {
    disconnect();
  }
}
