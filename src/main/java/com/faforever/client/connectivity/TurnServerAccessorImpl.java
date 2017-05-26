package com.faforever.client.connectivity;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Ice.Turn;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.relay.CreatePermissionMessage;
import com.faforever.client.remote.FafService;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.commons.compress.utils.IOUtils;
import org.ice4j.ChannelDataMessageEvent;
import org.ice4j.ResponseCollector;
import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.StunResponseEvent;
import org.ice4j.StunTimeoutEvent;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static com.github.nocatch.NoCatch.noCatch;
import static org.ice4j.attribute.Attribute.ERROR_CODE;
import static org.ice4j.attribute.Attribute.XOR_MAPPED_ADDRESS;
import static org.ice4j.attribute.Attribute.XOR_PEER_ADDRESS;
import static org.ice4j.attribute.Attribute.XOR_RELAYED_ADDRESS;
import static org.ice4j.attribute.RequestedTransportAttribute.UDP;

@Service
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

  private final Map<TransportAddress, Character> peerAddressToChannel;
  private final Map<Character, TransportAddress> channelToPeerAddress;
  @Resource
  TaskScheduler taskScheduler;
  @Resource
  FafService fafService;
  @Resource
  ConnectivityService connectivityService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  ClientProperties clientProperties;

  private TransportAddress relayedAddress;
  private TransportAddress mappedAddress;
  private ScheduledFuture<?> refreshTask;
  private TransportAddress serverAddress;
  private TransportAddress localAddress;
  private MultiplexingDatagramSocket localSocket;
  private MultiplexedDatagramSocket channelDataSocket;
  private ObjectProperty<ConnectionState> connectionState;
  private Collection<Consumer<DatagramPacket>> onPacketListeners;
  private StunStack stunStack;

  public TurnServerAccessorImpl() {
    peerAddressToChannel = new HashMap<>();
    channelToPeerAddress = new HashMap<>();
    connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);
    onPacketListeners = new LinkedHashSet<>();
  }

  @VisibleForTesting
  public InetSocketAddress getLocalSocketAddress() {
    return (InetSocketAddress) localSocket.getLocalSocketAddress();
  }

  /**
   * Permits a peer to send data through the TURN.
   *
   * @param address the peer's publicly visible address
   */
  private void permit(InetSocketAddress address) {
    logger.info("Permitting sends from {}", address);
    Request createPermissionRequest = MessageFactory.createCreatePermissionRequest(
        new TransportAddress(address, Transport.UDP),
        TransactionID.createNewTransactionID().getBytes()
    );
    sendBlockingStunRequest(createPermissionRequest);
    sendStunRequest(createPermissionRequest, new ResponseCollector() {
      @Override
      public void processResponse(StunResponseEvent event) {
        logger.debug("Permitted sends from {}", address);
      }

      @Override
      public void processTimeout(StunTimeoutEvent event) {
        logger.warn("Permission request for '{}' timed out.", address);
      }
    });
  }

  @Override
  @PreDestroy
  public void disconnect() {
    connectionState.set(ConnectionState.DISCONNECTED);
    releaseAllocation();

    peerAddressToChannel.clear();
    channelToPeerAddress.clear();

    if (localAddress != null && stunStack != null) {
      stunStack.removeSocket(localAddress);
      stunStack.shutDown();
    }
    if (refreshTask != null) {
      refreshTask.cancel(true);
    }
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
    //noinspection SuspiciousMethodCalls
    if (!peerAddressToChannel.containsKey(socketAddress)) {
      logger.warn("Peer {} is not bound to a channel", socketAddress);
      return;
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    Character channelNumber = peerAddressToChannel.get(socketAddress);

    byte[] payload = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), packet.getOffset(), payload, packet.getOffset(), payload.length);

    ChannelData channelData = new ChannelData();
    channelData.setData(payload);
    channelData.setChannelNumber(channelNumber);

    if (logger.isTraceEnabled()) {
      logger.trace("Writing {} bytes to channel {}: {}", packet.getLength(), (int) channelNumber,
          new String(payload, 0, payload.length, StandardCharsets.US_ASCII));
    }
    try {
      stunStack.sendChannelData(channelData, serverAddress, localAddress);
    } catch (StunException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ConnectionState getConnectionState() {
    return connectionState.get();
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public void connect() {
    if (connectionState.get() == ConnectionState.CONNECTED) {
      return;
    }
    stunStack = applicationContext.getBean(StunStack.class);

    fafService.addOnMessageListener(CreatePermissionMessage.class, message -> permit(message.getAddress()));

    Turn turn = clientProperties.getIce().getTurn();
    serverAddress = new TransportAddress(turn.getHost(), turn.getPort(), Transport.UDP);
    connectionState.set(ConnectionState.CONNECTING);
    try {
      localSocket = new MultiplexingDatagramSocket(0);
      channelDataSocket = localSocket.getSocket(
          new TurnDatagramPacketFilter(serverAddress) {
            @Override
            public boolean accept(DatagramPacket packet) {
              return isChannelData(packet);
            }

            @Override
            protected boolean acceptMethod(char method) {
              return false;
            }
          });

      localAddress = new TransportAddress((InetSocketAddress) localSocket.getLocalSocketAddress(), Transport.UDP);
      stunStack.addSocket(new IceUdpSocketWrapper(localSocket), serverAddress);

      stunStack.addIndicationListener(localAddress, this::onIndication);

      releaseAllocation();
      allocateAddress(serverAddress);
      permit(connectivityService.getExternalSocketAddress());

      connectionState.set(ConnectionState.CONNECTED);
      taskScheduler.schedule(this::runInReceiveChannelDataThread, new Date());
    } catch (StunException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @SuppressWarnings("SuspiciousMethodCalls")
  public boolean isBound(InetSocketAddress socketAddress) {
    return peerAddressToChannel.containsKey(socketAddress);
  }

  @Override
  public void bind(InetSocketAddress socketAddress) {
    bind(new TransportAddress(socketAddress, Transport.UDP), CHANNEL_NUMBER.getAndIncrement());
  }

  private void bind(TransportAddress address, int channelNumber) {
    synchronized (peerAddressToChannel) {
      if (peerAddressToChannel.containsKey(address)) {
        return;
      }

      logger.info("Binding '{}' to channel '{}'", address, channelNumber);
      sendStunRequest(MessageFactory.createChannelBindRequest(
          (char) channelNumber, address, TransactionID.createNewTransactionID().getBytes()
      ), new ResponseCollector() {
        @Override
        public void processResponse(StunResponseEvent event) {
          if (event.getResponse().isSuccessResponse()) {
            logger.debug("Bound '{}' to channel '{}'", address, channelNumber);

            peerAddressToChannel.put(address, (char) channelNumber);
            channelToPeerAddress.put((char) channelNumber, address);
          } else {
            logger.warn("Binding for '{}' to channel '{}' failed", address, channelNumber);
          }
        }

        @Override
        public void processTimeout(StunTimeoutEvent event) {
          logger.warn("Binding request for '{}' to channel '{}' timed out.", address, channelNumber);
        }
      });
    }
  }

  private void releaseAllocation() {
    if (refreshTask != null && !refreshTask.isCancelled()) {
      logger.debug("Releasing previous allocation");
      sendBlockingStunRequest(MessageFactory.createRefreshRequest(0));
      refreshTask.cancel(true);
    }
  }

  @NotNull
  private Response sendBlockingStunRequest(Request request) {
    return noCatch(() -> {
      CompletableFuture<Response> responseFuture = new CompletableFuture<>();
      sendStunRequest(request, responseCollector(responseFuture));
      Response response = responseFuture.get();

      if (response.isErrorResponse()) {
        logger.warn("STUN error: {}", ((ErrorCodeAttribute) response.getAttribute(ERROR_CODE)).getReasonPhrase());
      }
      return response;
    });
  }

  private void sendStunRequest(Request request, ResponseCollector responseCollector) {
    noCatch(() -> stunStack.sendRequest(request, serverAddress, localAddress, responseCollector));
  }

  @NotNull
  private ResponseCollector responseCollector(final CompletableFuture<Response> responseFuture) {
    return new ResponseCollector() {
      @Override
      public void processResponse(StunResponseEvent event) {
        responseFuture.complete(event.getResponse());
      }

      @Override
      public void processTimeout(StunTimeoutEvent event) {
        logger.warn("STUN request timed out: {}", event.getMessage());
        responseFuture.completeExceptionally(new RuntimeException("STUN request " + event.getTransactionID() + " timed out"));
      }
    };
  }

  @Override
  public void addOnPacketListener(Consumer<DatagramPacket> listener) {
    onPacketListeners.add(listener);
  }

  @Override
  public void removeOnPacketListener(Consumer<DatagramPacket> listener) {
    onPacketListeners.remove(listener);
  }

  /**
   * Determines whether a specific {@code DatagramPacket} is accepted by {@link #channelDataSocket} (i.e. whether {@code
   * channelDataSocket} understands {@code packet} and {@code packet} is meant to be received by {@code
   * channelDataSocket}).
   *
   * @param packet the {@code DatagramPacket} which is to be checked whether it is accepted by {@code
   * channelDataSocket}
   * @return {@code true} if {@code channelDataSocket} accepts {@code packet} (i.e. {@code channelDataSocket}
   * understands {@code packet} and {@code p} is meant to be received by {@code channelDataSocket}); otherwise, {@code
   * false}
   */
  private boolean isChannelData(DatagramPacket packet) {
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
     * The first two bits should be 0b01 because of the current channel number range 0x4000 - 0x7FFE. But 0b10 and 0b11
     * which are currently reserved and may be used in the future to extend the range of channel numbers.
     */
    if ((pData[pOffset] & 0xC0) == 0) {
      return false;
    }

    pOffset += CHANNELDATA_CHANNELNUMBER_LENGTH;
    packetLength -= CHANNELDATA_CHANNELNUMBER_LENGTH;

    int length = ((pData[pOffset++] << 8) | (pData[pOffset] & 0xFF));

    int padding = ((length % 4) > 0) ? 4 - (length % 4) : 0;

    /*
     * The Length field specifies the length in bytes of the Application Data field. The Length field does not include
     * the padding that is sometimes present in the data of the DatagramPacket.
     */
    return length == packetLength - padding - CHANNELDATA_LENGTH_LENGTH
        || length == packetLength - CHANNELDATA_LENGTH_LENGTH;
  }

  private void allocateAddress(TransportAddress turnServerAddress) throws StunException, IOException {
    logger.info("Requesting address allocation at {}", serverAddress);
    Response response = sendBlockingStunRequest(MessageFactory.createAllocateRequest(UDP, false));

    byte[] transactionID = response.getTransactionID();
    relayedAddress = ((XorRelayedAddressAttribute) response.getAttribute(XOR_RELAYED_ADDRESS)).getAddress(transactionID);
    fafService.setRelayAddress(relayedAddress);
    mappedAddress = ((XorMappedAddressAttribute) response.getAttribute(XOR_MAPPED_ADDRESS)).getAddress(transactionID);

    logger.info("Relayed address: {}, mapped address: {}", relayedAddress, mappedAddress);

    refreshTask = scheduleRefresh(20_000);
  }

  private ScheduledFuture<?> scheduleRefresh(int interval) {
    return taskScheduler.scheduleWithFixedDelay(() -> {
      logger.debug("Refreshing TURN allocation");
      sendBlockingStunRequest(MessageFactory.createRefreshRequest(interval));

      for (Map.Entry<TransportAddress, Character> entry : peerAddressToChannel.entrySet()) {
        bind(entry.getKey(), entry.getValue());
      }
    }, new Date(), interval);
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
    onPacketReceived(datagramPacket);
  }

  private void onPacketReceived(DatagramPacket datagramPacket) {
    onPacketListeners.forEach(consumer -> consumer.accept(datagramPacket));
  }

  private void runInReceiveChannelDataThread() {
    int receiveBufferSize = 1500;
    DatagramPacket packet = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);

    while (connectionState.get() == ConnectionState.CONNECTED) {
      try {
        channelDataSocket.receive(packet);
      } catch (IOException e) {
        if (channelDataSocket.isClosed()) {
          logger.debug("Channel data socket has been closed");
          return;
        } else {
          throw new RuntimeException(e);
        }
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

      if (!channelToPeerAddress.containsKey(channelNumber)) {
        logger.warn("Received {} bytes on unbound channel '{}' from {}", channelDataLength, (int) channelNumber, packet.getSocketAddress());
        continue;
      }

      byte[] payload = new byte[length];
      System.arraycopy(receivedData, channelDataOffset, payload, 0, length);

      if (logger.isTraceEnabled()) {
        logger.trace("Received {} bytes on channel {}: {}", channelDataLength, (int) channelNumber,
            new String(payload, 0, length, StandardCharsets.US_ASCII));
      }

      ChannelData channelData = new ChannelData();
      channelData.setChannelNumber(channelNumber);
      channelData.setData(payload);

      try {
        onChannelData(new ChannelDataMessageEvent(stunStack,
            channelToPeerAddress.get(channelNumber),
            localAddress,
            channelData
        ));
      } catch (Exception e) {
        logger.warn("Error while handling channel data", e);
      }
    }

    logger.info("Stopped reading channel data");
  }

  private void onChannelData(ChannelDataMessageEvent event) {
    ChannelData channelData = event.getChannelDataMessage();

    DatagramPacket datagramPacket = new DatagramPacket(channelData.getData(), channelData.getDataLength());
    datagramPacket.setSocketAddress(event.getRemoteAddress());
    onPacketReceived(datagramPacket);
  }
}
