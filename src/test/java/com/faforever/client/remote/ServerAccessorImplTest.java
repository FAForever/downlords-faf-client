package com.faforever.client.remote;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fa.CloseGameEvent;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.ServerMessageSerializer;
import com.faforever.client.legacy.UidService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.MatchmakerInfoMessage;
import com.faforever.client.remote.domain.ClientMessageType;
import com.faforever.client.remote.domain.FafServerMessage;
import com.faforever.client.remote.domain.FafServerMessageType;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.NoticeMessage;
import com.faforever.client.remote.domain.PlayerInfo;
import com.faforever.client.remote.domain.RatingRange;
import com.faforever.client.remote.domain.SessionMessage;
import com.faforever.client.remote.io.QDataInputStream;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.UITest;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
  @Mock
  private ClientProperties clientProperties;

  private FafServerAccessorImpl instance;
  private ServerSocket fafLobbyServerSocket;
  private Socket localToServerSocket;
  private ServerWriter serverToClientWriter;
  private boolean stopped;
  private BlockingQueue<String> messagesReceivedByFafServer;
  private CountDownLatch serverToClientReadyLatch;

  @BeforeEach
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
    loginPrefs.setRefreshToken("junit");

    when(preferencesService.getFafDataDirectory()).thenReturn(faDirectory);
    when(uidService.generate(any(), any())).thenReturn("encrypteduidstring");
  }

  private void startFakeFafLobbyServer() throws IOException {
    fafLobbyServerSocket = new ServerSocket(0);
    log.info("Fake server listening on " + fafLobbyServerSocket.getLocalPort());

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

  @AfterEach
  public void tearDown() {
    IOUtils.closeQuietly(fafLobbyServerSocket);
    IOUtils.closeQuietly(localToServerSocket);
  }

  @Test
  public void testConnectAndLogIn() throws Exception {
    int playerUid = 123;
    String token = "abc";
    long sessionId = 456;

    CompletableFuture<LoginMessage> loginFuture = instance.connectAndLogin(token).toCompletableFuture();

    String initSessionJSON = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(initSessionJSON, containsString(ClientMessageType.ASK_SESSION.getString()));

    SessionMessage sessionMessage = new SessionMessage();
    sessionMessage.setSession(sessionId);
    sendFromServer(sessionMessage);

    String loginClientJSON = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(loginClientJSON, containsString(ClientMessageType.OAUTH_LOGIN.getString()));
    assertThat(loginClientJSON, containsString(token));
    assertThat(loginClientJSON, containsString(String.valueOf(sessionId)));
    assertThat(loginClientJSON, containsString("encrypteduidstring"));

    LoginMessage loginServerMessage = new LoginMessage();
    PlayerInfo me = new PlayerInfo(playerUid, "Junit", null, null, null, null, null, null);

    loginServerMessage.setMe(me);

    sendFromServer(loginServerMessage);

    LoginMessage result = loginFuture.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(result.getMessageType(), is(FafServerMessageType.WELCOME));
    assertThat(result.getMe().getId(), is(playerUid));
    assertThat(result.getMe().getLogin(), is("Junit"));

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
    CompletableFuture<LoginMessage> loginFuture = instance.connectAndLogin("JUnit").toCompletableFuture();

    assertNotNull(messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT));

    SessionMessage sessionMessage = new SessionMessage();
    sessionMessage.setSession(5678);
    sendFromServer(sessionMessage);

    assertNotNull(messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT));

    LoginMessage loginServerMessage = new LoginMessage();

    PlayerInfo me = new PlayerInfo(123, "Junit", null, null, null, null, null, null);

    loginServerMessage.setMe(me);

    sendFromServer(loginServerMessage);

    assertNotNull(loginFuture.get(TIMEOUT, TIMEOUT_UNIT));
  }

  @Test
  public void testRankedMatchNotification() throws Exception {
    connectAndLogIn();

    MatchmakerInfoMessage matchmakerMessage = new MatchmakerInfoMessage();
    String timeString = DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(65)); // TODO: this is used in multiple tests, extract
    matchmakerMessage.setQueues(singletonList(new MatchmakerInfoMessage.MatchmakerQueue("ladder1v1", timeString, 1, 0, singletonList(new RatingRange(100, 200)), singletonList(new RatingRange(100, 200)))));

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
  public void onUIDNotFound() throws Exception {
    instance.onUIDNotExecuted(new FakeTestException("UID not found"));
    verify(notificationService).addNotification(any(ImmediateNotification.class));
  }
}
