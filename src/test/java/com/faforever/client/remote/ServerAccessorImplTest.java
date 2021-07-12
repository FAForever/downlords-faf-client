package com.faforever.client.remote;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.relay.event.CloseGameEvent;
import com.faforever.client.game.GameInfoMessageTestBuilder;
import com.faforever.client.game.GameLaunchMessageTestBuilder;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.game.NewGameInfoBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.UidService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.LobbyMode;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.PlayerInfo;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.remote.domain.inbound.InboundMessage;
import com.faforever.client.remote.domain.inbound.faf.AuthenticationFailedMessage;
import com.faforever.client.remote.domain.inbound.faf.AvatarMessage;
import com.faforever.client.remote.domain.inbound.faf.GameInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.GameLaunchMessage;
import com.faforever.client.remote.domain.inbound.faf.IceServersMessage;
import com.faforever.client.remote.domain.inbound.faf.LoginMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchCancelledMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchFoundMessage;
import com.faforever.client.remote.domain.inbound.faf.MatchmakerInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.NoticeMessage;
import com.faforever.client.remote.domain.inbound.faf.PartyInviteMessage;
import com.faforever.client.remote.domain.inbound.faf.PartyKickedMessage;
import com.faforever.client.remote.domain.inbound.faf.PlayerInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.SearchInfoMessage;
import com.faforever.client.remote.domain.inbound.faf.SessionMessage;
import com.faforever.client.remote.domain.inbound.faf.SocialMessage;
import com.faforever.client.remote.domain.inbound.faf.UpdatePartyMessage;
import com.faforever.client.remote.domain.inbound.faf.UpdatePartyMessage.PartyMember;
import com.faforever.client.remote.domain.inbound.faf.UpdatedAchievementsMessage;
import com.faforever.client.remote.domain.inbound.gpg.ConnectToPeerMessage;
import com.faforever.client.remote.domain.inbound.gpg.DisconnectFromPeerMessage;
import com.faforever.client.remote.domain.inbound.gpg.GpgHostGameMessage;
import com.faforever.client.remote.domain.inbound.gpg.GpgJoinGameMessage;
import com.faforever.client.remote.domain.inbound.gpg.IceInboundMessage;
import com.faforever.client.remote.domain.outbound.gpg.GpgOutboundMessage;
import com.faforever.client.remote.io.QDataInputStream;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.serialization.FactionMixin;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.teammatchmaking.MatchmakingQueueBuilder;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.UITest;
import com.faforever.commons.api.dto.Faction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.eventbus.EventBus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class ServerAccessorImplTest extends UITest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  @TempDir
  public Path faDirectory;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private UidService uidService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private ReportingService reportingService;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private EventBus eventBus;
  @Mock
  private ReconnectTimerService reconnectTimerService;

  private FafServerAccessorImpl instance;
  private ServerSocket fafLobbyServerSocket;
  private Socket localToServerSocket;
  private ServerWriter serverToClientWriter;
  private boolean stopped;
  private BlockingQueue<String> messagesReceivedByFafServer;
  private CountDownLatch serverToClientReadyLatch;
  private CountDownLatch messageReceivedLatch;
  private InboundMessage receivedMessage;
  private ClientProperties clientProperties;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() throws Exception {
    serverToClientReadyLatch = new CountDownLatch(1);
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);
    objectMapper = new ObjectMapper()
        .addMixIn(Faction.class, FactionMixin.class)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

    startFakeFafLobbyServer();

    clientProperties = new ClientProperties();
    clientProperties.getServer()
        .setHost(LOOPBACK_ADDRESS.getHostAddress())
        .setPort(fafLobbyServerSocket.getLocalPort());

    instance = new FafServerAccessorImpl(preferencesService, uidService, notificationService, i18n, reportingService, taskScheduler, eventBus, reconnectTimerService, clientProperties);
    instance.afterPropertiesSet();
    instance.addOnMessageListener(InboundMessage.class, inboundMessage -> {
      receivedMessage = inboundMessage;
      messageReceivedLatch.countDown();
    });
    LoginPrefs loginPrefs = new LoginPrefs();
    loginPrefs.setRefreshToken("junit");

    when(preferencesService.getFafDataDirectory()).thenReturn(faDirectory);
    when(uidService.generate(any(), any())).thenReturn("encrypteduidstring");

    connectAndLogIn();
  }

  private InboundMessage parseServerString(String json) throws JsonProcessingException {
    return objectMapper.readValue(json, InboundMessage.class);
  }

  private void startFakeFafLobbyServer() throws IOException {
    fafLobbyServerSocket = new ServerSocket(0);
    log.info("Fake server listening on " + fafLobbyServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {

      try (Socket socket = fafLobbyServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToClientWriter = new ServerWriter(socket.getOutputStream(), objectMapper);

        serverToClientReadyLatch.countDown();

        while (!stopped) {
          qDataInputStream.readInt();
          String json = qDataInputStream.readQString();

          messagesReceivedByFafServer.add(json);
        }
      } catch (IOException e) {
        System.out.println("Closing fake FAF lobby server: " + e.getMessage());
      }
    });
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private void assertMessageContainsComponents(String... values) {
    String json = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    for (String string : values) {
      assertThat(json, containsString(string));
    }
    assertThat(json, containsString("command"));
    assertThat(json, containsString("target"));
  }

  @AfterEach
  public void tearDown() {
    instance.disconnect();
    IOUtils.closeQuietly(fafLobbyServerSocket);
    IOUtils.closeQuietly(localToServerSocket);
  }

  private void connectAndLogIn() throws Exception {
    int playerUid = 123;
    String token = "abc";
    long sessionId = 456;

    CompletableFuture<LoginMessage> loginFuture = instance.connectAndLogin(token).toCompletableFuture();

    assertMessageContainsComponents("downlords-faf-client",
        "version",
        "user_agent",
        "ask_session"
        );

    SessionMessage sessionMessage = new SessionMessage(sessionId);
    sendFromServer(sessionMessage);

    assertMessageContainsComponents(token,
        String.valueOf(sessionId),
        "encrypteduidstring",
        "token",
        "session",
        "unique_id",
        "auth"
    );

    PlayerInfo me = new PlayerInfo(playerUid, "Junit", null, null, null, 0, null, null);
    LoginMessage loginServerMessage = new LoginMessage(me);

    sendFromServer(loginServerMessage);

    LoginMessage result = loginFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(result.getMe().getId(), is(playerUid));
    assertThat(result.getMe().getLogin(), is("Junit"));
  }

  /**
   * Writes the specified message to the client as if it was sent by the FAF server.
   */
  @SneakyThrows
  private void sendFromServer(InboundMessage fafServerMessage) {
    serverToClientReadyLatch.await();
    if (messageReceivedLatch != null) {
      messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    }
    receivedMessage = null;
    messageReceivedLatch = new CountDownLatch(1);
    serverToClientWriter.write(fafServerMessage);
  }

  @Test
  public void testRankedMatchNotification() throws Exception {
    String timeString = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(65)); // TODO: this is used in multiple tests, extract
    MatchmakerInfoMessage matchmakerMessage = new MatchmakerInfoMessage(
        List.of(new MatchmakerInfoMessage.MatchmakerQueue("ladder1v1", timeString, 1, 0, singletonList(new RatingRange(100, 200)), singletonList(new RatingRange(100, 200)))));

    CompletableFuture<MatchmakerInfoMessage> serviceStateDoneFuture = new CompletableFuture<>();

    WaitForAsyncUtils.waitForAsyncFx(200, () -> instance.addOnMessageListener(
        MatchmakerInfoMessage.class, serviceStateDoneFuture::complete
    ));

    sendFromServer(matchmakerMessage);

    MatchmakerInfoMessage matchmakerServerMessage = serviceStateDoneFuture.get(TIMEOUT, TIMEOUT_UNIT);
    assertThat(matchmakerServerMessage.getQueues(), not(empty()));

    instance.disconnect();
  }


  @Test
  public void testOnNotice() throws Exception {
    NoticeMessage noticeMessage = new NoticeMessage("foo bar", "warning");

    when(i18n.get("messageFromServer")).thenReturn("Message from Server");

    sendFromServer(noticeMessage);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService, timeout(1000)).addServerNotification(captor.capture());

    ImmediateNotification notification = captor.getValue();
    assertThat(notification.getSeverity(), is(Severity.WARN));
    assertThat(notification.getText(), is("foo bar"));
    assertThat(notification.getTitle(), is("Message from Server"));
    verify(i18n).get("messageFromServer");

    instance.disconnect();
  }

  @Test
  public void onKillNoticeStopsGame() throws Exception {
    NoticeMessage noticeMessage = new NoticeMessage(null, "kill");

    sendFromServer(noticeMessage);

    verify(eventBus, timeout(1000)).post(any(CloseGameEvent.class));

    instance.disconnect();
  }

  @Test
  public void onKickNoticeStopsApplication() throws Exception {
    NoticeMessage noticeMessage = new NoticeMessage(null, "kick");

    sendFromServer(noticeMessage);

    verify(taskScheduler, timeout(1000)).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

    instance.disconnect();
  }

  @Test
  public void onUIDNotFound() throws Exception {
    instance.onUIDNotExecuted(new FakeTestException("UID not found"));
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }

  @Test
  public void testRequestHostGame() {
    NewGameInfo newGameInfo = NewGameInfoBuilder.create()
        .defaultValues()
        .enforceRatingRange(true)
        .ratingMax(3000)
        .ratingMin(0)
        .get();

    instance.requestHostGame(newGameInfo);

    assertMessageContainsComponents("access",
        "mapname",
        "title",
        "options",
        "mod",
        "password",
        "version",
        "visibility",
        "rating_min",
        "rating_max",
        "enforce_rating_range",
        "game_host",
        "password",
        newGameInfo.getMap(),
        newGameInfo.getTitle(),
        newGameInfo.getFeaturedMod().getTechnicalName(),
        newGameInfo.getPassword(),
        "public",
        String.valueOf(newGameInfo.getRatingMax()),
        String.valueOf(newGameInfo.getRatingMin()),
        "true"
    );
  }

  @Test
  public void testRequestJoinGame() {
    instance.requestJoinGame(1, "pass");

    assertMessageContainsComponents("uid",
        "password",
        "game_join",
        "pass",
        String.valueOf(1)
    );
  }

  @Test
  public void testAddFriend() {
    instance.addFriend(1);

    assertMessageContainsComponents("friend",
        "social_add",
        String.valueOf(1)
    );
  }

  @Test
  public void testAddFoe() {
    instance.addFoe(1);

    assertMessageContainsComponents("foe",
        "social_add",
        String.valueOf(1)
    );
  }

  @Test
  public void testRemoveFriend() {
    instance.removeFriend(1);

    assertMessageContainsComponents("friend",
        "social_remove",
        String.valueOf(1)
    );
  }

  @Test
  public void testRemoveFoe() {
    instance.removeFoe(1);

    assertMessageContainsComponents("foe",
        "social_remove",
        String.valueOf(1)
    );
  }

  @Test
  public void testRequestMatchmakerInfo() {
    instance.requestMatchmakerInfo();

    assertMessageContainsComponents("matchmaker_info");
  }

  @Test
  public void testSendGpgMessage() {
    instance.sendGpgMessage(new GpgOutboundMessage("Test", List.of("arg1", "arg2")));

    assertMessageContainsComponents("command",
        "args",
        "Test",
        "arg1",
        "arg2");
  }

  @Test
  public void testBanPlayer() {
    instance.banPlayer(1, 100, PeriodType.DAY, "test");

    assertMessageContainsComponents("user_id",
        "ban",
        "action",
        "admin",
        "test",
        "reason",
        "duration",
        "period",
        PeriodType.DAY.name(),
        String.valueOf(1),
        String.valueOf(100));
  }

  @Test
  public void testClosePlayersGame() {
    instance.closePlayersGame(1);

    assertMessageContainsComponents("user_id",
        "admin",
        "action",
        String.valueOf(1));
  }

  @Test
  public void testClosePlayersLobby() {
    instance.closePlayersLobby(1);

    assertMessageContainsComponents("user_id",
        "admin",
        "action",
        String.valueOf(1));
  }

  @Test
  public void testBroadcastMessage() {
    instance.broadcastMessage("Test");

    assertMessageContainsComponents("message",
        "admin",
        "action",
        "Test");
  }

  @Test
  public void testGetAvailableAvatars() {
    try {
      instance.getAvailableAvatars();
    } catch (Exception ignored) {
    }

    assertMessageContainsComponents("avatar",
        "action");
  }

  @Test
  public void testGetIceServers() {
    instance.getIceServers();

    assertMessageContainsComponents("ice_servers");
  }

  @Test
  public void testRestoreGameSession() {
    instance.restoreGameSession(1);

    assertMessageContainsComponents("game_id",
        "restore_game_session",
        String.valueOf(1));
  }

  @Test
  public void testOnPing() {
    instance.ping();

    assertMessageContainsComponents("ping");
  }

  @Test
  public void testGameMatchmaking() {
    MatchmakingQueue queue = MatchmakingQueueBuilder.create().defaultValues().get();
    instance.gameMatchmaking(queue, MatchmakingState.START);

    assertMessageContainsComponents("queue_name",
        "state",
        "game_matchmaking",
        queue.getTechnicalName(),
        "start");
  }

  @Test
  public void testInviteToParty() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.inviteToParty(player);

    assertMessageContainsComponents("recipient_id",
        "invite_to_party",
        String.valueOf(player.getId()));
  }

  @Test
  public void testAcceptPartyInvite() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.acceptPartyInvite(player);

    assertMessageContainsComponents("sender_id",
        "accept_party_invite",
        String.valueOf(player.getId()));
  }

  @Test
  public void testKickPlayerFromParty() {
    Player player = PlayerBuilder.create("junit").defaultValues().get();
    instance.kickPlayerFromParty(player);

    assertMessageContainsComponents("kicked_player_id",
        "kick_player_from_party",
        String.valueOf(player.getId()));
  }

  @Test
  public void testReadyParty() {
    instance.readyParty();

    assertMessageContainsComponents("ready_party");
  }

  @Test
  public void testUnreadyParty() {
    instance.unreadyParty();

    assertMessageContainsComponents("unready_party");
  }

  @Test
  public void testLeaveParty() {
    instance.leaveParty();

    assertMessageContainsComponents("leave_party");
  }

  @Test
  public void testSetPartyFactions() {
    instance.setPartyFactions(List.of(Faction.AEON, Faction.UEF, Faction.CYBRAN, Faction.SERAPHIM));

    assertMessageContainsComponents("factions",
        "set_party_factions",
        "aeon", "uef", "cybran", "seraphim");
  }

  @Test
  public void testSelectAvatar() throws MalformedURLException {
    URL url = new URL("http://google.com");
    instance.selectAvatar(url);

    assertMessageContainsComponents("avatar",
        "action",
        url.toString()
    );
  }

  @Test
  public void testOnGameInfo() throws InterruptedException, JsonProcessingException {
    GameInfoMessage gameInfoMessage = GameInfoMessageTestBuilder.create(1)
        .defaultValues()
        .get();

    sendFromServer(gameInfoMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(gameInfoMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "game_info",
          "host" : "Some host",
          "password_protected" : false,
          "visibility" : null,
          "state" : "open",
          "num_players" : 1,
          "teams" : { },
          "featured_mod" : "faf",
          "uid" : 1,
          "max_players" : 4,
          "title" : "Test preferences",
          "sim_mods" : null,
          "mapname" : "scmp_007",
          "launched_at" : null,
          "rating_type" : null,
          "rating_min" : 0,
          "rating_max" : 3000,
          "enforce_rating_range" : false,
          "game_type" : null,
          "games" : null,
          "target" : null
        }""");

    assertThat(parsedMessage, is(gameInfoMessage));
  }

  @Test
  public void testOnGameLaunch() throws InterruptedException, JsonProcessingException {
    GameLaunchMessage gameLaunchMessage = GameLaunchMessageTestBuilder.create()
        .defaultValues()
        .faction(Faction.AEON)
        .initMode(LobbyMode.AUTO_LOBBY)
        .get();

    instance.startSearchMatchmaker();
    sendFromServer(gameLaunchMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(gameLaunchMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "game_launch",
          "args" : [ ],
          "uid" : 1,
          "mod" : "faf",
          "mapname" : null,
          "name" : "test",
          "expected_players" : null,
          "team" : null,
          "map_position" : null,
          "faction" : "aeon",
          "init_mode" : 1,
          "rating_type" : "global",
          "target" : null
        }""");

      assertThat(parsedMessage, is(gameLaunchMessage));

  }

  @Test
  public void testOnPlayerInfo() throws InterruptedException, JsonProcessingException {
    PlayerInfoMessage playerInfoMessage = new PlayerInfoMessage(List.of());

    sendFromServer(playerInfoMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(playerInfoMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "player_info",
          "players" : [ ],
          "target" : null
        }""");

    assertThat(parsedMessage, is(playerInfoMessage));
  }

  @Test
  public void testOnMatchmakerInfo() throws InterruptedException, JsonProcessingException {
    MatchmakerInfoMessage matchmakerInfoMessage = new MatchmakerInfoMessage(List.of());

    sendFromServer(matchmakerInfoMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(matchmakerInfoMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "matchmaker_info",
          "queues" : [ ],
          "target" : null
        }""");

    assertThat(parsedMessage, is(matchmakerInfoMessage));
  }

  @Test
  public void testOnMatchFound() throws InterruptedException, JsonProcessingException {
    MatchFoundMessage matchFoundMessage = new MatchFoundMessage("test");

    sendFromServer(matchFoundMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(matchFoundMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "match_found",
          "queue_name" : "test",
          "target" : null
        }""");

    assertThat(parsedMessage, is(matchFoundMessage));
  }

  @Test
  public void testOnMatchCancelled() throws InterruptedException, JsonProcessingException {
    MatchCancelledMessage matchCancelledMessage = new MatchCancelledMessage();

    sendFromServer(matchCancelledMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(matchCancelledMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "match_cancelled",
          "target" : null
        }""");

    assertThat(parsedMessage, is(matchCancelledMessage));
  }

  @Test
  public void testOnSocialMessage() throws InterruptedException, JsonProcessingException {
    SocialMessage socialMessage = new SocialMessage(List.of(123, 124), List.of(456, 457), List.of("aeolus"));

    sendFromServer(socialMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(socialMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "social",
          "friends" : [ 123, 124 ],
          "foes" : [ 456, 457 ],
          "channels" : [ "aeolus" ],
          "target" : null
        }""");

    assertThat(parsedMessage, is(socialMessage));
  }

  //Causes an infinite loop on github actions
  @Disabled
  @Test
  public void testOnAuthenticationFailed() throws InterruptedException, JsonProcessingException {
    AuthenticationFailedMessage authenticationFailedMessage = new AuthenticationFailedMessage("boo");

    instance.connectAndLogin("a");
    sendFromServer(authenticationFailedMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(authenticationFailedMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
         "command" : "authentication_failed",
         "text" : "boo",
          "target" : null
        }""");

    assertThat(parsedMessage, is(authenticationFailedMessage));
  }

  @Test
  public void testOnUpdatedAchievements() throws InterruptedException, JsonProcessingException {
    UpdatedAchievementsMessage updatedAchievementsMessage = new UpdatedAchievementsMessage(List.of());

    sendFromServer(updatedAchievementsMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(updatedAchievementsMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "updated_achievements",
          "updated_achievements" : [ ],
          "target" : null
        }""");

    assertThat(parsedMessage, is(updatedAchievementsMessage));
  }

  @Test
  public void testOnIceServers() throws InterruptedException, JsonProcessingException {
    IceServersMessage iceServersMessage = new IceServersMessage(List.of());

    instance.getIceServers();
    sendFromServer(iceServersMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(iceServersMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "ice_servers",
          "ice_servers" : [ ],
          "target" : null
        }""");

    assertThat(parsedMessage, is(iceServersMessage));
  }

  @Test
  public void testOnAvatarMessage() throws InterruptedException, JsonProcessingException {
    AvatarMessage avatarMessage = new AvatarMessage(List.of());

    try {
      instance.getAvailableAvatars();
    } catch (Exception ignored) {
    }
    sendFromServer(avatarMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(avatarMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "avatar",
          "avatarlist" : [ ],
          "target" : null
        }""");

    assertThat(parsedMessage, is(avatarMessage));
  }

  @Test
  public void testOnUpdatePartyMessage() throws InterruptedException, JsonProcessingException {
    UpdatePartyMessage updatePartyMessage = new UpdatePartyMessage(1, List.of(new PartyMember(123, List.of(Faction.UEF, Faction.CYBRAN, Faction.AEON, Faction.SERAPHIM))));

    sendFromServer(updatePartyMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(updatePartyMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "update_party",
          "owner" : 1,
          "members" : [{"player":123,"factions":["uef","cybran","aeon","seraphim"]} ],
          "target" : null
        }""");

    assertThat(parsedMessage, is(updatePartyMessage));
  }

  @Test
  public void testOnPartyInviteMessage() throws InterruptedException, JsonProcessingException {
    PartyInviteMessage partyInviteMessage = new PartyInviteMessage(1);

    sendFromServer(partyInviteMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(partyInviteMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "party_invite",
          "sender" : 1,
          "target" : null
        }""");

    assertThat(parsedMessage, is(partyInviteMessage));
  }

  @Test
  public void testOnPartyKickedMessage() throws InterruptedException, JsonProcessingException {
    PartyKickedMessage partyKickedMessage = new PartyKickedMessage();

    sendFromServer(partyKickedMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(partyKickedMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "kicked_from_party",
          "target" : null
        }""");

    assertThat(parsedMessage, is(partyKickedMessage));
  }

  @Test
  public void testOnSearchInfoMessage() throws InterruptedException, JsonProcessingException {
    SearchInfoMessage searchInfoMessage = new SearchInfoMessage("test", MatchmakingState.START);

    sendFromServer(searchInfoMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(searchInfoMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "search_info",
          "queue_name": "test",
          "state": "start",
          "target" : null
        }""");

    assertThat(parsedMessage, is(searchInfoMessage));
  }

  @Test
  public void testOnGpgHostMessage() throws InterruptedException, JsonProcessingException {
    GpgHostGameMessage gpgHostGameMessage = new GpgHostGameMessage();

    gpgHostGameMessage.setMap("test");

    sendFromServer(gpgHostGameMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(gpgHostGameMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "HostGame",
          "target" : "game",
          "args" : [ "test" ]
        }""");

    assertThat(parsedMessage, is(gpgHostGameMessage));
  }

  @Test
  public void testOnGpgJoinMessage() throws InterruptedException, JsonProcessingException {
    GpgJoinGameMessage gpgJoinGameMessage = new GpgJoinGameMessage();

    gpgJoinGameMessage.setUsername("test");
    gpgJoinGameMessage.setPeerUid(1);

    sendFromServer(gpgJoinGameMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(gpgJoinGameMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "JoinGame",
          "target" : "game",
          "args" : [ "test", 1 ]
        }""");

    assertThat(parsedMessage, is(gpgJoinGameMessage));
  }

  @Test
  public void testOnConnectToPeerMessage() throws InterruptedException, JsonProcessingException {
    ConnectToPeerMessage connectToPeerMessage = new ConnectToPeerMessage();

    connectToPeerMessage.setUsername("test");
    connectToPeerMessage.setPeerUid(1);
    connectToPeerMessage.setOffer(true);

    sendFromServer(connectToPeerMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(connectToPeerMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
          "command" : "ConnectToPeer",
          "target" : "game",
          "args" : [ "test", 1, true ]
        }""");

    assertThat(parsedMessage, is(connectToPeerMessage));
  }

  @Test
  public void testOnIceInboundMessage() throws InterruptedException, JsonProcessingException {
    IceInboundMessage iceInboundMessage = new IceInboundMessage();

    iceInboundMessage.setSender(1);
    iceInboundMessage.setRecord(3);

    sendFromServer(iceInboundMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(iceInboundMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
           "command" : "IceMsg",
           "target" : "game",
           "args" : [ 1, 3 ]
         }""");

    assertThat(parsedMessage, is(iceInboundMessage));
  }

  @Test
  public void testOnDisconnectFromPeerMessage() throws InterruptedException, JsonProcessingException {
    DisconnectFromPeerMessage disconnectFromPeerMessage = new DisconnectFromPeerMessage();

    disconnectFromPeerMessage.setUid(1);

    sendFromServer(disconnectFromPeerMessage);
    messageReceivedLatch.await(TIMEOUT, TIMEOUT_UNIT);
    assertThat(receivedMessage, is(disconnectFromPeerMessage));

    InboundMessage parsedMessage = parseServerString("""
        {
            "command" : "DisconnectFromPeer",
            "target" : "game",
            "args" : [ 1 ]
          }""");

    assertThat(parsedMessage, equalTo(disconnectFromPeerMessage));
  }
}
