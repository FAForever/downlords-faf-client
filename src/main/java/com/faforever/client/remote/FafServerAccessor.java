package com.faforever.client.remote;

import com.faforever.client.api.TokenRetriever;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.UIDException;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.UidService;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.function.Consumer;

@Lazy
@Component
@Slf4j
@RequiredArgsConstructor
public class FafServerAccessor implements InitializingBean, DisposableBean {

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

  private boolean autoReconnect = false;

  @Override
  public void afterPropertiesSet() throws Exception {
    getEvents(NoticeInfo.class)
        .doOnNext(this::onNotice)
        .doOnError(throwable -> log.error("Error processing notice", throwable))
        .retry()
        .subscribe();

    setPingIntervalSeconds(25);

    lobbyClient.getConnectionStatus().retry().map(connectionStatus -> switch (connectionStatus) {
      case DISCONNECTED -> ConnectionState.DISCONNECTED;
      case CONNECTING -> ConnectionState.CONNECTING;
      case CONNECTED -> ConnectionState.CONNECTED;
    }).subscribe(connectionState::set, throwable -> log.error("Error processing connection status", throwable));

    connectionState.subscribe((oldValue, newValue) -> {
      if (autoReconnect && oldValue == ConnectionState.CONNECTED && newValue == ConnectionState.DISCONNECTED) {
        connectAndLogIn().subscribe();
      }
    });
  }

  public <T extends ServerMessage> Flux<T> getEvents(Class<T> type) {
    return lobbyClient.getEvents().ofType(type);
  }

  /**
   * @deprecated should use {@link FafServerAccessor#getEvents(Class)} instead
   */
  @Deprecated
  public <T extends ServerMessage> void addEventListener(Class<T> type, Consumer<T> listener) {
    lobbyClient.getEvents()
               .ofType(type)
               .flatMap(message -> Mono.fromRunnable(() -> listener.accept(message)).onErrorResume(throwable -> {
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

  public Mono<Player> connectAndLogIn() {
    autoReconnect = true;
    return userWebClientFactory.getObject()
                               .get()
                               .uri("/lobby/access")
                               .retrieve()
                               .bodyToMono(LobbyAccess.class)
                               .map(LobbyAccess::accessUrl)
                               .zipWith(tokenRetriever.getRefreshedTokenValue())
                               .map(TupleUtils.function(
                                   (lobbyUrl, token) -> new Config(token, Version.getCurrentVersion(),
                                                                   clientProperties.getUserAgent(), lobbyUrl,
                                                                   this::tryGenerateUid, 1024 * 1024, false)))
                               .flatMap(lobbyClient::connectAndLogin)
                               .timeout(Duration.ofSeconds(30))
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
    return lobbyClient.requestHostGame(newGameInfo.getTitle(), newGameInfo.getMap(),
                                       newGameInfo.getFeaturedMod().getTechnicalName(),
                                       GameVisibility.valueOf(newGameInfo.getGameVisibility().name()),
                                       newGameInfo.getPassword(), newGameInfo.getRatingMin(),
                                       newGameInfo.getRatingMax(), newGameInfo.getEnforceRatingRange()).toFuture();
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
                      .filter(event -> event instanceof GameLaunchResponse)
                      .next()
                      .cast(GameLaunchResponse.class)
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
    notificationService.addServerNotification(
        new ImmediateNotification(i18n.get("messageFromServer"), noticeMessage.getText(), severity,
                                  Collections.singletonList(new DismissAction(i18n))));
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

  @Override
  public void destroy() {
    disconnect();
  }
}
