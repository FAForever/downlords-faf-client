package com.faforever.client.remote;

import com.faforever.client.api.TokenRetriever;
import com.faforever.client.builders.GameInfoMessageBuilder;
import com.faforever.client.builders.GameLaunchMessageBuilder;
import com.faforever.client.builders.MatchmakerQueueBeanBuilder;
import com.faforever.client.builders.NewGameInfoBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.MatchmakerQueueBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.game.GameService;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.io.UidService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.update.Version;
import com.faforever.commons.lobby.AvatarListInfo;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.Faction;
import com.faforever.commons.lobby.FafLobbyClient;
import com.faforever.commons.lobby.GameInfo;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.faforever.commons.lobby.LobbyMode;
import com.faforever.commons.lobby.LoginSuccessResponse;
import com.faforever.commons.lobby.MatchmakerInfo;
import com.faforever.commons.lobby.MatchmakerInfo.MatchmakerQueue;
import com.faforever.commons.lobby.MatchmakerMatchCancelledResponse;
import com.faforever.commons.lobby.MatchmakerMatchFoundResponse;
import com.faforever.commons.lobby.MatchmakerState;
import com.faforever.commons.lobby.MessageTarget;
import com.faforever.commons.lobby.NoticeInfo;
import com.faforever.commons.lobby.PartyInfo;
import com.faforever.commons.lobby.PartyInfo.PartyMember;
import com.faforever.commons.lobby.PartyInvite;
import com.faforever.commons.lobby.PartyKick;
import com.faforever.commons.lobby.Player.Avatar;
import com.faforever.commons.lobby.PlayerInfo;
import com.faforever.commons.lobby.SearchInfo;
import com.faforever.commons.lobby.ServerMessage;
import com.faforever.commons.lobby.SessionResponse;
import com.faforever.commons.lobby.SocialInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule.Builder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.LineEncoder;
import io.netty.handler.codec.string.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
public class ServerAccessorTest extends ServiceTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  @TempDir
  public Path tempDirectory;

  @Mock
  private UidService uidService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private TokenRetriever tokenRetriever;
  @Mock
  private I18n i18n;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private GameService gameService;
  @Mock
  private IceAdapter iceAdapter;
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private ObjectMapper objectMapper;

  private FafServerAccessor instance;
  private CountDownLatch messageReceivedByClientLatch;
  private ServerMessage receivedMessage;

  private final String token = "abc";
  private final Sinks.Many<String> serverReceivedSink = Sinks.many().replay().latest();
  private final Flux<String> serverMessagesReceived = serverReceivedSink.asFlux();
  private final Sinks.Many<String> serverSentSink = Sinks.many().unicast().onBackpressureBuffer();
  private DisposableServer disposableServer;

  private MockWebServer mockApi;

  @BeforeEach
  public void setUp() throws Exception {
    mockApi = new MockWebServer();
    mockApi.start();

    objectMapper.registerModule(new Builder().build())
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

    when(tokenRetriever.getRefreshedTokenValue()).thenReturn(Mono.just(token));

    startFakeFafLobbyServer();

    clientProperties.getUser()
                    .setBaseUrl("http://localhost:%d".formatted(mockApi.getPort()));
    clientProperties.setUserAgent("downlords-faf-client");

    WebClient webClient = WebClient.builder()
                                   .baseUrl(String.format("http://localhost:%s", mockApi.getPort()))
                                   .build();

    mockApi.enqueue(new MockResponse()
                        .setBody(objectMapper.writeValueAsString(
                            new LobbyAccess("http://localhost:%d".formatted(disposableServer.port()))))
                        .addHeader("Content-Type", "application/json;charset=utf-8"));

    instance = new FafServerAccessor(notificationService, i18n, taskScheduler, tokenRetriever, uidService,
                                     clientProperties, new FafLobbyClient(objectMapper), () -> webClient);

    instance.afterPropertiesSet();
    instance.addEventListener(ServerMessage.class, serverMessage -> {
      receivedMessage = serverMessage;
      messageReceivedByClientLatch.countDown();
    });

    when(uidService.generate(any())).thenReturn("encrypteduidstring");

    connectAndLogIn();
  }

  private ServerMessage parseServerString(String json) throws JsonProcessingException {
    return objectMapper.readValue(json, ServerMessage.class);
  }

  private void startFakeFafLobbyServer() {
    this.disposableServer = HttpServer.create()
                                      .doOnConnection(connection -> {
                                        log.info("New Client connected to server");
                                        connection.addHandlerFirst(new LineEncoder(
                                                      LineSeparator.UNIX)) // TODO: This is not working. Raise a bug ticket! Workaround below
                                                  .addHandlerLast(new LineBasedFrameDecoder(1024 * 1024));
                                      })
                                      .doOnBound(disposableServer -> log.info("Fake server listening at {} on port {}",
                                                                              disposableServer.host(),
                                                                              disposableServer.port()))
                                      .noSSL()
                                      .host(LOOPBACK_ADDRESS.getHostAddress())
                                      .route(routes -> routes.ws("/", (inbound, outbound) -> {
                                        Mono<Void> inboundMono = inbound.receive()
                                                                        .asString(StandardCharsets.UTF_8)
                                                                        .doOnNext(message -> {
                                                                          log.info("Received message at server {}",
                                                                                   message);
                                                                          log.info("Emit Result is {}",
                                                                                   serverReceivedSink.tryEmitNext(
                                                                                       message));
                                                                        })
                                                                        .then();

                                        Mono<Void> outboundMono = outbound.sendString(
                                            serverSentSink.asFlux().map(message -> {
                                              log.info("Sending message from fake server {}", message);
                                              return message + "\n";
                                            }), StandardCharsets.UTF_8).then();

                                        return Flux.firstWithSignal(inboundMono, outboundMono);
                                      }))
                                      .bindNow();
  }

  private void assertMessageContainsComponents(String command, String... values) {
    serverMessagesReceived.filter(message -> message.contains(command)).next()
                          .switchIfEmpty(Mono.error(new AssertionError("No matching messages")))
                          .doOnNext(json -> {
                            assertThat(json, containsString("command"));
                            for (String string : values) {
                              assertThat(json, containsString(string));
                            }
                          }).block(Duration.ofSeconds(10));
  }

  @AfterEach
  public void tearDown() throws IOException {
    mockApi.shutdown();
    disposableServer.disposeNow();
    instance.disconnect();
  }

  private void connectAndLogIn() throws Exception {
    long sessionId = 456;
    SessionResponse sessionMessage = new SessionResponse(sessionId);

    int playerUid = 123;
    com.faforever.commons.lobby.Player me = new com.faforever.commons.lobby.Player(playerUid, "Junit", null, null, "",
                                                                                   new HashMap<>(), new HashMap<>());
    LoginSuccessResponse loginServerMessage = new LoginSuccessResponse(me);

    StepVerifier stepVerifier = StepVerifier.create(instance.connectAndLogIn())
                                            .expectNextMatches(player -> player.getId() == playerUid && "Junit".equals(
                                                player.getLogin()))
                                            .expectComplete()
                                            .verifyLater();

    assertMessageContainsComponents("ask_session",
                                    "downlords-faf-client",
                                    "version",
                                    "user_agent",
                                    Version.getCurrentVersion()
    );

    sendFromServer(sessionMessage);


    assertMessageContainsComponents(
        "auth",
        token,
        String.valueOf(sessionId),
        "encrypteduidstring",
        "token",
        "session",
        "unique_id"
    );

    sendFromServer(loginServerMessage);

    stepVerifier.verify(Duration.ofMillis(TIMEOUT));
  }

  /**
   * Writes the specified message to the client as if it was sent by the FAF server.
   */
  private void sendFromServer(ServerMessage fafServerMessage) throws JsonProcessingException {
    messageReceivedByClientLatch = new CountDownLatch(1);
    serverSentSink.tryEmitNext(objectMapper.writeValueAsString(fafServerMessage));
  }

  @Test
  public void testRankedMatchNotification() throws Exception {
    OffsetDateTime popTime = OffsetDateTime.ofInstant(Instant.now().plusSeconds(65), ZoneOffset.UTC);
    MatchmakerQueue queue = new MatchmakerQueue("ladder1v1", popTime, 65, 1, 0, List.of(List.of(100, 200)),
                                                List.of(List.of(100, 200)));
    MatchmakerInfo matchmakerMessage = new MatchmakerInfo(
        List.of(queue));

    CompletableFuture<MatchmakerInfo> serviceStateDoneFuture = new CompletableFuture<>();

    instance.addEventListener(MatchmakerInfo.class, serviceStateDoneFuture::complete);

    sendFromServer(matchmakerMessage);

    MatchmakerInfo matchmakerServerMessage = serviceStateDoneFuture.get(TIMEOUT, TIMEOUT_UNIT);
    assertThat(matchmakerServerMessage.getQueues(), contains(queue));
  }


  @Test
  public void testOnNotice() throws Exception {
    NoticeInfo noticeMessage = new NoticeInfo("warning", "foo bar");

    when(i18n.get("messageFromServer")).thenReturn("Message from Server");

    sendFromServer(noticeMessage);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService, timeout(1000)).addServerNotification(captor.capture());

    ImmediateNotification notification = captor.getValue();
    assertThat(notification.getSeverity(), is(Severity.WARN));
    assertThat(notification.getText(), is("foo bar"));
    assertThat(notification.getTitle(), is("Message from Server"));
    verify(i18n).get("messageFromServer");
  }

  @Test
  public void onKickNoticeStopsApplication() throws Exception {
    NoticeInfo noticeMessage = new NoticeInfo("kick", null);

    sendFromServer(noticeMessage);

    verify(taskScheduler, timeout(10000)).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
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

    assertMessageContainsComponents(
        "game_host",
        "access",
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

    assertMessageContainsComponents(
        "game_join",
        "id",
        "password",
        "pass",
        String.valueOf(1)
    );
  }

  @Test
  public void testAddFriend() {

    instance.addFriend(1);

    assertMessageContainsComponents(
        "social_add",
        "friend",
        String.valueOf(1)
    );
  }

  @Test
  public void testAddFoe() {

    instance.addFoe(1);

    assertMessageContainsComponents(
        "social_add",
        "foe",
        String.valueOf(1)
    );
  }

  @Test
  public void testRemoveFriend() {

    instance.removeFriend(1);

    assertMessageContainsComponents(
        "social_remove",
        "friend",
        String.valueOf(1)
    );
  }

  @Test
  public void testRemoveFoe() {

    instance.removeFoe(1);

    assertMessageContainsComponents(
        "social_remove",
        "foe",
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

    instance.sendGpgMessage(new GpgGameOutboundMessage("Test", List.of("arg1", "arg2"), MessageTarget.GAME));

    assertMessageContainsComponents(
        "Test",
        "args",
        "arg1",
        "arg2");
  }

  @Test
  public void testClosePlayersGame() {

    instance.closePlayersGame(1);

    assertMessageContainsComponents(
        "admin",
        "user_id",
        "action",
        String.valueOf(1));
  }

  @Test
  public void testClosePlayersLobby() {

    instance.closePlayersLobby(1);

    assertMessageContainsComponents(
        "admin",
        "user_id",
        "action",
        String.valueOf(1));
  }

  @Test
  public void testBroadcastMessage() {

    instance.broadcastMessage("Test");

    assertMessageContainsComponents(
        "admin",
        "message",
        "action",
        "Test");
  }

  @Test
  public void testGetAvailableAvatars() throws Exception {

    instance.getAvailableAvatars();

    assertMessageContainsComponents(
        "avatar",
        "list_avatar",
        "action"
    );

    AvatarListInfo avatarList = new AvatarListInfo(
        List.of(new Avatar("google.com", "test"), new Avatar("google.com", "test")));
    sendFromServer(avatarList);

    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(avatarList));
  }

  @Test
  public void testRestoreGameSession() {

    instance.restoreGameSession(1);

    assertMessageContainsComponents(
        "restore_game_session",
        "game_id",
        String.valueOf(1));
  }

  @Test
  public void testGameMatchmaking() {
    MatchmakerQueueBean queue = MatchmakerQueueBeanBuilder.create().defaultValues().get();

    instance.gameMatchmaking(queue, MatchmakerState.START);

    assertMessageContainsComponents(
        "game_matchmaking",
        "queue_name",
        "state",
        queue.getTechnicalName(),
        "start");
  }

  @Test
  public void testInviteToParty() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();

    instance.inviteToParty(player);

    assertMessageContainsComponents(
        "invite_to_party",
        "recipient_id",
        String.valueOf(player.getId()));
  }

  @Test
  public void testAcceptPartyInvite() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();

    instance.acceptPartyInvite(player);

    assertMessageContainsComponents(
        "accept_party_invite",
        "sender_id",
        String.valueOf(player.getId()));
  }

  @Test
  public void testKickPlayerFromParty() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();

    instance.kickPlayerFromParty(player);

    assertMessageContainsComponents(
        "kick_player_from_party",
        "kicked_player_id",
        String.valueOf(player.getId()));
  }

  @Test
  public void testReadyParty() {

    instance.sendReady("1");

    assertMessageContainsComponents("is_ready_response", "1");
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

    assertMessageContainsComponents(
        "set_party_factions",
        "factions",
        "aeon", "uef", "cybran", "seraphim");
  }

  @Test
  public void testSelectAvatar() throws MalformedURLException {
    URL url = new URL("http://google.com");

    instance.selectAvatar(url);

    assertMessageContainsComponents(
        "avatar",
        "action",
        url.toString()
    );
  }

  @Test
  public void testOnGameInfo() throws InterruptedException, JsonProcessingException {
    GameInfo gameInfoMessage = GameInfoMessageBuilder.create(1)
                                                     .defaultValues()
                                                     .get();

    sendFromServer(gameInfoMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(gameInfoMessage));

    ServerMessage parsedMessage = parseServerString("""
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
                                                          "map_file_path" : "scmp_007",
                                                          "launched_at" : null,
                                                          "rating_type" : null,
                                                          "rating_min" : 0,
                                                          "rating_max" : 3000,
                                                          "enforce_rating_range" : false,
                                                          "game_type" : null,
                                                          "games" : null
                                                        }""");

    assertThat(parsedMessage, equalTo(gameInfoMessage));
  }

  @Test
  public void testOnGameLaunch() throws InterruptedException, JsonProcessingException {
    GameLaunchResponse gameLaunchMessage = GameLaunchMessageBuilder.create()
                                                                   .defaultValues()
                                                                   .faction(Faction.AEON)
                                                                   .gameType(GameType.MATCHMAKER)
                                                                   .initMode(LobbyMode.AUTO_LOBBY)
                                                                   .get();

    instance.startSearchMatchmaker();
    sendFromServer(gameLaunchMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(gameLaunchMessage));

    ServerMessage parsedMessage = parseServerString("""
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
                                                          "game_type" : "matchmaker",
                                                          "rating_type" : "global"
                                                        }""");

    assertThat(parsedMessage, equalTo(gameLaunchMessage));

  }

  @Test
  public void testOnPlayerInfo() throws InterruptedException, JsonProcessingException {
    PlayerInfo playerInfoMessage = new PlayerInfo(List.of());

    sendFromServer(playerInfoMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(playerInfoMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "player_info",
                                                          "players" : [ ]
                                                        }""");

    assertThat(parsedMessage, equalTo(playerInfoMessage));
  }

  @Test
  public void testOnMatchmakerInfo() throws InterruptedException, JsonProcessingException {
    MatchmakerInfo matchmakerInfoMessage = new MatchmakerInfo(List.of());

    sendFromServer(matchmakerInfoMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(matchmakerInfoMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "matchmaker_info",
                                                          "queues" : [ ]
                                                        }""");

    assertThat(parsedMessage, equalTo(matchmakerInfoMessage));
  }

  @Test
  public void testOnMatchFound() throws InterruptedException, JsonProcessingException {
    MatchmakerMatchFoundResponse matchFoundMessage = new MatchmakerMatchFoundResponse("test");

    sendFromServer(matchFoundMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(matchFoundMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "match_found",
                                                          "queue_name" : "test"
                                                        }""");

    assertThat(parsedMessage, equalTo(matchFoundMessage));
  }

  @Test
  public void testOnMatchCancelled() throws InterruptedException, JsonProcessingException {
    MatchmakerMatchCancelledResponse matchCancelledMessage = new MatchmakerMatchCancelledResponse();

    sendFromServer(matchCancelledMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage.getClass(), equalTo(matchCancelledMessage.getClass()));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "match_cancelled"
                                                        }""");

    assertThat(parsedMessage.getClass(), equalTo(matchCancelledMessage.getClass()));
  }

  @Test
  public void testOnSocialMessage() throws InterruptedException, JsonProcessingException {
    SocialInfo socialMessage = new SocialInfo(List.of("aeolus"), List.of("aeolus"), List.of(123, 124),
                                              List.of(456, 457), 0);

    sendFromServer(socialMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(socialMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "social",
                                                          "friends" : [ 123, 124 ],
                                                          "foes" : [ 456, 457 ],
                                                          "channels" : [ "aeolus" ],
                                                          "autojoin" : [ "aeolus" ]
                                                        }""");

    assertThat(parsedMessage, equalTo(socialMessage));
  }

  @Test
  public void testOnAvatarMessage() throws InterruptedException, JsonProcessingException {
    AvatarListInfo avatarMessage = new AvatarListInfo(List.of());

    instance.getAvailableAvatars();
    sendFromServer(avatarMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(avatarMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "avatar",
                                                          "avatarlist" : [ ]
                                                        }""");

    assertThat(parsedMessage, equalTo(avatarMessage));
  }

  @Test
  public void testOnUpdatePartyMessage() throws InterruptedException, JsonProcessingException {
    PartyInfo updatePartyMessage = new PartyInfo(1, List.of(
        new PartyMember(123, List.of(Faction.UEF, Faction.CYBRAN, Faction.AEON, Faction.SERAPHIM))));

    sendFromServer(updatePartyMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(updatePartyMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "update_party",
                                                          "owner" : 1,
                                                          "members" : [{"player":123,"factions":["uef","cybran","aeon","seraphim"]} ]
                                                        }""");

    assertThat(parsedMessage, equalTo(updatePartyMessage));
  }

  @Test
  public void testOnPartyInviteMessage() throws InterruptedException, JsonProcessingException {
    PartyInvite partyInviteMessage = new PartyInvite(1);

    sendFromServer(partyInviteMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(partyInviteMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "party_invite",
                                                          "sender" : 1
                                                        }""");

    assertThat(parsedMessage, equalTo(partyInviteMessage));
  }

  @Test
  public void testOnPartyKickedMessage() throws InterruptedException, JsonProcessingException {
    PartyKick partyKickedMessage = new PartyKick();

    sendFromServer(partyKickedMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage.getClass(), equalTo(partyKickedMessage.getClass()));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "kicked_from_party"
                                                        }""");

    assertThat(parsedMessage.getClass(), equalTo(partyKickedMessage.getClass()));
  }

  @Test
  public void testOnSearchInfoMessage() throws InterruptedException, JsonProcessingException {
    SearchInfo searchInfoMessage = new SearchInfo("test", MatchmakerState.START);

    sendFromServer(searchInfoMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(searchInfoMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "search_info",
                                                          "queue_name": "test",
                                                          "state": "start"
                                                        }""");

    assertThat(parsedMessage, equalTo(searchInfoMessage));
  }

  @Test
  public void testOnGpgHostMessage() throws InterruptedException, JsonProcessingException {
    HostGameGpgCommand gpgHostGameMessage = new HostGameGpgCommand(MessageTarget.GAME, List.of("test"));

    sendFromServer(gpgHostGameMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(gpgHostGameMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "HostGame",
                                                          "target" : "game",
                                                          "args" : [ "test" ]
                                                        }""");

    assertThat(parsedMessage, equalTo(gpgHostGameMessage));
  }

  @Test
  public void testOnGpgJoinMessage() throws InterruptedException, JsonProcessingException {
    JoinGameGpgCommand gpgJoinGameMessage = new JoinGameGpgCommand(MessageTarget.GAME, List.of("test", 1));

    sendFromServer(gpgJoinGameMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(gpgJoinGameMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "JoinGame",
                                                          "target" : "game",
                                                          "args" : [ "test", 1 ]
                                                        }""");

    assertThat(parsedMessage, equalTo(gpgJoinGameMessage));
  }

  @Test
  public void testOnConnectToPeerMessage() throws InterruptedException, JsonProcessingException {
    ConnectToPeerGpgCommand connectToPeerMessage = new ConnectToPeerGpgCommand(MessageTarget.GAME,
                                                                               List.of("test", 1, true));

    sendFromServer(connectToPeerMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(connectToPeerMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                          "command" : "ConnectToPeer",
                                                          "target" : "game",
                                                          "args" : [ "test", 1, true ]
                                                        }""");

    assertThat(parsedMessage, equalTo(connectToPeerMessage));
  }

  @Test
  public void testOnIceServerMessage() throws InterruptedException, JsonProcessingException {
    IceMsgGpgCommand iceServerMessage = new IceMsgGpgCommand(MessageTarget.GAME, List.of(1, 3));

    sendFromServer(iceServerMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(iceServerMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                           "command" : "IceMsg",
                                                           "target" : "game",
                                                           "args" : [ 1, 3 ]
                                                         }""");

    assertThat(parsedMessage, equalTo(iceServerMessage));
  }

  @Test
  public void testOnDisconnectFromPeerMessage() throws InterruptedException, JsonProcessingException {
    DisconnectFromPeerGpgCommand disconnectFromPeerMessage = new DisconnectFromPeerGpgCommand(MessageTarget.GAME,
                                                                                              List.of(1));

    sendFromServer(disconnectFromPeerMessage);
    assertTrue(messageReceivedByClientLatch.await(TIMEOUT, TIMEOUT_UNIT));
    assertThat(receivedMessage, is(disconnectFromPeerMessage));

    ServerMessage parsedMessage = parseServerString("""
                                                        {
                                                            "command" : "DisconnectFromPeer",
                                                            "target" : "game",
                                                            "args" : [ 1 ]
                                                          }""");

    assertThat(parsedMessage, equalTo(disconnectFromPeerMessage));
  }
}
