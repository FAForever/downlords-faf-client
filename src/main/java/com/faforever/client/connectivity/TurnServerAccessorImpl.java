package com.faforever.client.connectivity;

import com.faforever.client.relay.ConnectToPeerMessage;
import com.faforever.client.relay.CreatePermissionMessage;
import com.faforever.client.relay.JoinGameMessage;
import com.faforever.client.remote.FafService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.compress.utils.IOUtils;
import org.ice4j.ChannelDataMessageEvent;
import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.StunResponseEvent;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.Attribute;
import org.ice4j.attribute.DataAttribute;
import org.ice4j.attribute.ErrorCodeAttribute;
import org.ice4j.attribute.XorMappedAddressAttribute;
import org.ice4j.attribute.XorPeerAddressAttribute;
import org.ice4j.attribute.XorRelayedAddressAttribute;
import org.ice4j.ice.Agent;
import org.ice4j.ice.harvest.CandidateHarvester;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.message.ChannelData;
import org.ice4j.message.Message;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.ice4j.attribute.Attribute.XOR_MAPPED_ADDRESS;
import static org.ice4j.attribute.Attribute.XOR_PEER_ADDRESS;
import static org.ice4j.attribute.Attribute.XOR_RELAYED_ADDRESS;
import static org.ice4j.attribute.RequestedTransportAttribute.UDP;

public class TurnServerAccessorImpl implements TurnServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final AtomicInteger CHANNEL_NUMBER = new AtomicInteger(0x4000);
  @VisibleForTesting
  final StunStack turnStack;
  private final Map<InetSocketAddress, Character> socketsToChannels;
  private final DatagramPacket datagramPacket;
  private final ChannelData channelData;
  @Resource
  ScheduledExecutorService scheduledExecutorService;
  @Resource
  FafService fafService;
  @Resource
  ConnectivityService connectivityService;
  @Value("${turn.host}")
  String turnHost;
  @Value("${turn.port}")
  int turnPort;
  @Value("${turn.refreshInterval}")
  int refreshInterval;
  private BlockingRequestSender blockingRequestSender;
  private TransportAddress relayedAddress;
  private TransportAddress mappedAddress;
  private ScheduledFuture<?> refreshTask;
  private TransportAddress serverAddress;
  private TransportAddress localAddress;
  private DatagramSocket localSocket;
  private Consumer<DatagramPacket> onDataListener;

  public TurnServerAccessorImpl() {
    turnStack = new StunStack(null, this::onChannelData);
    channelData = new ChannelData();
    socketsToChannels = new HashMap<>();
    datagramPacket = new DatagramPacket(new byte[1024], 1024);
  }

  @PostConstruct
  void postConstruct() {
    serverAddress = new TransportAddress(turnHost, turnPort, Transport.UDP);
    fafService.addOnMessageListener(CreatePermissionMessage.class, message -> addPeer(message.getAddress()));
    fafService.addOnMessageListener(JoinGameMessage.class, message -> addPeer(message.getPeerAddress()));
    fafService.addOnMessageListener(ConnectToPeerMessage.class, message -> addPeer(message.getPeerAddress()));
  }

  private void addPeer(InetSocketAddress address) {
    permit(address);

    if (!socketsToChannels.containsKey(address)) {
      bind(address, CHANNEL_NUMBER.getAndIncrement());
    }
  }

  private void permit(InetSocketAddress address) {
    logger.debug("Permitting sends from {}", address);
    Request createPermissionRequest = MessageFactory.createCreatePermissionRequest(
        new TransportAddress(address, Transport.UDP),
        TransactionID.createNewTransactionID().getBytes()
    );
    sendRequest(createPermissionRequest);
    logger.info("Permitted sends from {}", address);
  }

  private void bind(InetSocketAddress address, int channelNumber) {
    logger.debug("Binding '{}' to channel '{}'", address, channelNumber);

    Request request = MessageFactory.createChannelBindRequest(
        (char) channelNumber,
        new TransportAddress(address, Transport.UDP),
        TransactionID.createNewTransactionID().getBytes()
    );
    sendRequest(request);
    logger.info("Bound '{}' to channel '{}'", address, channelNumber);
    socketsToChannels.put(address, (char) channelNumber);
  }

  private Response sendRequest(Request request) {
    try {
      StunMessageEvent stunMessageEvent = blockingRequestSender.sendRequestAndWaitForResponse(request, serverAddress);
      Response response = ((StunResponseEvent) stunMessageEvent).getResponse();
      if (response.isErrorResponse()) {
        throw new StunException(((ErrorCodeAttribute) response.getAttribute(Attribute.ERROR_CODE)).getReasonPhrase());
      }
      return response;
    } catch (StunException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void connect() {
    Agent agent = new Agent();
    CandidateHarvester stunHarvester = new StunCandidateHarvester(serverAddress);
    CandidateHarvester turnHarvester = new TurnCandidateHarvester(serverAddress);

    agent.addCandidateHarvester(stunHarvester);
    agent.addCandidateHarvester(turnHarvester);

    // FIXME continue

    try {
      localSocket = new DatagramSocket(0, InetAddress.getLocalHost());
      localAddress = new TransportAddress((InetSocketAddress) localSocket.getLocalSocketAddress(), Transport.UDP);
      turnStack.addSocket(new IceUdpSocketWrapper(localSocket), serverAddress);
      blockingRequestSender = new BlockingRequestSender(turnStack, localAddress);

      turnStack.addIndicationListener(localAddress, this::onIndication);

      releaseAllocation();
      allocateAddress(serverAddress);
      // TODO is it required to permit ourselves?
      permit(connectivityService.getExternalSocketAddress());
      permit(new InetSocketAddress("37.58.123.2", 6112));
      permit(new InetSocketAddress("37.58.123.3", 6112));
    } catch (StunException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void releaseAllocation() {
    if (refreshTask != null && !refreshTask.isCancelled()) {
      logger.debug("Releasing previous allocation");
      sendRequest(MessageFactory.createRefreshRequest(0));
      refreshTask.cancel(true);
    }
  }

  private void allocateAddress(TransportAddress turnServerAddress) throws StunException, IOException {
    logger.info("Requesting address allocation at {}", serverAddress);
    Response response = sendRequest(MessageFactory.createAllocateRequest(UDP, false));

    byte[] transactionID = response.getTransactionID();
    relayedAddress = ((XorRelayedAddressAttribute) response.getAttribute(XOR_RELAYED_ADDRESS)).getAddress(transactionID);
    mappedAddress = ((XorMappedAddressAttribute) response.getAttribute(XOR_MAPPED_ADDRESS)).getAddress(transactionID);

    logger.info("Relayed address: {}, mapped address: {}", relayedAddress, mappedAddress);

    refreshTask = scheduleRefresh(refreshInterval);
  }

  @NotNull
  private ScheduledFuture<?> scheduleRefresh(int interval) {
    return scheduledExecutorService.scheduleWithFixedDelay((Runnable) () -> {
      logger.debug("Refreshing TURN allocation");
      sendRequest(MessageFactory.createRefreshRequest(interval));

      for (Map.Entry<InetSocketAddress, Character> entry : socketsToChannels.entrySet()) {
        bind(entry.getKey(), entry.getValue());
      }
    }, interval, interval, TimeUnit.SECONDS);
  }

  @Override
  @PreDestroy
  public void close() {
    if (localAddress != null) {
      turnStack.removeSocket(localAddress);
    }
    releaseAllocation();
    IOUtils.closeQuietly(localSocket);
  }

  @Override
  public InetSocketAddress getRelayAddress() {
    return relayedAddress;
  }

  @Override
  public void send(DatagramPacket datagramPacket) {
    SocketAddress socketAddress = datagramPacket.getSocketAddress();
    if (!socketsToChannels.containsKey(socketAddress)) {
      logger.warn("Peer {} is not bound to a channel", socketAddress);
      return;
    }

    Character channelNumber = socketsToChannels.get(socketAddress);

    this.channelData.setData(datagramPacket.getData());
    this.channelData.setChannelNumber(channelNumber);

    logger.trace("Forwarding {} bytes to channel {}", datagramPacket.getLength(), (int) channelNumber);
    try {
      turnStack.sendChannelData(this.channelData, serverAddress, localAddress);
    } catch (StunException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InetSocketAddress getMappedAddress() {
    return mappedAddress;
  }

  @Override
  public void setOnDataListener(Consumer<DatagramPacket> listener) {
    this.onDataListener = listener;
  }

  @Override
  public void unbind() {
    socketsToChannels.clear();
  }

  private void onIndication(StunMessageEvent event) {
    Message message = event.getMessage();
    byte[] data = ((DataAttribute) message.getAttribute(Attribute.DATA)).getData();
    TransportAddress sender = ((XorPeerAddressAttribute) message.getAttribute(XOR_PEER_ADDRESS)).getAddress(message.getTransactionID());

    logger.trace("Received {} bytes indication from '{}'", data.length, sender);

    datagramPacket.setData(data);
    datagramPacket.setSocketAddress(sender);
    onDataListener.accept(datagramPacket);
  }

  private void onChannelData(ChannelDataMessageEvent event) {
    ChannelData channelData = event.getChannelDataMessage();
    logger.trace("Received {} bytes on channel {}", channelData.getDataLength(), (int) channelData.getChannelNumber());

    datagramPacket.setData(channelData.getData());
    datagramPacket.setSocketAddress(event.getRemoteAddress());
    onDataListener.accept(datagramPacket);
  }
}
