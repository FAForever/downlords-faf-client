package com.faforever.client.remote;

import com.faforever.client.FafClientApplication;
import com.faforever.client.api.TokenService;
import com.faforever.client.config.CacheNames;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Server;
import com.faforever.client.fa.relay.event.CloseGameEvent;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.UidService;
import com.faforever.client.login.LoginFailedException;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.CopyErrorAction;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.GetHelpAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.SerializableMessage;
import com.faforever.client.remote.domain.ServerCommand;
import com.faforever.client.remote.domain.inbound.InboundMessage;
import com.faforever.client.remote.domain.inbound.faf.AuthenticationFailedMessage;
import com.faforever.client.remote.domain.inbound.faf.AvatarMessage;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage.IceServer;
import com.faforever.client.remote.domain.inbound.faf.IrcPasswordServerMessage;
import com.faforever.client.remote.domain.inbound.faf.LoginMessage;
import com.faforever.client.remote.domain.inbound.faf.NoticeMessage;
import com.faforever.client.remote.domain.inbound.faf.SessionMessage;
import com.faforever.client.remote.domain.inbound.gpg.GpgInboundMessage;
import com.faforever.client.remote.domain.outbound.faf.AcceptPartyInviteMessage;
import com.faforever.client.remote.domain.outbound.faf.AddFoeMessage;
import com.faforever.client.remote.domain.outbound.faf.AddFriendMessage;
import com.faforever.client.remote.domain.outbound.faf.BanPlayerMessage;
import com.faforever.client.remote.domain.outbound.faf.ClosePlayersFAMessage;
import com.faforever.client.remote.domain.outbound.faf.ClosePlayersLobbyMessage;
import com.faforever.client.remote.domain.outbound.faf.GameMatchmakingMessage;
import com.faforever.client.remote.domain.outbound.faf.HostGameMessage;
import com.faforever.client.remote.domain.outbound.faf.InitSessionMessage;
import com.faforever.client.remote.domain.outbound.faf.InviteToPartyMessage;
import com.faforever.client.remote.domain.outbound.faf.JoinGameMessage;
import com.faforever.client.remote.domain.outbound.faf.KickPlayerFromPartyMessage;
import com.faforever.client.remote.domain.outbound.faf.LeavePartyMessage;
import com.faforever.client.remote.domain.outbound.faf.ListIceServersMessage;
import com.faforever.client.remote.domain.outbound.faf.ListPersonalAvatarsMessage;
import com.faforever.client.remote.domain.outbound.faf.LoginOauthClientMessage;
import com.faforever.client.remote.domain.outbound.faf.MakeBroadcastMessage;
import com.faforever.client.remote.domain.outbound.faf.MatchmakerInfoOutboundMessage;
import com.faforever.client.remote.domain.outbound.faf.PingMessage;
import com.faforever.client.remote.domain.outbound.faf.PongMessage;
import com.faforever.client.remote.domain.outbound.faf.ReadyPartyMessage;
import com.faforever.client.remote.domain.outbound.faf.RemoveFoeMessage;
import com.faforever.client.remote.domain.outbound.faf.RemoveFriendMessage;
import com.faforever.client.remote.domain.outbound.faf.RestoreGameSessionMessage;
import com.faforever.client.remote.domain.outbound.faf.SelectAvatarMessage;
import com.faforever.client.remote.domain.outbound.faf.SetPartyFactionsMessage;
import com.faforever.client.remote.domain.outbound.faf.UnreadyPartyMessage;
import com.faforever.client.remote.domain.outbound.gpg.GpgOutboundMessage;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.serialization.FactionMixin;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.update.Version;
import com.faforever.commons.api.dto.Faction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

@Lazy
@Component
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
@Slf4j
public class FafServerAccessorImpl extends AbstractServerAccessor implements FafServerAccessor,
    InitializingBean, DisposableBean {

  private final ObjectMapper objectMapper = new ObjectMapper()
      .addMixIn(Faction.class, FactionMixin.class)
      .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
  private final HashMap<Class<? extends InboundMessage>, Collection<Consumer<InboundMessage>>> messageListeners = new HashMap<>();

  private final PreferencesService preferencesService;
  private final UidService uidService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final TokenService tokenService;
  private final ReportingService reportingService;
  private final TaskScheduler taskScheduler;
  private final EventBus eventBus;
  private final ReconnectTimerService reconnectTimerService;

  @NotNull
  private final ClientProperties clientProperties;
  private Task<Void> fafConnectionTask;
  private ServerWriter serverWriter;
  private volatile CompletableFuture<LoginMessage> loginFuture;
  private CompletableFuture<GameLaunchMessage> gameLaunchFuture;
  private final ObjectProperty<Long> sessionId = new SimpleObjectProperty<>();
  private final ReadOnlyObjectWrapper<ConnectionState> connectionState = new ReadOnlyObjectWrapper<>();
  private Socket fafServerSocket;
  private CompletableFuture<List<Avatar>> avatarsFuture;
  private CompletableFuture<List<IceServer>> iceServersFuture;

  private void onAvatarMessage(AvatarMessage avatarMessage) {
    avatarsFuture.complete(avatarMessage.getAvatarList());
  }

  private void onIceServersMessage(IceServersMessage iceServersMessage) {
    iceServersFuture.complete(iceServersMessage.getIceServers());
  }

  private void onNotice(NoticeMessage noticeMessage) {
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
    notificationService.addServerNotification(new ImmediateNotification(i18n.get("messageFromServer"), noticeMessage.getText(), noticeMessage.getSeverity(),
        Collections.singletonList(new DismissAction(i18n))));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends InboundMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    if (!messageListeners.containsKey(type)) {
      messageListeners.put(type, new LinkedList<>());
    }
    messageListeners.get(type).add((Consumer<InboundMessage>) listener);
  }

  @Override
  public <T extends InboundMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.get(type).remove(listener);
  }

  @Override
  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState.getReadOnlyProperty();
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn() {
    loginFuture = new CompletableFuture<>();

    // TODO extract class?
    fafConnectionTask = new Task<>() {

      @Override
      protected Void call() {
        while (!isCancelled()) {
          Server server = clientProperties.getServer();
          String serverHost = server.getHost();
          int serverPort = server.getPort();

          log.info("Trying to connect to FAF server at {}:{}", serverHost, serverPort);
          connectionState.set(ConnectionState.CONNECTING);

          try (Socket fafServerSocket = new Socket(serverHost, serverPort);
               OutputStream outputStream = fafServerSocket.getOutputStream()) {
            FafServerAccessorImpl.this.fafServerSocket = fafServerSocket;

            fafServerSocket.setKeepAlive(true);

            serverWriter = createServerWriter(outputStream);

            writeToServer(new InitSessionMessage(Version.getCurrentVersion()));

            log.info("FAF server connection established");
            connectionState.set(ConnectionState.CONNECTED);
            reconnectTimerService.resetConnectionFailures();

            blockingReadServer(fafServerSocket);
          } catch (IOException e) {
            connectionState.set(ConnectionState.DISCONNECTED);
            if (isCancelled()) {
              log.debug("Connection to FAF server has been closed");
            } else {
              if (loginFuture != null) {
                loginFuture.completeExceptionally(new LoginFailedException("Lost connection to server during login"));
                loginFuture = null;
                fafConnectionTask.cancel();
                return null;
              }
              log.warn("Lost connection to Server", e);
              reconnectTimerService.incrementConnectionFailures();
              reconnectTimerService.waitForReconnect();
            }
          }
        }
        return null;
      }

      @Override
      protected void failed() {
        super.failed();
        log.error("Server connection task failed", getException());
        if (loginFuture != null) {
          loginFuture.completeExceptionally(new LoginException("The server connection task failed, an internal error occurred"));
          loginFuture = null;
        }
        IOUtils.closeQuietly(serverWriter);
        IOUtils.closeQuietly(fafServerSocket);
      }

      @Override
      protected void cancelled() {
        IOUtils.closeQuietly(serverWriter);
        IOUtils.closeQuietly(fafServerSocket);
        log.debug("Closed connection to FAF lobby server");
      }
    };
    executeInBackground(fafConnectionTask);
    return loginFuture;
  }


  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    HostGameMessage hostGameMessage = new HostGameMessage(
        StringUtils.isEmpty(newGameInfo.getPassword()) ? GameAccess.PUBLIC : GameAccess.PASSWORD,
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        new boolean[0],
        newGameInfo.getFeaturedMod().getTechnicalName(),
        newGameInfo.getPassword(),
        null,
        newGameInfo.getGameVisibility(),
        newGameInfo.getRatingMin(),
        newGameInfo.getRatingMax(),
        newGameInfo.getEnforceRatingRange()
    );

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(hostGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    JoinGameMessage joinGameMessage = new JoinGameMessage(gameId, password);

    gameLaunchFuture = new CompletableFuture<>();
    writeToServer(joinGameMessage);
    return gameLaunchFuture;
  }

  @Override
  public void destroy() {
    disconnect();
  }

  public void disconnect() {
    if (fafConnectionTask != null) {
      fafConnectionTask.cancel(true);
    }
  }

  @Override
  public void reconnect() {
    IOUtils.closeQuietly(fafServerSocket);
    reconnectTimerService.skipWait();
  }

  @Override
  public void addFriend(int playerId) {
    writeToServer(new AddFriendMessage(playerId));
  }

  @Override
  public void addFoe(int playerId) {
    writeToServer(new AddFoeMessage(playerId));
  }

  @Override
  public void requestMatchmakerInfo() {
    writeToServer(new MatchmakerInfoOutboundMessage());
  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchMatchmaker() {
    gameLaunchFuture = new CompletableFuture<>();
    return gameLaunchFuture;
  }

  @Override
  public void stopSearchMatchmaker() {
    if (gameLaunchFuture != null && !gameLaunchFuture.isDone()) {
      gameLaunchFuture.cancel(true);
    } else {
      // this might happen when entering multiple queues, the game already having started and the server
      // telling the client about leaving all queues, therefore the client trying to cancel the matchmaking
      // as it isn't aware of the launching game anymore (which has already launched)
      log.warn("Game launch was already completed / cancelled when trying to stop searching for a matchmade game. Ignoring...");
    }
  }

  @Override
  public void sendGpgMessage(GpgOutboundMessage message) {
    writeToServer(message);
  }

  @Override
  public void removeFriend(int playerId) {
    writeToServer(new RemoveFriendMessage(playerId));
  }

  @Override
  public void removeFoe(int playerId) {
    writeToServer(new RemoveFoeMessage(playerId));
  }

  @Override
  public void selectAvatar(URL url) {
    writeToServer(new SelectAvatarMessage(url));
  }

  @Override
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {
    writeToServer(new BanPlayerMessage(playerId, reason, duration, periodType.name()));
  }

  @Override
  public void closePlayersGame(int playerId) {
    writeToServer(new ClosePlayersFAMessage(playerId));
  }

  @Override
  public void closePlayersLobby(int playerId) {
    writeToServer(new ClosePlayersLobbyMessage(playerId));
  }

  @Override
  public void broadcastMessage(String message) {
    writeToServer(new MakeBroadcastMessage(message));
  }

  @Override
  @Cacheable(value = CacheNames.AVAILABLE_AVATARS, sync = true)
  public CompletableFuture<List<Avatar>> getAvailableAvatars() {
    avatarsFuture = new CompletableFuture<>();
    writeToServer(new ListPersonalAvatarsMessage());
    return avatarsFuture;
  }

  @Override
  public CompletableFuture<List<IceServer>> getIceServers() {
    iceServersFuture = new CompletableFuture<>();
    writeToServer(new ListIceServersMessage());
    return iceServersFuture;
  }

  @Override
  public void restoreGameSession(int id) {
    writeToServer(new RestoreGameSessionMessage(id));
  }

  private ServerWriter createServerWriter(OutputStream outputStream) {
    return new ServerWriter(outputStream, objectMapper);
  }

  private void writeToServer(SerializableMessage message) {
    final CompletableFuture<LoginMessage> loginFuture = this.loginFuture;
    if (message instanceof GpgInboundMessage && loginFuture != null && !loginFuture.isDone()) {
      log.warn("GPGNetMessage discarded due to not being logged in");
      return;
    }

    serverWriter.write(message);
  }

  public void onServerMessage(String message) {
    ServerCommand serverCommand = ServerCommand.fromString(message);
    if (serverCommand != null) {
      dispatchServerMessage(serverCommand);
    } else {
      parseServerObject(message);
    }
  }

  private void dispatchServerMessage(ServerCommand serverCommand) {
    switch (serverCommand) {
      case PING -> {
        log.debug("Server PINGed");
        onServerPing();
      }
      case PONG -> log.debug("Server PONGed");
      default -> log.warn("Unknown server response: {}", serverCommand);
    }
  }

  private void parseServerObject(String jsonString) {
    try {
      InboundMessage inboundMessage = objectMapper.readValue(jsonString, InboundMessage.class);
      if (inboundMessage == null) {
        log.debug("Discarding unimplemented server message: {}", jsonString);
        return;
      }

      Class<?> messageClass = inboundMessage.getClass();
      while (messageClass != Object.class) {
        messageListeners.getOrDefault(messageClass, Collections.emptyList())
            .forEach(consumer -> consumer.accept(inboundMessage));
        messageClass = messageClass.getSuperclass();
      }

    } catch (JsonProcessingException e) {
      log.warn("Could not deserialize message: " + jsonString, e);
    }
  }

  private void onServerPing() {
    writeToServer(new PongMessage());
  }

  private void dispatchAuthenticationFailed(AuthenticationFailedMessage message) {
    fafConnectionTask.cancel();
    loginFuture.completeExceptionally(new LoginFailedException(message.getText()));
    loginFuture = null;
  }

  private void onFafLoginSucceeded(LoginMessage loginServerMessage) {
    log.info("FAF login succeeded");

    if (loginFuture != null) {
      loginFuture.complete(loginServerMessage);
      loginFuture = null;
    } else {
      log.warn("Unexpected login message from server");
    }
  }

  private void onSessionInitiated(SessionMessage sessionMessage) {
    log.info("FAF session initiated, session ID: {}", sessionMessage.getSession());
    this.sessionId.set(sessionMessage.getSession());
    logIn();
  }

  private void logIn() {
    try {
      String uniqueId = uidService.generate(String.valueOf(sessionId.get()), preferencesService.getFafDataDirectory().resolve("uid.log"));
      writeToServer(new LoginOauthClientMessage(tokenService.getRefreshedTokenValue(), sessionId.get(), uniqueId));
    } catch (IOException e) {
      onUIDNotExecuted(e);
    }
  }

  @VisibleForTesting
  protected void onUIDNotExecuted(Exception e) {
    log.error("UID.exe not executed", e);
    if (e.getMessage() == null) {
      return;
    }
    notificationService.addNotification(new ImmediateNotification(i18n.get("UIDNotExecuted"), e.getMessage(), Severity.ERROR,
        List.of(new CopyErrorAction(i18n, reportingService, e), new GetHelpAction(i18n, reportingService), new DismissAction(i18n))));
  }

  private void onIrcPassword(IrcPasswordServerMessage ircPasswordServerMessage) {
    eventBus.post(ircPasswordServerMessage);
  }

  private void onGameLaunchInfo(GameLaunchMessage gameLaunchMessage) {
    gameLaunchFuture.complete(gameLaunchMessage);
    gameLaunchFuture = null;
  }

  @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
  @Override
  public void ping() {
    if (fafServerSocket == null || !fafServerSocket.isConnected() || serverWriter == null) {
      return;
    }
    writeToServer(new PingMessage());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    addOnMessageListener(NoticeMessage.class, this::onNotice);
    addOnMessageListener(SessionMessage.class, this::onSessionInitiated);
    addOnMessageListener(LoginMessage.class, this::onFafLoginSucceeded);
    addOnMessageListener(IrcPasswordServerMessage.class, this::onIrcPassword);
    addOnMessageListener(GameLaunchMessage.class, this::onGameLaunchInfo);
    addOnMessageListener(AuthenticationFailedMessage.class, this::dispatchAuthenticationFailed);
    addOnMessageListener(AvatarMessage.class, this::onAvatarMessage);
    addOnMessageListener(IceServersMessage.class, this::onIceServersMessage);
  }


  @Override
  public void gameMatchmaking(MatchmakingQueue queue, MatchmakingState state) {
    writeToServer(new GameMatchmakingMessage(queue.getTechnicalName(), state));
  }

  @Override
  public void inviteToParty(Player recipient) {
    writeToServer(new InviteToPartyMessage(recipient.getId()));
  }

  @Override
  public void acceptPartyInvite(Player sender) {
    writeToServer(new AcceptPartyInviteMessage(sender.getId()));
  }

  @Override
  public void kickPlayerFromParty(Player kickedPlayer) {
    writeToServer(new KickPlayerFromPartyMessage(kickedPlayer.getId()));
  }

  @Override
  public void readyParty() {
    writeToServer(new ReadyPartyMessage());
  }

  @Override
  public void unreadyParty() {
    writeToServer(new UnreadyPartyMessage());
  }

  @Override
  public void leaveParty() {
    writeToServer(new LeavePartyMessage());
  }

  @Override
  public void setPartyFactions(List<Faction> factions) {
    writeToServer(new SetPartyFactionsMessage(factions));
  }
}
