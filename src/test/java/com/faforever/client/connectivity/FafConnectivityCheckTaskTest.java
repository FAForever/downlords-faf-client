package com.faforever.client.connectivity;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.relay.GpgServerMessage;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.relay.SendNatPacketMessage;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.upnp.UpnpService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.util.SocketUtils.PORT_RANGE_MAX;
import static org.springframework.util.SocketUtils.PORT_RANGE_MIN;

public class FafConnectivityCheckTaskTest extends AbstractPlainJavaFxTest {

  public static final int GAME_PORT = 6112;
  private FafConnectivityCheckTask instance;

  @Mock
  private ExecutorService executorService;
  @Mock
  private I18n i18n;
  @Mock
  private LobbyServerAccessor lobbyServerAccessor;
  @Mock
  private UpnpService upnpService;
  @Captor
  private ArgumentCaptor<Consumer<GpgServerMessage>> connectivityMessageListenerCaptor;

  @Before
  public void setUp() throws Exception {
    instance = new FafConnectivityCheckTask();
    instance.executorService = executorService;
    instance.i18n = i18n;
    instance.lobbyServerAccessor = lobbyServerAccessor;
    instance.upnpService = upnpService;

    doAnswer(invocation -> {
      CompletableFuture.runAsync(invocation.getArgumentAt(0, Runnable.class));
      return null;
    }).when(executorService).execute(any(Runnable.class));
  }

  @Test(expected = IllegalStateException.class)
  public void testCallPortNotSetThrowsIse() throws Exception {
    instance.call();
  }

  @Test
  public void testPublic() throws Exception {
    int playerId = 1234;

    doAnswer(invocation -> {
      int port = invocation.getArgumentAt(0, int.class);

      try (DatagramSocket socket = new DatagramSocket(0)) {
        byte[] bytes = String.format("Are you public? %s", playerId).getBytes(UTF_8);
        DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
        datagramPacket.setAddress(InetAddress.getLocalHost());
        datagramPacket.setPort(port);
        socket.send(datagramPacket);
      }
      return null;
    }).when(lobbyServerAccessor).initConnectivityTest(GAME_PORT);

    doAnswer(invocation -> {
      ProcessNatPacketMessage processNatPacketMessage = invocation.getArgumentAt(0, ProcessNatPacketMessage.class);

      InetAddress expectedAddress = InetAddress.getLocalHost();
      InetAddress actualAddress = processNatPacketMessage.getAddress().getAddress();
      int actualPort = processNatPacketMessage.getAddress().getPort();

      assertThat(processNatPacketMessage.getTarget(), is(MessageTarget.CONNECTIVITY));
      assertThat(actualAddress, is(expectedAddress));
      assertThat(actualPort, is(both(greaterThan(PORT_RANGE_MIN)).and(lessThan(PORT_RANGE_MAX))));
      assertThat(processNatPacketMessage.getMessage(), is("Are you public? " + playerId));

      verify(lobbyServerAccessor).addOnMessageListener(eq(GpgServerMessage.class), connectivityMessageListenerCaptor.capture());
      connectivityMessageListenerCaptor.getValue().accept(new ConnectivityStateMessage(ConnectivityState.PUBLIC));

      return null;
    }).when(lobbyServerAccessor).sendGpgMessage(any());

    instance.setPort(GAME_PORT);

    assertThat(instance.call(), is(ConnectivityState.PUBLIC));
    verify(upnpService).forwardPort(GAME_PORT);
    verify(lobbyServerAccessor).initConnectivityTest(GAME_PORT);
  }

  @Test
  public void testStun() throws Exception {
    int playerId = 1234;

    doAnswer(invocation -> {
      verify(lobbyServerAccessor).addOnMessageListener(eq(GpgServerMessage.class), connectivityMessageListenerCaptor.capture());

      SendNatPacketMessage sendNatPacketMessage = new SendNatPacketMessage();
      sendNatPacketMessage.setTarget(MessageTarget.CONNECTIVITY);
      sendNatPacketMessage.setMessage("Hello " + playerId);

      try (DatagramSocket datagramSocket = new DatagramSocket(new InetSocketAddress(InetAddress.getLocalHost(), 0))) {
        sendNatPacketMessage.setPublicAddress((InetSocketAddress) datagramSocket.getLocalSocketAddress());
        connectivityMessageListenerCaptor.getValue().accept(sendNatPacketMessage);

        byte[] bytes = new byte[64];
        DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
        datagramSocket.receive(datagramPacket);

        assertThat(new String(datagramPacket.getData(), 0, datagramPacket.getLength(), UTF_8), is("\u0008Hello " + playerId));

        connectivityMessageListenerCaptor.getValue().accept(new ConnectivityStateMessage(ConnectivityState.STUN));
      }
      return null;
    }).when(lobbyServerAccessor).initConnectivityTest(GAME_PORT);

    instance.setPort(GAME_PORT);

    assertThat(instance.call(), is(ConnectivityState.STUN));
    verify(upnpService).forwardPort(GAME_PORT);
    verify(lobbyServerAccessor).initConnectivityTest(GAME_PORT);
  }
}