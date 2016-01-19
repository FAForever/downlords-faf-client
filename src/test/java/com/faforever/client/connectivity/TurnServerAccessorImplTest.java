package com.faforever.client.connectivity;

import com.faforever.client.relay.CreatePermissionMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.apache.commons.compress.utils.IOUtils;
import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.LifetimeAttribute;
import org.ice4j.message.Message;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;
import org.ice4j.message.Response;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stack.StunStack;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.ice4j.Transport.UDP;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class TurnServerAccessorImplTest extends AbstractPlainJavaFxTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private TurnServerAccessorImpl instance;
  private DatagramSocket turnServerSocket;

  @Mock
  private FafService fafService;
  @Mock
  private ScheduledExecutorService scheduledExecutorService;
  @Captor
  private ArgumentCaptor<Consumer<CreatePermissionMessage>> createPermissionListenerCaptor;

  private BlockingQueue<StunMessageEvent> eventsReceivedByTurnServer;
  private StunStack stunStack;

  @Before
  public void setUp() throws Exception {
    DatagramSocket serverSocket = startFakeTurnServer();

    instance = new TurnServerAccessorImpl();
    instance.scheduledExecutorService = scheduledExecutorService;
    instance.fafService = fafService;
    instance.turnHost = serverSocket.getLocalAddress().getHostAddress();
    instance.turnPort = serverSocket.getLocalPort();

    eventsReceivedByTurnServer = new LinkedBlockingQueue<>();

    Mockito.doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      return null;
    }).when(scheduledExecutorService).execute(any());

    instance.postConstruct();
  }

  private DatagramSocket startFakeTurnServer() throws Exception {
    turnServerSocket = new DatagramSocket(0, InetAddress.getLocalHost());
    logger.info("Fake server listening on " + turnServerSocket.getLocalSocketAddress());

    stunStack = new StunStack();
    stunStack.addSocket(new IceUdpSocketWrapper(turnServerSocket));
    stunStack.addRequestListener(evt -> eventsReceivedByTurnServer.add(evt));

    return turnServerSocket;
  }

  @After
  public void tearDown() throws Exception {
    IOUtils.closeQuietly(turnServerSocket);
  }

  @Test
  public void testConnect() throws Exception {
    WaitForAsyncUtils.async(() -> {
      handleAllocationRequest();
      handleRefreshRequest();
      return null;
    });

    CountDownLatch refreshRequestLatch = new CountDownLatch(1);
    doAnswer(invocation -> {
      invocation.getArgumentAt(0, Runnable.class).run();
      refreshRequestLatch.countDown();
      return null;
    }).when(scheduledExecutorService).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());

    instance.ensureConnected();
    InetSocketAddress relayAddress = instance.getRelayAddress();
    assertThat(relayAddress.getAddress().getHostAddress(), is(turnServerSocket.getLocalAddress().getHostAddress()));
    assertThat(relayAddress.getPort(), is(2222));

    verify(scheduledExecutorService).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
    assertTrue(refreshRequestLatch.await(3, TimeUnit.SECONDS));
  }

  private void handleAllocationRequest() throws StunException, IOException, InterruptedException {
    StunMessageEvent event = eventsReceivedByTurnServer.poll(5, TimeUnit.SECONDS);
    TransportAddress localAddress = event.getLocalAddress();
    TransportAddress remoteAddress = event.getRemoteAddress();

    Request request = (Request) event.getMessage();
    assertThat(request.getMessageType(), is(Message.ALLOCATE_REQUEST));
    Response allocationResponse = MessageFactory.createAllocationResponse(request, remoteAddress, new TransportAddress(localAddress.getHostAddress(), 2222, UDP), 100);

    sendResponse(remoteAddress, request, allocationResponse);
  }

  private void handleRefreshRequest() throws StunException, IOException, InterruptedException {
    StunMessageEvent event = eventsReceivedByTurnServer.poll(5, TimeUnit.SECONDS);
    TransportAddress remoteAddress = event.getRemoteAddress();

    Request request = (Request) event.getMessage();
    assertThat(request.getMessageType(), is(Message.REFRESH_REQUEST));
    assertThat(((LifetimeAttribute) request.getAttribute(Attribute.LIFETIME)).getLifetime(), is(33));
    Response refreshResponse = MessageFactory.createRefreshResponse(33);

    sendResponse(remoteAddress, request, refreshResponse);
  }

  private void sendResponse(TransportAddress remoteAddress, Request request, Response channelBindResponse) throws StunException, IOException {
    TransportAddress sendThrough = new TransportAddress((InetSocketAddress) turnServerSocket.getLocalSocketAddress(), UDP);
    stunStack.sendResponse(request.getTransactionID(), channelBindResponse, sendThrough, remoteAddress);
  }

  @Test
  public void testClose() throws Exception {
    instance.disconnect();
  }

  @Test
  public void testGetRelayAddress() throws Exception {
    assertThat(instance.getRelayAddress(), nullValue());

    WaitForAsyncUtils.async(() -> {
      handleAllocationRequest();
      handleCreatePermissionRequest();
      handleChannelBindRequest();

      return null;
    });

    instance.ensureConnected();

    InetSocketAddress relayAddress = instance.getRelayAddress();
    assertThat(relayAddress.getAddress().getHostAddress(), is(turnServerSocket.getLocalAddress().getHostAddress()));
    assertThat(relayAddress.getPort(), is(2222));
  }

  private void handleCreatePermissionRequest() throws StunException, IOException, InterruptedException {
    StunMessageEvent event = eventsReceivedByTurnServer.poll(5, TimeUnit.SECONDS);
    TransportAddress remoteAddress = event.getRemoteAddress();

    Request request = (Request) event.getMessage();
    assertThat(request.getMessageType(), is(Message.CREATEPERMISSION_REQUEST));
    Response createPermissionResponse = MessageFactory.createCreatePermissionResponse();

    sendResponse(remoteAddress, request, createPermissionResponse);
  }

  private void handleChannelBindRequest() throws InterruptedException, IOException, StunException {
    StunMessageEvent event = eventsReceivedByTurnServer.poll(5, TimeUnit.SECONDS);
    TransportAddress remoteAddress = event.getRemoteAddress();

    Request request = (Request) event.getMessage();
    assertThat(request.getMessageType(), is(Message.CHANNELBIND_REQUEST));
    Response channelBindResponse = MessageFactory.createChannelBindResponse();

    sendResponse(remoteAddress, request, channelBindResponse);
  }

  @Test
  public void testSend() throws Exception {
    WaitForAsyncUtils.async(() -> {
      handleAllocationRequest();
      handleCreatePermissionRequest();
      handleChannelBindRequest();

      return null;
    });

    instance.ensureConnected();

    InetSocketAddress remotePeerAddress = new InetSocketAddress("93.184.216.34", 1234);

    CreatePermissionMessage createPermissionMessage = new CreatePermissionMessage();
    createPermissionMessage.setAddress(remotePeerAddress);

    verify(fafService).addOnMessageListener(eq(CreatePermissionMessage.class), createPermissionListenerCaptor.capture());
    createPermissionListenerCaptor.getValue().accept(createPermissionMessage);

    byte[] bytes = new byte[1024];
    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
    datagramPacket.setSocketAddress(remotePeerAddress);

    instance.send(datagramPacket);
  }
}
