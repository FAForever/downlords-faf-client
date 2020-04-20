package com.faforever.client.remote;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.CloseGameEvent;
import com.faforever.client.game.Faction;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.FactionDeserializer;
import com.faforever.client.legacy.ServerMessageSerializer;
import com.faforever.client.legacy.UidService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.rankedmatch.MatchmakerInfoMessage.MatchmakerQueue.QueueName;
import com.faforever.client.rankedmatch.SearchLadder1v1ClientMessage;
import com.faforever.client.rankedmatch.StopSearchLadder1v1ClientMessage;
import com.faforever.client.remote.domain.ClientMessageType;
import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.InitSessionMessage;
import com.faforever.client.remote.domain.LoginClientMessage;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.remote.domain.SessionMessage;
import com.faforever.client.remote.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.remote.gson.MessageTargetTypeAdapter;
import com.faforever.client.remote.gson.RatingRangeTypeAdapter;
import com.faforever.client.remote.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.remote.io.QDataInputStream;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.test.FakeTestException;
import com.google.common.eventbus.EventBus;
import com.google.common.hash.Hashing;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerAccessorImplTest extends AbstractPlainJavaFxTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final Gson gson = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
      .registerTypeAdapter(MessageTarget.class, MessageTargetTypeAdapter.INSTANCE)
      .registerTypeAdapter(Faction.class, new FactionDeserializer())
      .registerTypeAdapter(RatingRange.class, RatingRangeTypeAdapter.INSTANCE)
      .create();

  @Rule
  public TemporaryFolder faDirectory = new TemporaryFolder();

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
  @Mock
  private ClientProperties clientProperties;

  private FafServerAccessorImpl instance;
  private ServerSocket fafLobbyServerSocket;
  private Socket localToServerSocket;
  private ServerWriter serverToClientWriter;
  private boolean stopped;
  private BlockingQueue<String> messagesReceivedByFafServer;
  private CountDownLatch serverToClientReadyLatch;

  @Before
  public void setUp() throws Exception {
    serverToClientReadyLatch = new CountDownLatch(1);
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);

    startFakeFafLobbyServer();

    ClientProperties clientProperties = new ClientProperties();
    clientProperties.getServer()
        .setHost(LOOPBACK_ADDRESS.getHostAddress())
        .setPort(fafLobbyServerSocket.getLocalPort());

    instance = new FafServerAccessorImpl(preferencesService, uidService, notificationService, i18n, reportingService, taskScheduler, eventBus, reconnectTimerService, clientProperties);
    instance.afterPropertiesSet();
    LoginPrefs loginPrefs = new LoginPrefs();
    loginPrefs.setUsername("junit");
    loginPrefs.setPassword("password");

    when(preferencesService.getFafDataDirectory()).thenReturn(faDirectory.getRoot().toPath());
    when(uidService.generate(any(), any())).thenReturn("encrypteduidstring");
  }

  private void startFakeFafLobbyServer() throws IOException {
    fafLobbyServerSocket = new ServerSocket(0);
    logger.info("Fake server listening on " + fafLobbyServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {

      try (Socket socket = fafLobbyServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToClientWriter = new ServerWriter(socket.getOutputStream());
        serverToClientWriter.registerMessageSerializer(new ServerMessageSerializer(), FafServerMessage.class);

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

  @After
  public void tearDown() {
    IOUtils.closeQuietly(fafLobbyServerSocket);
    IOUtils.closeQuietly(localToServerSocket);
  }

  @Test
  public void testConnectAndLogIn() throws Exception {
    int playerUid = 123;
    String username = "JunitUser";
    String password = "JunitPassword";
    long sessionId = 456;

    CompletableFuture<LoginMessage> loginFuture = instance.connectAndLogIn(username, password).toCompletableFuture();

    String json = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    InitSessionMessage initSessionMessage = gson.fromJson(json, InitSessionMessage.class);

    assertThat(initSessionMessage.getCommand(), is(ClientMessageType.ASK_SESSION));

    SessionMessage sessionMessage = new SessionMessage();
    sessionMessage.setSession(sessionId);
    sendFromServer(sessionMessage);

    json = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    LoginClientMessage loginClientMessage = gson.fromJson(json, LoginClientMessage.class);

    assertThat(loginClientMessage.getCommand(), is(ClientMessageType.LOGIN));
    assertThat(loginClientMessage.getLogin(), is(username));
    assertThat(loginClientMessage.getPassword(), is(Hashing.sha256().hashString(password, UTF_8).toString()));
    assertThat(loginClientMessage.getSession(), is(sessionId));
    assertThat(loginClientMessage.getUniqueId(), is("encrypteduidstring"));

    LoginMessage loginServerMessage = new LoginMessage();
    loginServerMessage.setId(playerUid);
    loginServerMessage.setLogin(username);

    sendFromServer(loginServerMessage);

    LoginMessage result = loginFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(result.getMessageType(), is(FafServerMessageType.WELCOME));
    assertThat(result.getId(), is(playerUid));
    assertThat(result.getLogin(), is(username));

    instance.disconnect();
  }

  /**
   * Writes the specified message to the client as if it was sent by the FAF server.
   */
  private void sendFromServer(FafServerMessage fafServerMessage) throws InterruptedException {
    serverToClientReadyLatch.await();
    serverToClientWriter.write(fafServerMessage);
  }

  private void connectAndLogIn() throws Exception {
    CompletableFuture<LoginMessage> loginFuture = instance.connectAndLogIn("JUnit", "JUnitPassword").toCompletableFuture();

    assertNotNull(messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT));

    SessionMessage sessionMessage = new SessionMessage();
    sessionMessage.setSession(5678);
    sendFromServer(sessionMessage);

    assertNotNull(messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT));

    LoginMessage loginServerMessage = new LoginMessage();
    loginServerMessage.setId(123);
    loginServerMessage.setLogin("JUnitUser");

    sendFromServer(loginServerMessage);

    assertNotNull(loginFuture.get(TIMEOUT, TIMEOUT_UNIT));
  }

  @Test
  public void testRankedMatchNotification() throws Exception {
    connectAndLogIn();

    MatchmakerInfoMessage matchmakerMessage = new MatchmakerInfoMessage();
    matchmakerMessage.setQueues(singletonList(new MatchmakerInfoMessage.MatchmakerQueue(QueueName.LADDER_1V1, null, singletonList(new RatingRange(100, 200)), singletonList(new RatingRange(100, 200)))));

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
    connectAndLogIn();

    NoticeMessage noticeMessage = new NoticeMessage();
    noticeMessage.setText("foo bar");
    noticeMessage.setStyle("warning");

    when(i18n.get("messageFromServer")).thenReturn("Message from Server");

    sendFromServer(noticeMessage);

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService, timeout(1000)).addNotification(captor.capture());

    ImmediateNotification notification = captor.getValue();
    assertThat(notification.getSeverity(), is(Severity.WARN));
    assertThat(notification.getText(), is("foo bar"));
    assertThat(notification.getTitle(), is("Message from Server"));
    verify(i18n).get("messageFromServer");

    instance.disconnect();
  }

  @Test
  public void onKillNoticeStopsGame() throws Exception {
    connectAndLogIn();

    NoticeMessage noticeMessage = new NoticeMessage();
    noticeMessage.setStyle("kill");

    sendFromServer(noticeMessage);

    verify(eventBus, timeout(1000)).post(any(CloseGameEvent.class));

    instance.disconnect();
  }

  @Test
  public void onKickNoticeStopsApplication() throws Exception {
    connectAndLogIn();

    NoticeMessage noticeMessage = new NoticeMessage();
    noticeMessage.setStyle("kick");

    sendFromServer(noticeMessage);

    verify(taskScheduler, timeout(1000)).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

    instance.disconnect();
  }

  @Test
  public void startSearchLadder1v1WithAeon() throws Exception {
    connectAndLogIn();

    CompletableFuture<GameLaunchMessage> future = instance.startSearchLadder1v1(Faction.AEON).toCompletableFuture();

    String clientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    SearchLadder1v1ClientMessage searchRanked1v1Message = gson.fromJson(clientMessage, SearchLadder1v1ClientMessage.class);

    assertThat(searchRanked1v1Message, instanceOf(SearchLadder1v1ClientMessage.class));
    assertThat(searchRanked1v1Message.getFaction(), is(Faction.AEON));

    GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
    gameLaunchMessage.setUid(1234);
    sendFromServer(gameLaunchMessage);

    assertThat(future.get(TIMEOUT, TIMEOUT_UNIT).getUid(), is(gameLaunchMessage.getUid()));

    instance.disconnect();
  }

  @Test
  public void stopSearchingLadder1v1Match() throws Exception {
    connectAndLogIn();

    instance.stopSearchingRanked();

    String clientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    StopSearchLadder1v1ClientMessage stopSearchRanked1v1Message = gson.fromJson(clientMessage, StopSearchLadder1v1ClientMessage.class);
    assertThat(stopSearchRanked1v1Message, instanceOf(StopSearchLadder1v1ClientMessage.class));
    assertThat(stopSearchRanked1v1Message.getCommand(), is(ClientMessageType.GAME_MATCH_MAKING));

    instance.disconnect();
  }

  @Test
  public void onUIDNotFound() throws Exception {
    instance.onUIDNotExecuted(new FakeTestException("UID not found"));
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
