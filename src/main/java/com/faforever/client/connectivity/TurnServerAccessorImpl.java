package com.faforever.client.connectivity;

import com.faforever.client.net.ConnectionState;
import com.faforever.client.relay.ConnectToPeerMessage;
import com.faforever.client.relay.CreatePermissionMessage;
import com.faforever.client.relay.JoinGameMessage;
import com.faforever.client.remote.FafService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import org.ice4j.message.ChannelData;
import org.ice4j.message.Message;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;
import org.ice4j.message.Response;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.socket.MultiplexedDatagramSocket;
import org.ice4j.socket.MultiplexingDatagramSocket;
import org.ice4j.socket.TurnDatagramPacketFilter;
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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.net.InetAddress.getLocalHost;
import static org.ice4j.attribute.Attribute.ERROR_CODE;
import static org.ice4j.attribute.Attribute.XOR_MAPPED_ADDRESS;
import static org.ice4j.attribute.Attribute.XOR_PEER_ADDRESS;
import static org.ice4j.attribute.Attribute.XOR_RELAYED_ADDRESS;
import static org.ice4j.attribute.RequestedTransportAttribute.UDP;

public class TurnServerAccessorImpl implements TurnServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final AtomicInteger CHANNEL_NUMBER = new AtomicInteger(0x4000);

  /**
   * The length in bytes of the Channel Number field of a TURN ChannelData message.
   */
  private static final int CHANNELDATA_CHANNELNUMBER_LENGTH = 2;

  /**
   * The length in bytes of the Length field of a TURN ChannelData message.
   */
  private static final int CHANNELDATA_LENGTH_LENGTH = 2;

  @VisibleForTesting
  final StunStack stunStack;
  private final Map<TransportAddress, Character> peerAddressToChannel;
  private final Map<Character, TransportAddress> channelToPeerAddress;
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
  private MultiplexingDatagramSocket localSocket;
  private MultiplexedDatagramSocket channelDataSocket;
  private Consumer<DatagramPacket> onDataListener;
  private ObjectProperty<ConnectionState> connectionState;

  public TurnServerAccessorImpl() {
    stunStack = new StunStack();
    peerAddressToChannel = new HashMap<>();
    channelToPeerAddress = new HashMap<>();
    connectionState = new SimpleObjectProperty<>();
  }

  @PostConstruct
  void postConstruct() {
    serverAddress = new TransportAddress(turnHost, turnPort, Transport.UDP);
    fafService.addOnMessageListener(CreatePermissionMessage.class, message -> addPeer(message.getAddress()));
    fafService.addOnMessageListener(JoinGameMessage.class, message -> addPeer(message.getPeerAddress()));
    fafService.addOnMessageListener(ConnectToPeerMessage.class, message -> addPeer(message.getPeerAddress()));
  }

  /**
   * Permits a peer to send data and binds it to a channel.
   *
   * @param address the peer's publicly visible address
   */
  private void addPeer(InetSocketAddress address) {
    permit(address);
    bind(new TransportAddress(address, Transport.UDP), (char) CHANNEL_NUMBER.getAndIncrement());
  }

  private void permit(InetSocketAddress address) {
    logger.info("Permitting sends from {}", address);
    Request createPermissionRequest = MessageFactory.createCreatePermissionRequest(
        new TransportAddress(address, Transport.UDP),
        TransactionID.createNewTransactionID().getBytes()
    );
    sendRequest(createPermissionRequest);
    logger.debug("Permitted sends from {}", address);
  }

  private void bind(TransportAddress address, int channelNumber) {
    if (peerAddressToChannel.containsKey(address)) {
      return;
    }

    logger.info("Binding '{}' to channel '{}'", address, channelNumber);
    Response response = sendRequest(MessageFactory.createChannelBindRequest(
        (char) channelNumber, address, TransactionID.createNewTransactionID().getBytes()
    ));
    if (response.isSuccessResponse()) {
      logger.debug("Bound '{}' to channel '{}'", address, channelNumber);

      peerAddressToChannel.put(address, (char) channelNumber);
      channelToPeerAddress.put((char) channelNumber, address);
    } else {
      logger.warn("Binding for '{}' to channel '{}' failed", address, channelNumber);
    }
  }

  private Response sendRequest(Request request) {
    try {
      StunMessageEvent stunMessageEvent = blockingRequestSender.sendRequestAndWaitForResponse(request, serverAddress);
      Response response = ((StunResponseEvent) stunMessageEvent).getResponse();
      if (response.isErrorResponse()) {
        logger.warn("STUN error: {}", ((ErrorCodeAttribute) response.getAttribute(ERROR_CODE)).getReasonPhrase());
      }
      return response;
    } catch (StunException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void ensureConnected() {
    if (connectionState.get() == ConnectionState.CONNECTED) {
      return;
    }
    connectionState.set(ConnectionState.CONNECTING);
    try {
      localSocket = new MultiplexingDatagramSocket(0, getLocalHost());
      channelDataSocket = localSocket.getSocket(
          new TurnDatagramPacketFilter(serverAddress) {
            @Override
            public boolean accept(DatagramPacket packet) {
              return channelDataSocketAccept(packet);
            }

            @Override
            protected boolean acceptMethod(char method) {
              return false;
            }
          });

      localAddress = new TransportAddress((InetSocketAddress) localSocket.getLocalSocketAddress(), Transport.UDP);
      stunStack.addSocket(new IceUdpSocketWrapper(localSocket), serverAddress);
      blockingRequestSender = new BlockingRequestSender(stunStack, localAddress);

      stunStack.addIndicationListener(localAddress, this::onIndication);

      releaseAllocation();
      allocateAddress(serverAddress);
      permit(connectivityService.getExternalSocketAddress());

      scheduledExecutorService.execute(this::runInReceiveChannelDataThread);
      connectionState.set(ConnectionState.CONNECTED);
    } catch (StunException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @PreDestroy
  public void disconnect() {
    connectionState.set(ConnectionState.DISCONNECTED);
    releaseAllocation();
    peerAddressToChannel.clear();
    if (localAddress != null) {
      stunStack.removeSocket(localAddress);
    }
    releaseAllocation();
    IOUtils.closeQuietly(localSocket);
    IOUtils.closeQuietly(channelDataSocket);
  }

  @Override
  public InetSocketAddress getRelayAddress() {
    return relayedAddress;
  }

  @Override
  public void send(DatagramPacket packet) {
    SocketAddress socketAddress = packet.getSocketAddress();
    if (!peerAddressToChannel.containsKey(socketAddress)) {
      logger.warn("Peer {} is not bound to a channel", socketAddress);
      return;
    }

    Character channelNumber = peerAddressToChannel.get(socketAddress);

    byte[] payload = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), packet.getOffset(), payload, packet.getOffset(), payload.length);

    ChannelData channelData = new ChannelData();
    channelData.setData(payload);
    channelData.setChannelNumber(channelNumber);

    if (logger.isTraceEnabled()) {
      logger.trace("Writing {} bytes on channel {}: {}", packet.getLength(), (int) channelNumber,
          new String(payload, 0, payload.length, StandardCharsets.US_ASCII));
    }
    try {
      stunStack.sendChannelData(channelData, serverAddress, localAddress);
    } catch (StunException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setOnDataListener(Consumer<DatagramPacket> listener) {
    this.onDataListener = listener;
  }

  /**
   * Determines whether a specific <tt>DatagramPacket</tt> is accepted by {@link #channelDataSocket} (i.e. whether
   * <tt>channelDataSocket</tt> understands <tt>p</tt> and <tt>p</tt> is meant to be received by
   * <tt>channelDataSocket</tt>).
   *
   * @param packet the <tt>DatagramPacket</tt> which is to be checked whether it is accepted by
   * <tt>channelDataSocket</tt>
   *
   * @return <tt>true</tt> if <tt>channelDataSocket</tt> accepts <tt>p</tt> (i.e. <tt>channelDataSocket</tt> understands
   * <tt>p</tt> and <tt>p</tt> is meant to be received by <tt>channelDataSocket</tt>); otherwise, <tt>false</tt>
   */
  private boolean channelDataSocketAccept(DatagramPacket packet) {
    // Is it from our TURN server?
    if (!serverAddress.equals(packet.getSocketAddress())) {
      return false;
    }

    int packetLength = packet.getLength();

    if (packetLength
        < (CHANNELDATA_CHANNELNUMBER_LENGTH
        + CHANNELDATA_LENGTH_LENGTH)) {
      return false;
    }

    byte[] pData = packet.getData();
    int pOffset = packet.getOffset();

    /*
     * The first two bits should be 0b01 because of the current
     * channel number range 0x4000 - 0x7FFE. But 0b10 and 0b11 which
     * are currently reserved and may be used in the future to
     * extend the range of channel numbers.
     */
    if ((pData[pOffset] & 0xC0) == 0) {
      return false;
    }

    pOffset += CHANNELDATA_CHANNELNUMBER_LENGTH;
    packetLength -= CHANNELDATA_CHANNELNUMBER_LENGTH;

    int length = ((pData[pOffset++] << 8) | (pData[pOffset] & 0xFF));

    int padding = ((length % 4) > 0) ? 4 - (length % 4) : 0;

    /*
     * The Length field specifies the length in bytes of the
     * Application Data field. The Length field does not include
     * the padding that is sometimes present in the data of the
     * DatagramPacket.
     */
    return length == packetLength - padding - CHANNELDATA_LENGTH_LENGTH
        || length == packetLength - CHANNELDATA_LENGTH_LENGTH;
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

      for (Map.Entry<TransportAddress, Character> entry : peerAddressToChannel.entrySet()) {
        bind(entry.getKey(), entry.getValue());
      }
    }, interval, interval, TimeUnit.SECONDS);
  }

  private void onIndication(StunMessageEvent event) {
    Message message = event.getMessage();
    byte[] data = ((DataAttribute) message.getAttribute(Attribute.DATA)).getData();
    TransportAddress sender = ((XorPeerAddressAttribute) message.getAttribute(XOR_PEER_ADDRESS)).getAddress(message.getTransactionID());

    if (logger.isTraceEnabled()) {
      logger.trace("Received {} bytes indication from '{}': {}", data.length, sender,
          new String(data, 0, data.length, StandardCharsets.US_ASCII));
    }

    DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
    datagramPacket.setSocketAddress(sender);
    onDataListener.accept(datagramPacket);
  }

  private void runInReceiveChannelDataThread() {
    int receiveBufferSize = 1500;
    DatagramPacket packet = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);

    while (connectionState.get() != ConnectionState.DISCONNECTED) {
      try {
        channelDataSocket.receive(packet);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      int channelDataLength = packet.getLength();
      if (channelDataLength < (CHANNELDATA_CHANNELNUMBER_LENGTH + CHANNELDATA_LENGTH_LENGTH)) {
        continue;
      }

      byte[] receivedData = packet.getData();
      int channelDataOffset = packet.getOffset();
      char channelNumber = (char)
          ((receivedData[channelDataOffset++] << 8)
              | (receivedData[channelDataOffset++] & 0xFF));

      channelDataLength -= CHANNELDATA_CHANNELNUMBER_LENGTH;

      char length = (char)
          ((receivedData[channelDataOffset++] << 8)
              | (receivedData[channelDataOffset++] & 0xFF));

      channelDataLength -= CHANNELDATA_LENGTH_LENGTH;
      if (length > channelDataLength) {
        continue;
      }

      byte[] payload = new byte[length];
      System.arraycopy(receivedData, channelDataOffset, payload, 0, length);

      ChannelData channelData = new ChannelData();
      channelData.setChannelNumber(channelNumber);
      channelData.setData(payload);
      onChannelData(new ChannelDataMessageEvent(stunStack,
          channelToPeerAddress.get(channelNumber),
          localAddress,
          channelData
      ));
    }
  }

  private void onChannelData(ChannelDataMessageEvent event) {
    ChannelData channelData = event.getChannelDataMessage();

    if (logger.isTraceEnabled()) {
      logger.trace("Received {} bytes on channel {}: {}", (int) channelData.getDataLength(), (int) channelData.getChannelNumber(),
          new String(channelData.getData(), 0, channelData.getDataLength(), StandardCharsets.US_ASCII));
    }

    DatagramPacket datagramPacket = new DatagramPacket(channelData.getData(), channelData.getDataLength());
    datagramPacket.setSocketAddress(event.getRemoteAddress());
    onDataListener.accept(datagramPacket);
  }
}
