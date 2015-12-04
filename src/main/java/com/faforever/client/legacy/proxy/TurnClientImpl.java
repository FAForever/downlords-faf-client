package com.faforever.client.legacy.proxy;

import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.io.QDataOutputStream;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.SocketAddressUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.concurrent.Task;
import org.ice4j.StunException;
import org.ice4j.StunMessageEvent;
import org.ice4j.StunResponseEvent;
import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.attribute.RequestedTransportAttribute;
import org.ice4j.message.MessageFactory;
import org.ice4j.message.Request;
import org.ice4j.message.Response;
import org.ice4j.socket.IceUdpSocketWrapper;
import org.ice4j.stack.StunStack;
import org.ice4j.stunclient.BlockingRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class TurnClientImpl implements TurnClient {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final int PROXY_CONNECTION_TIMEOUT = 10000;

  /**
   * Number of prefix bytes for writing a QByteArray as a QVariant.
   */
  private static final int Q_BYTE_ARRAY_PREFIX_LENGTH = 9;
  @VisibleForTesting
  final Map<Integer, Peer> peersByUid;
  /**
   * Maps peer addresses (local and public) to peers.
   */
  @VisibleForTesting
  final Map<String, Peer> peersByAddress;
  /**
   * Holds UDP sockets that represent other players. Key is the player's number (0 - 11).
   */
  @VisibleForTesting
  final Map<Integer, DatagramSocket> proxySocketsByPlayerNumber;
  final private InetAddress localInetAddr;
  /**
   * Lock to synchronize multiple threads trying to read/write/open a FAF proxy connection
   */
  private final Object proxyLock;
  @Resource
  Environment environment;
  @Resource
  PreferencesService preferencesService;
  @Resource
  Executor executor;
  boolean bottleneck;
  int uid;
  /**
   * Socket to the FAF proxy server.
   */
  private Socket fafProxySocket;
  private QDataOutputStream fafProxyOutputStream;
  private QDataInputStream fafProxyReader;
  private DatagramSocket socket;

  public TurnClientImpl() {
    proxyLock = new Object();
    peersByUid = new HashMap<>();
    peersByAddress = new HashMap<>();
    localInetAddr = InetAddress.getLoopbackAddress();
    proxySocketsByPlayerNumber = new ConcurrentHashMap<>();
  }

  @Override
  public CompletableFuture<Void> connect() {
    String turnHost = environment.getProperty("turn.host");
    int turnPort = environment.getProperty("turn.port", int.class);

    TransportAddress turnServerAddress = new TransportAddress(turnHost, turnPort, Transport.UDP);

    return CompletableFuture.runAsync(() -> {
      try {
        DatagramSocket datagramSocket = new DatagramSocket(0, InetAddress.getLocalHost());
        TransportAddress localAddress = new TransportAddress((InetSocketAddress) datagramSocket.getLocalSocketAddress(), Transport.UDP);

        StunStack stunStack = new StunStack();
        stunStack.addSocket(new IceUdpSocketWrapper(datagramSocket), turnServerAddress);

        Request allocateRequest = MessageFactory.createAllocateRequest(RequestedTransportAttribute.UDP, false);

        BlockingRequestSender blockingRequestSender = new BlockingRequestSender(stunStack, localAddress);
        StunMessageEvent stunMessageEvent = blockingRequestSender.sendRequestAndWaitForResponse(
            allocateRequest, turnServerAddress
        );
        Response response = ((StunResponseEvent) stunMessageEvent).getResponse();
        System.out.println(response);
      } catch (StunException | IOException e) {
        throw new RuntimeException(e);
      }
    }, executor);
  }

  @Override
  public void close() throws IOException {
    logger.info("Closing proxy sockets");

    Iterator<Map.Entry<Integer, DatagramSocket>> iterator = proxySocketsByPlayerNumber.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Integer, DatagramSocket> entry = iterator.next();
      Integer playerNumber = entry.getKey();
      DatagramSocket socket = entry.getValue();

      logger.debug("Closing socket {} for player #{}", socket.getLocalPort(), playerNumber);

      socket.close();
      iterator.remove();
    }
    if (fafProxySocket != null) {
      logger.info("Closing connection FAF proxy");
      fafProxySocket.close();
    }
  }

  @Override
  public InetSocketAddress bindAndGetProxySocketAddress(int playerNumber, int playerUid) throws IOException {
    DatagramSocket proxySocket = proxySocketsByPlayerNumber.get(playerNumber);

    if (proxySocket == null) {
      proxySocket = new DatagramSocket(new InetSocketAddress(localInetAddr, 0));
    }

    proxySocketsByPlayerNumber.put(playerNumber, proxySocket);

    InetSocketAddress proxySocketAddress = (InetSocketAddress) proxySocket.getLocalSocketAddress();
    logger.debug("Player #{} with uid {} has been assigned to proxy socket {}",
        playerNumber, playerUid, SocketAddressUtil.toString(proxySocketAddress)
    );

    startFaReaderInBackground(playerNumber, playerUid, proxySocket);

    return proxySocketAddress;
  }

  /**
   * Starts a background reader that reads all incoming UDP data (from FA) of the given socket and forwards it to the
   * FAF proxy. If the connection fails, it does not reconnect automatically.
   *
   * @param proxySocket a local UDP socket representing another player
   */
  private void startFaReaderInBackground(int playerNumber, int playerUid, final DatagramSocket proxySocket) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ensureFafProxyConnection();

        byte[] buffer = new byte[8092];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

        while (!isCancelled()) {
          try {
            proxySocket.receive(datagramPacket);

            logger.trace("Received {} bytes from FA for player #{}, forwarding to FAF proxy", datagramPacket.getLength(), playerNumber);
          } catch (SocketException | EOFException e) {
            logger.info("Proxy socket for player #{} has been closed ({})", playerNumber, e.getMessage());
            return null;
          }

          try {
            writeToFafProxyServer(playerNumber, playerUid, datagramPacket);
          } catch (SocketException | EOFException e) {
            // Sometimes the proxy disconnects for no apparent reason
            ensureFafProxyConnection();
          }
        }

        return null;
      }
    });
  }

  private void ensureFafProxyConnection() throws IOException {
    synchronized (proxyLock) {
      if (fafProxySocket != null && fafProxySocket.isConnected()) {
        return;
      }

      String proxyHost = environment.getProperty("proxy.host");
      int proxyPort = environment.getProperty("proxy.port", int.class);

      logger.info("Connecting to FAF proxy at {}:{}", proxyHost, proxyPort);

      fafProxySocket = new Socket();
      fafProxySocket.setTcpNoDelay(true);
      fafProxySocket.connect(new InetSocketAddress(proxyHost, proxyPort), PROXY_CONNECTION_TIMEOUT);

      fafProxyOutputStream = new QDataOutputStream(new BufferedOutputStream(fafProxySocket.getOutputStream()));
      fafProxyReader = new QDataInputStream(new DataInputStream(new BufferedInputStream(fafProxySocket.getInputStream())));

      sendUid(uid);
      startFafProxyReaderInBackground();
    }
  }

  private void writeToFafProxyServer(int playerNumber, int uid, DatagramPacket datagramPacket) throws IOException {
    byte[] data = Arrays.copyOfRange(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());

    synchronized (proxyLock) {
      // Number of bytes for port, uid and QByteArray (prefix stuff plus data length)
      fafProxyOutputStream.writeInt(Short.BYTES + Short.BYTES + Q_BYTE_ARRAY_PREFIX_LENGTH + data.length);
      fafProxyOutputStream.writeShort(playerNumber);
      // WTF: The UID can be larger than 65535 but who cares? Just cut it off, what can possibly happen? -.-
      fafProxyOutputStream.writeShort(uid);
      fafProxyOutputStream.writeQByteArray(data);
      fafProxyOutputStream.flush();
    }
  }

  private void sendUid(int uid) throws IOException {
    logger.debug("Sending UID to server: {}", uid);

    synchronized (proxyLock) {
      fafProxyOutputStream.writeInt(Short.BYTES);
      fafProxyOutputStream.writeShort(uid);
      fafProxyOutputStream.flush();
    }
  }

  /**
   * Starts a reader in background that reads the FAF proxy data and forwards that data to FA as if it was sent by a
   * specific player. We're the man-in-the-middle.
   */
  private void startFafProxyReaderInBackground() {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        int port = preferencesService.getPreferences().getForgedAlliance().getPort();

        byte[] payloadBuffer = new byte[8192];
        byte[] datagramBuffer = new byte[8192];
        DatagramPacket datagramPacket = new DatagramPacket(datagramBuffer, datagramBuffer.length);
        datagramPacket.setSocketAddress(new InetSocketAddress(localInetAddr, port));

        try {
          while (!isCancelled()) {
            // Skip block size bytes, we have no use for it
            fafProxyReader.readInt();

            int playerNumber = fafProxyReader.readShort();
            int payloadSize = fafProxyReader.readQByteArray(payloadBuffer);

            logger.trace("Forwarding {} bytes from FAF proxy, sent by player #{}", payloadSize, playerNumber);

            datagramPacket.setData(payloadBuffer, 0, payloadSize);

            DatagramSocket proxySocket = proxySocketsByPlayerNumber.get(playerNumber);
            if (proxySocket == null) {
              logger.warn("Discarding proxy data for player #{} as its socket is not yet ready", playerNumber);
              continue;
            }

            proxySocket.send(datagramPacket);
          }
        } catch (SocketException | EOFException e) {
          logger.debug("Connection closed ({})", e.getMessage());
        }
        return null;
      }
    });
  }
}
