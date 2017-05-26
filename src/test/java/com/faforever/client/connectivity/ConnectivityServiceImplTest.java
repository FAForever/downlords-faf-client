package com.faforever.client.connectivity;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.relay.SendNatPacketMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.user.UserService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.util.SocketUtils;
import org.testfx.util.WaitForAsyncUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ConnectivityServiceImplTest extends AbstractPlainJavaFxTest {

  private ConnectivityServiceImpl instance;

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private NotificationService notificationService;
  @Mock
  private FafService fafService;
  @Mock
  private ThreadPoolExecutor threadPoolExecutor;
  @Mock
  private TurnServerAccessor turnServerAccessor;
  @Mock
  private UserService userService;

  @Captor
  private ArgumentCaptor<Consumer<SendNatPacketMessage>> sendNatPacketMessageListenerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<LoginMessage>> loginMessageListenerCaptor;

  private BooleanProperty loggedInProperty;
  private ObjectProperty<ConnectionState> connectionStateProperty;

  @Before
  public void setUp() throws Exception {
    instance = new ConnectivityServiceImpl();
    instance.taskService = taskService;
    instance.preferencesService = preferencesService;
    i18n = instance.i18n = i18n;
    instance.platformService = platformService;
    instance.applicationContext = applicationContext;
    instance.notificationService = notificationService;
    instance.fafService = fafService;
    instance.threadPoolExecutor = threadPoolExecutor;
    instance.turnServerAccessor = turnServerAccessor;
    instance.userService = userService;

    IntegerProperty portProperty = new SimpleIntegerProperty(SocketUtils.findAvailableUdpPort());
    connectionStateProperty = new SimpleObjectProperty<>();
    loggedInProperty = new SimpleBooleanProperty();

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPort()).thenReturn(portProperty.get());
    when(forgedAlliancePrefs.portProperty()).thenReturn(portProperty);
    when(fafService.connectionStateProperty()).thenReturn(connectionStateProperty);

    doAnswer(invocation -> invocation.getArgument(0)).when(taskService).submitTask(any());

    doAnswer(invocation -> {
      WaitForAsyncUtils.async((Runnable) invocation.getArgument(0));
      return null;
    }).when(threadPoolExecutor).execute(any());

    instance.postConstruct();

    verify(fafService).addOnMessageListener(eq(SendNatPacketMessage.class), sendNatPacketMessageListenerCaptor.capture());
    verify(fafService).addOnMessageListener(eq(LoginMessage.class), loginMessageListenerCaptor.capture());
  }

  @Test
  public void testConnectivityStateInitiallyUnknown() throws Exception {
    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));
  }

  @Test
  public void testConnectivityCheckTriggeredByLoginMessage() throws Exception {
    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));
    verifyZeroInteractions(taskService);

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();
    ConnectivityCheckTask connectivityCheckTask = mockConnectivityCheckTask();

    loginMessageListenerCaptor.getValue().accept(new LoginMessage());

    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(taskService).submitTask(connectivityCheckTask);
  }

  @Test
  public void testConnectivityStateResetOnDisconnect() throws Exception {
    mockUpnpPortForwardingTask();

    ConnectivityCheckTask connectivityCheckTask = mockConnectivityCheckTask();
    when(connectivityCheckTask.getFuture()).thenReturn(completedFuture(new ConnectivityStateMessage(ConnectivityState.PUBLIC, new InetSocketAddress(1337))));
    when(taskService.submitTask(connectivityCheckTask)).thenReturn(connectivityCheckTask);

    instance.checkConnectivity();

    assertThat(instance.getConnectivityState(), is(ConnectivityState.PUBLIC));
    connectionStateProperty.set(ConnectionState.DISCONNECTED);

    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));
  }

  private UpnpPortForwardingTask mockUpnpPortForwardingTask() {
    UpnpPortForwardingTask upnpPortForwardingTask = mock(UpnpPortForwardingTask.class);
    when(upnpPortForwardingTask.getFuture()).thenReturn(completedFuture(null));
    when(applicationContext.getBean(UpnpPortForwardingTask.class)).thenReturn(upnpPortForwardingTask);
    return upnpPortForwardingTask;
  }

  private ConnectivityCheckTask mockConnectivityCheckTask() {
    ConnectivityCheckTask connectivityCheckTask = mock(ConnectivityCheckTask.class);
    when(applicationContext.getBean(ConnectivityCheckTask.class)).thenReturn(connectivityCheckTask);
    return connectivityCheckTask;
  }

  @Test(expected = IllegalStateException.class)
  public void testInitConnectionThrowsIseWhenConnectivityStateIsUnknown() throws Exception {
    ((ObjectProperty<ConnectivityState>) instance.connectivityStateProperty()).set(ConnectivityState.UNKNOWN);
    instance.connect();
  }

  @Test
  public void testInitConnectionUsesDirectConnectionWhenConnectivityStateIsPublic() throws Exception {
    ((ObjectProperty<ConnectivityState>) instance.connectivityStateProperty()).set(ConnectivityState.PUBLIC);
    instance.connect();

    verify(turnServerAccessor, never()).disconnect();

    verifySendThroughPublicSocket();

    verify(turnServerAccessor, never()).send(any());
  }

  private void verifySendThroughPublicSocket() throws Exception {
    try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0))) {
      Future<DatagramPacket> packetFuture = WaitForAsyncUtils.async(() -> {
        DatagramPacket datagramPacket = new DatagramPacket(new byte[5], 5);
        socket.receive(datagramPacket);
        return datagramPacket;
      });

      byte[] data = "Hello".getBytes(StandardCharsets.US_ASCII);
      DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
      datagramPacket.setSocketAddress(socket.getLocalSocketAddress());

      instance.send(datagramPacket);

      DatagramPacket packet = packetFuture.get(2, TimeUnit.SECONDS);
      assertArrayEquals(data, packet.getData());
    }
  }

  @Test
  public void testInitConnectionUsesTurnWhenConnectivityStateIsStunAndPeerIsBound() throws Exception {
    ((ObjectProperty<ConnectivityState>) instance.connectivityStateProperty()).set(ConnectivityState.STUN);
    instance.connect();

    verify(turnServerAccessor).addOnPacketListener(any());
    verify(turnServerAccessor).connect();

    when(turnServerAccessor.isBound(any())).thenReturn(true);

    byte[] data = "Hello".getBytes(StandardCharsets.US_ASCII);
    DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
    datagramPacket.setSocketAddress(new InetSocketAddress("example.com", 1));
    instance.send(datagramPacket);

    verify(turnServerAccessor).send(datagramPacket);
  }

  @Test
  public void testInitConnectionUsesDirectConnectionWhenConnectivityStateIsStunButPeerIsNotBound() throws Exception {
    ((ObjectProperty<ConnectivityState>) instance.connectivityStateProperty()).set(ConnectivityState.STUN);
    instance.connect();

    verify(turnServerAccessor).addOnPacketListener(any());
    verify(turnServerAccessor).connect();

    when(turnServerAccessor.isBound(any())).thenReturn(false);

    verifySendThroughPublicSocket();
    verify(turnServerAccessor, never()).send(any());
  }

  @Test(expected = IllegalStateException.class)
  public void testInitConnectionThrowsExceptionWhenConnectivityStateIsBlocked() throws Exception {
    ((ObjectProperty<ConnectivityState>) instance.connectivityStateProperty()).set(ConnectivityState.BLOCKED);
    instance.connect();

    verify(turnServerAccessor).addOnPacketListener(any());
    verify(turnServerAccessor).connect();

    DatagramPacket datagramPacket = new DatagramPacket(new byte[1], 1);
    datagramPacket.setSocketAddress(new InetSocketAddress("example.com", 1));
    instance.send(datagramPacket);

    verify(turnServerAccessor).send(datagramPacket);
  }

  @Test
  public void testGetConnectivityStateInitiallyUnknown() throws Exception {
    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));
  }

  @Test
  public void testSendNatPacket() throws Exception {
    String message = "/PLAYERID 21447 Downlord";
    DatagramSocket datagramSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0));

    SendNatPacketMessage sendNatPacketMessage = new SendNatPacketMessage();
    sendNatPacketMessage.setPublicAddress((InetSocketAddress) datagramSocket.getLocalSocketAddress());
    sendNatPacketMessage.setMessage(message);
    sendNatPacketMessageListenerCaptor.getValue().accept(sendNatPacketMessage);

    byte[] bytes = new byte[128];
    DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length);
    datagramSocket.receive(packet);

    assertThat(new String(packet.getData(), 1, packet.getLength() - 1, StandardCharsets.US_ASCII), is(message));
  }

  @Test
  public void testCheckGamePortInBackgroundBlockedTriggersNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mockConnectivityCheckTask();
    when(connectivityCheckTask.getFuture()).thenReturn(completedFuture(new ConnectivityStateMessage(
        ConnectivityState.BLOCKED, new InetSocketAddress(51111)
    )));

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().toCompletableFuture().get(1, TimeUnit.SECONDS);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.BLOCKED));

    verify(taskService).submitTask(connectivityCheckTask);
    verify(connectivityCheckTask).setPublicPort(anyInt());
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verify(notificationService).addNotification(any(PersistentNotification.class));
    assertThat(instance.getConnectivityState(), is(ConnectivityState.BLOCKED));
  }

  @Test
  public void testCheckGamePortInBackgroundStunDoesntTriggerNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mockConnectivityCheckTask();
    when(connectivityCheckTask.getFuture()).thenReturn(
        completedFuture(new ConnectivityStateMessage(
            ConnectivityState.STUN, new InetSocketAddress(51111)
        ))
    );

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().toCompletableFuture().get(1, TimeUnit.SECONDS);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.STUN));

    verify(taskService).submitTask(connectivityCheckTask);
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verifyZeroInteractions(notificationService);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.STUN));
  }

  @Test
  public void testCheckGamePortInBackgroundPublicDoesntTriggerNotification() throws Exception {
    ConnectivityCheckTask connectivityCheckTask = mockConnectivityCheckTask();
    when(connectivityCheckTask.getFuture()).thenReturn(completedFuture(
        new ConnectivityStateMessage(ConnectivityState.PUBLIC, new InetSocketAddress(51111)))
    );
    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().toCompletableFuture().get(1, TimeUnit.SECONDS);

    verify(taskService).submitTask(connectivityCheckTask);
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verifyZeroInteractions(notificationService);
    assertThat(instance.getConnectivityState(), is(ConnectivityState.PUBLIC));
  }

  @Test
  public void testCheckGamePortInBackgroundFailsTriggersNotification() throws Exception {
    ArgumentCaptor<PersistentNotification> persistentNotificationCaptor = ArgumentCaptor.forClass(PersistentNotification.class);
    CompletableFuture<ConnectivityStateMessage> completableFuture = new CompletableFuture<>();
    completableFuture.completeExceptionally(new Exception("junit test exception"));

    ConnectivityCheckTask connectivityCheckTask = mockConnectivityCheckTask();
    when(connectivityCheckTask.getFuture()).thenReturn(completableFuture);

    UpnpPortForwardingTask upnpPortForwardingTask = mockUpnpPortForwardingTask();

    instance.checkConnectivity().toCompletableFuture().get(1, TimeUnit.SECONDS);

    verify(taskService).submitTask(connectivityCheckTask);
    verify(taskService).submitTask(upnpPortForwardingTask);
    verify(upnpPortForwardingTask).setPort(anyInt());
    verify(notificationService).addNotification(persistentNotificationCaptor.capture());
    assertThat(instance.getConnectivityState(), is(ConnectivityState.UNKNOWN));

    assertThat(persistentNotificationCaptor.getValue().getSeverity(), is(Severity.WARN));
    assertThat(persistentNotificationCaptor.getValue().getActions(), hasSize(1));
  }

  @After
  public void tearDown() throws Exception {
    instance.preDestroy();
  }

  @Test
  public void testHandleSendNatPacket() throws Exception {
    CompletableFuture<ProcessNatPacketMessage> messageFuture = new CompletableFuture<>();
    doAnswer(invocation -> {
      messageFuture.complete(invocation.getArgument(0));
      return null;
    }).when(fafService).sendGpgGameMessage(any());

    InetSocketAddress publicSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), instance.getPublicSocketAddress().getPort());

    try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0))) {
      byte[] data = "\bFoo".getBytes(StandardCharsets.US_ASCII);
      DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
      datagramPacket.setSocketAddress(publicSocketAddress);

      socket.send(datagramPacket);
    }

    assertThat(messageFuture.get(2, TimeUnit.SECONDS).getMessage(), is("Foo"));
  }

  @Test
  public void testGetRelayAddress() throws Exception {
    verifyZeroInteractions(turnServerAccessor);
    instance.getRelayAddress();
    verify(turnServerAccessor).getRelayAddress();
  }

  @Test
  public void testAddOnPackageListener() throws Exception {
    CompletableFuture<DatagramPacket> packetFuture = new CompletableFuture<>();
    instance.addOnPacketListener(packetFuture::complete);

    InetSocketAddress publicSocketAddress = new InetSocketAddress(InetAddress.getLocalHost(), instance.getPublicSocketAddress().getPort());

    try (DatagramSocket socket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0))) {
      byte[] data = "Hello".getBytes(StandardCharsets.US_ASCII);
      DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
      datagramPacket.setSocketAddress(publicSocketAddress);

      socket.send(datagramPacket);
    }

    assertThat(packetFuture.get(2, TimeUnit.SECONDS).getLength(), is(greaterThan(0)));
  }

  @Test
  public void testRemoveOnPackageListener() throws Exception {
    // No idea how to test NOT being called in async context (waiting for timeout? Meh.)
    instance.removeOnPacketListener(datagramPacket -> {
    });
  }
}
