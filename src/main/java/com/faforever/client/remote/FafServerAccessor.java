package com.faforever.client.remote;

import com.faforever.client.api.TokenRetriever;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.domain.server.MatchmakerQueueInfo;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.exception.UIDException;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.UidService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ServerNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.update.Version;
import com.faforever.commons.lobby.ConnectionStatus;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.FafLobbyClient;
import com.faforever.commons.lobby.FafLobbyClient.Config;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameVisibility;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.LoginException;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.MessageTarget;
import com.faforever.commons.lobby.NoticeInfo;
import com.faforever.commons.lobby.Player;
import com.faforever.commons.lobby.Player.Avatar;
import com.faforever.commons.lobby.ServerMessage;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.context.Phased;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Lazy
@Component
@Slf4j
@RequiredArgsConstructor
public class FafServerAccessor implements InitializingBean, DisposableBean, Lifecycle, Phased {

  private final ReadOnlyObjectWrapper<ConnectionState> connectionState = new ReadOnlyObjectWrapper<>(
      ConnectionState.DISCONNECTED);

  private final NotificationService notificationService;
  private final I18n i18n;
  private final TaskScheduler taskScheduler;
  private final TokenRetriever tokenRetriever;
  private final UidService uidService;
  private final ClientProperties clientProperties;
  private final FafLobbyClient lobbyClient;
  @Qualifier("userWebClient")
  private final ObjectFactory<WebClient> userWebClientFactory;

  private boolean autoReconnect;
  @Getter
  private boolean running;
  @Getter
  @Setter
  private int timeoutLoginReconnectSeconds;

  @Override
  public void afterPropertiesSet() throws Exception {
    start();
  }

  @Override
  public void start() {
    if (!isRunning()) {
      getEvents(NoticeInfo.class).doOnNext(this::onNotice)
                                 .doOnError(throwable -> log.error("Error processing notice", throwable))
                                 .retry()
                                 .subscribe();

      setPingIntervalSeconds(25);
      setTimeoutLoginReconnectSeconds(30);

      lobbyClient.getConnectionStatus()
                 .map(connectionStatus -> switch (connectionStatus) {
                   case DISCONNECTED -> ConnectionState.DISCONNECTED;
                   case CONNECTING -> ConnectionState.CONNECTING;
                   case CONNECTED -> ConnectionState.CONNECTED;
                 })
                 .doOnNext(connectionState::set)
                 .doOnError(throwable -> log.error("Error processing connection status", throwable))
                 .retry()
                 .subscribe();

      connectionState.subscribe((oldValue, newValue) -> {
        if (autoReconnect && oldValue == ConnectionState.CONNECTED && newValue == ConnectionState.DISCONNECTED) {
          connectAndLogIn().subscribe();
        }
      });
      running = true;
    }
  }

  @Override
  public void destroy() {
    stop();
  }

  @Override
  public void stop() {
    disconnect();
    running = false;
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE;
  }

  public <T extends ServerMessage> Flux<T> getEvents(Class<T> type) {
    return lobbyClient.getEvents().ofType(type);
  }

  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState.getReadOnlyProperty();
  }

  public Mono<Player> connectAndLogIn() {
    autoReconnect = true;
    return userWebClientFactory.getObject()
                               .get()
                               .uri("/lobby/access")
                               .retrieve().bodyToMono(HmacAccess.class).map(HmacAccess::accessUrl)
                               .zipWith(tokenRetriever.getRefreshedTokenValue())
                               .map(TupleUtils.function(
                                   (lobbyUrl, token) -> new Config(token, Version.getCurrentVersion(),
                                                                   clientProperties.getUserAgent(), lobbyUrl,
                                                                   this::tryGenerateUid, 1024 * 1024, false)))
                               .flatMap(lobbyClient::connectAndLogin)
                               .timeout(Duration.ofSeconds(timeoutLoginReconnectSeconds))
                               .retryWhen(createRetrySpec(clientProperties.getServer()));
  }

  private Retry createRetrySpec(Server server) {
    return Retry.fixedDelay(server.getRetryAttempts(), Duration.ofSeconds(server.getRetryDelaySeconds()))
                .filter(exception -> !(exception instanceof LoginException))
                .doBeforeRetry(
                    retry -> log.warn("Could not reach server retrying: Attempt #{} of {}", retry.totalRetries(),
                                      server.getRetryAttempts(), retry.failure()))
                .onRetryExhaustedThrow((spec, retrySignal) -> new LoginException(
                    "Could not reach server after %d attempts".formatted(spec.maxAttempts), retrySignal.failure()));
  }


  private String tryGenerateUid(Long sessionId) {
    try {
      return uidService.generate(String.valueOf(sessionId));
    } catch (IOException e) {
      throw new UIDException("Cannot generate UID", e, "uid.generate.error");
    }
  }

  public CompletableFuture<GameLaunchResponse> requestHostGame(NewGameInfo newGameInfo) {
    return lobbyClient.requestHostGame(newGameInfo.title(), newGameInfo.map(),
                                       newGameInfo.featuredModName(),
                                       GameVisibility.valueOf(newGameInfo.gameVisibility().name()),
                                       newGameInfo.password(), newGameInfo.ratingMin(),
                                       newGameInfo.ratingMax(), newGameInfo.enforceRatingRange()).toFuture();
  }

  public CompletableFuture<GameLaunchResponse> requestJoinGame(int gameId, String password) {
    return lobbyClient.requestJoinGame(gameId, password).toFuture();
  }

  public void disconnect() {
    autoReconnect = false;
    log.info("Closing lobby server connection");
    lobbyClient.disconnect();
  }

  public Mono<Player> reconnect() {
    return lobbyClient.getConnectionStatus()
                      .filter(ConnectionStatus.DISCONNECTED::equals)
                      .next()
                      .take(Duration.ofSeconds(5))
                      .then(connectAndLogIn())
                      .doOnSubscribe(ignored -> disconnect());
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
                      .ofType(GameLaunchResponse.class)
                      .next()
                      .toFuture();
  }

  public void sendGpgMessage(GpgGameOutboundMessage message) {
    lobbyClient.sendGpgGameMessage(
        new GpgGameOutboundMessage(message.getCommand(), message.getArgs(), MessageTarget.GAME));
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
    if (Objects.equals(noticeMessage.getStyle(), "kick")) {
      log.info("Kicked from lobby, client closing after delay");
      notificationService.addNotification(new ImmediateNotification(i18n.get("server.kicked.title"),
                                                                    i18n.get("server.kicked.message",
                                                                             clientProperties.getLinks()
                                                                                             .get("linksRules")),
                                                                    Severity.WARN, Collections.singletonList(
          new DismissAction(i18n))));
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
    notificationService.addNotification(
        new ServerNotification(i18n.get("messageFromServer"), noticeMessage.getText(), severity,
                               Collections.singletonList(new DismissAction(i18n))));
  }

  public void restoreGameSession(int id) {
    lobbyClient.restoreGameSession(id);
  }

  public void gameMatchmaking(MatchmakerQueueInfo queue, MatchmakerState state) {
    lobbyClient.gameMatchmaking(queue.getTechnicalName(), state);
  }

  public void inviteToParty(PlayerInfo recipient) {
    lobbyClient.inviteToParty(recipient.getId());
  }

  public void acceptPartyInvite(PlayerInfo sender) {
    lobbyClient.acceptPartyInvite(sender.getId());
  }

  public void kickPlayerFromParty(PlayerInfo kickedPlayer) {
    lobbyClient.kickPlayerFromParty(kickedPlayer.getId());
  }

  public void sendReady(String requestId) {
    lobbyClient.sendReady(requestId);
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
}
