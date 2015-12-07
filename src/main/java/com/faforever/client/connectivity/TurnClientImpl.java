package com.faforever.client.connectivity;

import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.relay.CreatePermissionMessage;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.compress.utils.IOUtils;
import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.StunResponseEvent;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.ErrorCodeAttribute;
import org.ice4j.attribute.LifetimeAttribute;
import org.ice4j.attribute.XorMappedAddressAttribute;
import org.ice4j.attribute.XorRelayedAddressAttribute;
import org.ice4j.message.ChannelData;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;
import org.ice4j.message.Response;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stack.StunStack;
import org.ice4j.stack.TransactionID;
import org.ice4j.stunclient.BlockingRequestSender;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.ice4j.attribute.Attribute.XOR_MAPPED_ADDRESS;
import static org.ice4j.attribute.Attribute.XOR_RELAYED_ADDRESS;
import static org.ice4j.attribute.RequestedTransportAttribute.UDP;

public class TurnClientImpl implements TurnClient {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final AtomicInteger CHANNEL_NUMBER = new AtomicInteger(0x4000);
  private final ChannelData channelData;

  @Resource
  ScheduledExecutorService scheduledExecutorService;
  @Resource
  LobbyServerAccessor lobbyServerAccessor;

  @Value("${turn.host}")
  String turnHost;
  @Value("${turn.port}")
  int turnPort;

  @VisibleForTesting
  StunStack stunStack;
  private BlockingRequestSender blockingRequestSender;
  private TransportAddress relayedAddress;
  private TransportAddress mappedAddress;
  private ScheduledFuture<?> refreshTask;
  private TransportAddress serverAddress;
  private TransportAddress localAddress;
  private Map<SocketAddress, Character> socketsToChannels;
  private DatagramSocket localSocket;

  public TurnClientImpl() {
    stunStack = new StunStack();
    channelData = new ChannelData();
    socketsToChannels = new HashMap<>();
  }

  @PostConstruct
  void postConstruct() {
    serverAddress = new TransportAddress(turnHost, turnPort, Transport.UDP);
    lobbyServerAccessor.addOnMessageListener(CreatePermissionMessage.class, message -> addPeer(message.getAddress()));
  }

  private void addPeer(InetSocketAddress address) {
    Request createPermissionRequest = MessageFactory.createCreatePermissionRequest(
        new TransportAddress(address, Transport.UDP),
        TransactionID.createNewTransactionID().getBytes()
    );
    sendRequest(createPermissionRequest);
    bindChannel(address);
  }

  private Response sendRequest(Request request) {
    try {
      StunMessageEvent stunMessageEvent = blockingRequestSender.sendRequestAndWaitForResponse(request, serverAddress);
      Response response = ((StunResponseEvent) stunMessageEvent).getResponse();
      if (response.isErrorResponse()) {
        throw new StunException(((ErrorCodeAttribute) request.getAttribute(Attribute.ERROR_CODE)).getReasonPhrase());
      }
      return response;
    } catch (StunException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void bindChannel(InetSocketAddress address) {
    char channelNumber = (char) CHANNEL_NUMBER.getAndIncrement();
    Request request = MessageFactory.createChannelBindRequest(
        channelNumber,
        new TransportAddress(address, Transport.UDP),
        TransactionID.createNewTransactionID().getBytes()
    );
    sendRequest(request);
    socketsToChannels.put(address, channelNumber);
  }

  @Override
  public CompletableFuture<SocketAddress> connect() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        localSocket = new DatagramSocket(0, InetAddress.getLocalHost());
        localAddress = new TransportAddress((InetSocketAddress) localSocket.getLocalSocketAddress(), Transport.UDP);
        stunStack.addSocket(new IceUdpSocketWrapper(localSocket), serverAddress);
        blockingRequestSender = new BlockingRequestSender(stunStack, localAddress);

        allocateAddress(serverAddress);
        return new InetSocketAddress(relayedAddress.getHostAddress(), relayedAddress.getPort());
      } catch (StunException | IOException e) {
        throw new RuntimeException(e);
      }
    }, scheduledExecutorService);
  }

  @Override
  @PreDestroy
  public void close() throws IOException {
    IOUtils.closeQuietly(localSocket);
    if (localAddress != null) {
      stunStack.removeSocket(localAddress);
    }
    if (refreshTask != null) {
      refreshTask.cancel(true);
    }
  }

  @Override
  public InetSocketAddress getRelayAddress() {
    return relayedAddress;
  }

  @Override
  public void send(DatagramPacket datagramPacket) {
    channelData.setData(datagramPacket.getData());
    channelData.setChannelNumber(socketsToChannels.get(datagramPacket.getSocketAddress()));

    try {
      stunStack.sendChannelData(channelData, serverAddress, localAddress);
    } catch (StunException e) {
      throw new RuntimeException(e);
    }
  }

  private void allocateAddress(TransportAddress turnServerAddress) throws StunException, IOException {
    logger.info("Requesting address allocation at {}", serverAddress);
    Response response = sendRequest(MessageFactory.createAllocateRequest(UDP, false));

    byte[] transactionID = response.getTransactionID();
    relayedAddress = ((XorRelayedAddressAttribute) response.getAttribute(XOR_RELAYED_ADDRESS)).getAddress(transactionID);
    mappedAddress = ((XorMappedAddressAttribute) response.getAttribute(XOR_MAPPED_ADDRESS)).getAddress(transactionID);

    logger.info("Relayed address: {}, mapped address: {}", relayedAddress, mappedAddress);

    int lifetime = ((LifetimeAttribute) response.getAttribute(Attribute.LIFETIME)).getLifetime();
    refreshTask = scheduleRefresh(turnServerAddress, lifetime / 3);
  }

  @NotNull
  private ScheduledFuture<?> scheduleRefresh(TransportAddress turnServerAddress, int interval) {
    return scheduledExecutorService.scheduleWithFixedDelay((Runnable) () -> {
      logger.trace("Refreshing TURN allocation");
      Request refreshRequest = MessageFactory.createRefreshRequest(interval);
      try {
        blockingRequestSender.sendRequestAndWaitForResponse(refreshRequest, turnServerAddress);
      } catch (StunException | IOException e) {
        logger.warn("Could not refresh TURN allocation", e);
        throw new RuntimeException(e);
      }
    }, interval, interval, TimeUnit.SECONDS);
  }
}
