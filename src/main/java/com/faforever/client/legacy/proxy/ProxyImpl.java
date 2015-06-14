package com.faforever.client.legacy.proxy;

import com.faforever.client.legacy.writer.QDataOutputStream;
import com.faforever.client.legacy.writer.QDataReader;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.SocketAddressUtil;
import com.google.common.net.InetAddresses;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

// FIXME implement disconnect events
public class ProxyImpl implements Proxy {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final byte[] RECONNECTION_ESCAPE_PREFIX = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

  private static final int PROXY_CONNECTION_TIMEOUT = 10000;

  /**
   * Number of bytes per connection tag.
   */
  private static final int TAG_LENGTH = 8;
  private static final int THEIR_TAG_BEGIN_INDEX = RECONNECTION_ESCAPE_PREFIX.length + 1;
  private static final int THEIR_TAG_END_INDEX = THEIR_TAG_BEGIN_INDEX + TAG_LENGTH;

  /**
   * This byte marks a connection tag offer. That is, the client tells us "hey, this is my tag".
   */
  private static final char MESSAGE_OFFER_TAG = 0x01;

  /**
   * This byte marks a connection tag confirmation request. That is, the client requests us "hey, this is the tag I got
   * from you. Is it correct?".
   */
  private static final char MESSAGE_REQUEST_TAG_CONFIRMATION = 0x0B;

  /**
   * This byte marks a "decline" domain.
   */
  private static final byte MESSAGE_DECLINE = 0x03;

  /**
   * This byte marks a "ack" domain.
   */
  private static final byte MESSAGE_ACKNOWLEDGE = 0x02;

  /**
   * This yte marks a "connect by intermediary" domain. (Description copied from python code) This is needed for "UDP
   * hole punching". In a situation where the disconnected peer attempts to reconnect to a peer whose NAT requires UDP
   * hole punching the reconnect would fail, because no hole has been punched for the new IP address. But in this case
   * the disconnected peer does not require hole punching, because otherwise p2p udp channel would not have been
   * possible without a proxy anyway. The disconnected peer announces his (promiscuous) new IP:port to a third peer to
   * which it already has reestablished a connection, which in turn forwards it to all peers to which he still has the
   * old and good connections. the peer that needs to initiate the udp hole punch gets the new IP:port via this third
   * peer. this does not work in a 1v1 where the peer that does not require hole punching gets disconnected though (that
   * special case requires another third party: the server and an additional mechanism). the same failure happens in a
   * XvX where the disconnected peer is the only one that did not require hole punching the format of this domain is the
   * same as for the reconnect domain and the tag is the same as it would be in the reconnect domain. on the second leg,
   * when the third party forwards this reconnect-by-intermediary domain it includes the originator IP:port of this
   * domain in its reconnect-by-intermediary-2 domain
   */
  private static final byte MESSAGE_RECONNECT_BY_INTERMEDIARY = 0x11;

  /**
   * This byte marks a reconnect domain that has been forwarded by an intermediary peer.
   */
  private static final byte MESSAGE_RECONNECT_BY_INTERMEDIARY_2 = 0x012;

  /**
   * This byte marks a "reconnect request" domain. Such a domain doesn't contain any additional information.
   */
  private static final byte MESSAGE_RECONNECT_REQUEST = 0x017;
  private static final int IPV4_BEGIN_INDEX = 24;
  private static final int IPV4_END_INDEX = IPV4_BEGIN_INDEX + 4 * 8;

  // TODO find out and document what does "rate limit" means
  private static final int TAG_OFFER_RATELIMIT = 5000;

  // TODO find out what exactly this means and document it
  public static final int PROXY_UPDATE_INTERVAL = 10000;
  private static final int MAX_TAG_OFFERS = 10;

  /**
   * Number of prefix bytes for writing a QByteArray as a QVariant.
   */
  private static final int Q_BYTE_ARRAY_PREFIX_LENGTH = 9;

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  private Map<Integer, Peer> peersByUid;
  private Map<String, Peer> peersByAddress;
  private boolean gameLaunched;
  private boolean bottleneck;
  private InetAddress localInetAddr;
  /**
   * Public UDP socket that receives game data if p2p proxy is enabled (default port 6112).
   */
  private DatagramSocket publicSocket;
  private final Random random;
  private int uid;
  /**
   * Socket to the FAF proxy server.
   */
  private Socket fafProxySocket;
  private long lastProxyDataTimestamp;
  private boolean p2pProxyEnabled;
  private Set<Integer> testedLoopbackPorts;
  private Set<OnProxyInitializedListener> onProxyInitializedListeners;

  /**
   * Holds UDP sockets that represent other players. Key is the player's number (0 - 11).
   */
  private final Map<Integer, DatagramSocket> proxySockets;
  private QDataOutputStream fafProxyOutputStream;
  private QDataReader fafProxyReader;
  private ForgedAlliancePrefs forgedAlliancePrefs;
  /**
   * Lock to synchronize multiple threads trying to (re)open a proxy connection
   */
  private final Object proxyLock;
  /**
   * Maps player numbers to CountDownLatches to synchronize 1. the FAF relay server telling us to open a proxy port for
   * a user and 2. the FAF proxy server sending us data for a user. If the proxy server sends data before the request
   * from the relay server has been received, there will be a mismatch. Using the latches, we can wait for the relay
   * server request to be processed before forwarding data.
   */
  private final Map<Integer, CountDownLatch> proxySocketLatches;

  public ProxyImpl(ForgedAlliancePrefs forgedAlliancePrefs) {
    this.forgedAlliancePrefs = forgedAlliancePrefs;

    proxyLock = new Object();
    random = new Random();
    peersByUid = new HashMap<>();
    peersByAddress = new HashMap<>();
    localInetAddr = InetAddress.getLoopbackAddress();
    testedLoopbackPorts = new HashSet<>();
    onProxyInitializedListeners = new HashSet<>();
    proxySockets = new ConcurrentHashMap<>();
    proxySocketLatches = new HashMap<>();
  }

  @Override
  public void closeSockets() {
    logger.info("Closing proxy sockets");
    for (DatagramSocket datagramSocket : proxySockets.values()) {
      logger.debug("Closing socket {}", datagramSocket.getLocalPort());
      datagramSocket.close();
    }
    // FIXME close/disconnect/free proxy sockets
  }

  /**
   * Starts a background reader that reads all incoming UDP data (from FA) of the given socket and forwards it to the
   * FAF proxy.
   *
   * @param proxySocket a local UDP socket representing another player
   */
  private void startProxyReaderInBackground(int playerNumber, int playerUid, final DatagramSocket proxySocket) throws IOException {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ensureFafProxyConnection();

        byte[] buffer = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

        int port = proxySocket.getLocalPort();

        try {
          while (!isCancelled()) {
            proxySocket.receive(datagramPacket);

            logger.trace("Received {} bytes from FA, forwarding to FAF proxy", datagramPacket.getLength(), port);

            writeToFafProxyServer(playerNumber, playerUid, datagramPacket);
          }
        } catch (SocketException | EOFException e) {
          logger.info("Proxy connection closed");
        }

        return null;
      }
    });
  }

  private void writeToFafProxyServer(int playerNumber, int uid, DatagramPacket datagramPacket) throws IOException {
    byte[] data = Arrays.copyOfRange(datagramPacket.getData(), datagramPacket.getOffset(), datagramPacket.getLength());

    // Number of bytes for port, uid and QByteArray (prefix stuff plus data length)
    fafProxyOutputStream.writeInt(Short.BYTES + Short.BYTES + Q_BYTE_ARRAY_PREFIX_LENGTH + data.length);
    fafProxyOutputStream.writeShort(playerNumber);
    // WTF: The UID can be larger than 65535 but who cares? Just cut it off, what can possibly happen? -.-
    fafProxyOutputStream.writeShort(uid);
    fafProxyOutputStream.writeQByteArray(data);

    fafProxyOutputStream.flush();
  }

  private void ensureFafProxyConnection() throws IOException {
    synchronized (proxyLock) {
      if (fafProxySocket != null && fafProxySocket.isConnected()) {
        return;
      }
    }

    String proxyHost = environment.getProperty("proxy.host");
    Integer proxyPort = environment.getProperty("proxy.port", Integer.class);

    logger.info("Connecting to FAF proxy at {}:{}", proxyHost, proxyPort);

    fafProxySocket = new Socket();
    fafProxySocket.setTcpNoDelay(true);
    fafProxySocket.connect(new InetSocketAddress(proxyHost, proxyPort), PROXY_CONNECTION_TIMEOUT);

    fafProxyOutputStream = new QDataOutputStream(new BufferedOutputStream(fafProxySocket.getOutputStream()));
    fafProxyReader = new QDataReader(new DataInputStream(new BufferedInputStream(fafProxySocket.getInputStream())));

    sendUid(uid);
    startFafProxyReaderInBackground();
  }

  /**
   * Starts a reader in background that reads the FAF proxy forwards its data to the corresponding player.
   */
  private void startFafProxyReaderInBackground() {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        byte[] payloadBuffer = new byte[1024];
        byte[] datagramBuffer = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(datagramBuffer, datagramBuffer.length);
        datagramPacket.setSocketAddress(new InetSocketAddress(localInetAddr, forgedAlliancePrefs.getPort()));

        while (!isCancelled()) {
          // Skip block size bytes, we have no use for it
          fafProxyReader.readInt32();

          int playerNumber = fafProxyReader.readShort();

          int payloadSize = fafProxyReader.readQByteArray(payloadBuffer);

          logger.trace("Received {} bytes from FAF proxy, forwarding to FA", payloadSize);

          datagramPacket.setData(payloadBuffer, 0, payloadSize);

          getProxySocketLatchForPlayer(playerNumber).await();
          DatagramSocket proxySocket = proxySockets.get(playerNumber);
          if (proxySocket == null) {
            logger.warn("Tried to start proxy reader for player #{} but no such socket has been opened", playerNumber);
          } else {
            proxySocket.send(datagramPacket);
          }
        }
        return null;
      }
    });
  }

  /**
   * Ensures that a proxy socket latch is available for the given player.
   *
   * @see #proxySocketLatches
   */
  private CountDownLatch getProxySocketLatchForPlayer(int playerNumber) {
    synchronized (proxySocketLatches) {
      if (!proxySocketLatches.containsKey(playerNumber)) {
        proxySocketLatches.put(playerNumber, new CountDownLatch(1));
      }
      return proxySocketLatches.get(playerNumber);
    }
  }

  private void sendUid(int uid) throws IOException {
    logger.debug("Sending UID to server: {}", uid);

    fafProxyOutputStream.writeInt(Short.BYTES);
    fafProxyOutputStream.writeShort(uid);
    fafProxyOutputStream.flush();
  }

  @Override
  public void updateConnectedState(int uid, boolean connected) {
    Peer peer = peersByUid.get(uid);
    if (!connected && peer.connected) {
      peersByUid.remove(uid);
      peer.connected = false;
    }
  }

  @Override
  public void setGameLaunched(boolean gameLaunched) {
    this.gameLaunched = gameLaunched;
  }


  @Override
  public void setBottleneck(boolean bottleneck) {
    this.bottleneck = bottleneck;
  }

  @Override
  public String translateToPublic(String localAddress) {
    Peer peer = peersByAddress.get(localAddress);

    if (peer == null) {
      logger.warn("No peer found for local address: " + localAddress);
      return null;
    }

    return SocketAddressUtil.toString(peer.inetSocketAddress);
  }

  @Override
  public String translateToLocal(String publicAddress) {
    Peer peer = peersByAddress.get(publicAddress);

    return SocketAddressUtil.toString(new InetSocketAddress(localInetAddr, peer.localSocket.getLocalPort()));
  }

  @Override
  public void registerPeerIfNecessary(String publicAddress) {
    if (peersByAddress.containsKey(publicAddress)) {
      logger.debug("Peer '{}' is already registered", publicAddress);
      return;
    }

    logger.debug("Registering peer '{}'", publicAddress);

    try {
      DatagramSocket localSocket = new DatagramSocket(new InetSocketAddress(localInetAddr, 0));

      Peer peer = new Peer();
      peer.inetSocketAddress = toInetSocketAddress(publicAddress);
      peer.localSocket = localSocket;

      redirectLocalToRemote(peer);

      peersByAddress.put(publicAddress, peer);
    } catch (SocketException e) {
      logger.warn("Could not create a local UDP socket", e);
    }
  }

  @Override
  public void initializeP2pProxy() throws SocketException {
    publicSocket = new DatagramSocket(forgedAlliancePrefs.getPort());
    readPublicSocketInBackground(publicSocket);

    onProxyInitializedListeners.forEach(Proxy.OnProxyInitializedListener::onProxyInitialized);
  }

  @Override
  public void setUidForPeer(String peerAddress, int peerUid) {
    Peer peer = peersByAddress.get(peerAddress);

    if (peer == null) {
      logger.warn("Got UID for unknown peer: {}", peerAddress);
      return;
    }

    peer.uid = peerUid;
    peersByUid.put(peerUid, peer);
  }

  @Override
  public void setUid(int uid) {
    logger.debug("UID has been set to {}", uid);
    this.uid = uid;
  }

  @Override
  public int getPort() {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public InetSocketAddress bindAndGetProxySocketAddress(int playerNumber, int playerUid) throws IOException {
    logger.debug("Opening proxy socket for player #{} with uid {}", playerNumber, playerUid);

    DatagramSocket proxySocket = new DatagramSocket(new InetSocketAddress(localInetAddr, 0));
    InetSocketAddress proxySocketAddress = (InetSocketAddress) proxySocket.getLocalSocketAddress();

    logger.debug("Registering proxy socket for player #{}", playerNumber);
    proxySockets.put(playerNumber, proxySocket);
    getProxySocketLatchForPlayer(playerNumber).countDown();

    logger.debug("Player #{} with uid {} has been assigned to proxy socket {}",
        playerNumber, playerUid, SocketAddressUtil.toString(proxySocketAddress)
    );

    startProxyReaderInBackground(playerNumber, playerUid, proxySocket);

    return proxySocketAddress;
  }

  @Override
  public void addOnProxyInitializedListener(OnProxyInitializedListener listener) {
    this.onProxyInitializedListeners.add(listener);
  }

  private void readPublicSocketInBackground(DatagramSocket publicSocket) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (isCancelled()) {
          byte[] buffer = new byte[1024];
          DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

          publicSocket.receive(datagramPacket);

          InetSocketAddress socketAddress = (InetSocketAddress) datagramPacket.getSocketAddress();

          String socketAddressString = SocketAddressUtil.toString(socketAddress);

          Peer peer = peersByAddress.get(socketAddressString);
          registerPeerIfNecessary(socketAddressString);

          dispatchPublicSocketData(datagramPacket, peer);
        }
        return null;
      }
    });
  }

  private void dispatchPublicSocketData(DatagramPacket datagramPacket, Peer peer) throws IOException {
    byte[] data = datagramPacket.getData();
    InetSocketAddress originSocketAddress = (InetSocketAddress) datagramPacket.getSocketAddress();

    if (isReconnectionSequence(data)) {
      dispatchReconnectMessage(peer, data, originSocketAddress);
    } else {
      datagramPacket.setAddress(peer.localSocket.getInetAddress());
      datagramPacket.setPort(ProxyUtils.translateToProxyPort(publicSocket.getLocalPort()));

      peer.localSocket.send(datagramPacket);

      if (peer.connected && peer.currentlyReconnecting || !peer.ourConnectionTagAcknowledged && !peer.ourConnectionTagDeclined) {
        if (peer.tagOfferTimestamp + TAG_OFFER_RATELIMIT >= System.currentTimeMillis()) {
          return;
        }

        if (peer.numberOfTagOffers >= MAX_TAG_OFFERS) {
          logger.info("Giving up on tag offers for peer '{}' after '{}' attempts", originSocketAddress, peer.numberOfTagOffers);
          return;
        }

        peer.tagOfferTimestamp = System.currentTimeMillis();
        if (peer.ourConnectionTag == null) {
          peer.ourConnectionTag = generateConnectionTag();
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(RECONNECTION_ESCAPE_PREFIX);

        if (peer.ourConnectionTagAcknowledged) {
          byteArrayOutputStream.write(MESSAGE_REQUEST_TAG_CONFIRMATION);
        } else {
          byteArrayOutputStream.write(MESSAGE_OFFER_TAG);
        }

        byteArrayOutputStream.write(peer.ourConnectionTag);

        logger.debug("Sending connection tag '{}' to peer '{}'", peer.ourConnectionTag);

        byte[] buffer = byteArrayOutputStream.toByteArray();
        publicSocket.send(new DatagramPacket(buffer, buffer.length, originSocketAddress));

        peer.numberOfTagOffers++;
      }
    }
  }

  private byte[] generateConnectionTag() {
    byte[] tag = new byte[TAG_LENGTH];
    random.nextBytes(tag);
    return tag;
  }

  private void dispatchReconnectMessage(Peer peer, byte[] data, InetSocketAddress originSocketAddress) throws IOException {
    byte[] tag = Arrays.copyOfRange(data, THEIR_TAG_BEGIN_INDEX, THEIR_TAG_END_INDEX);

    byte messageType = data[15];
    switch (messageType) {
      case MESSAGE_OFFER_TAG:
        if (peer.connectionTag != null && !Arrays.equals(peer.connectionTag, tag)) {
          declineTag(originSocketAddress, peer, tag);
        } else {
          updateTagForPeer(originSocketAddress, peer, tag);
        }
        break;

      case MESSAGE_REQUEST_TAG_CONFIRMATION:
        if (peer.connectionTag == null) {
          declineTag(originSocketAddress, peer, tag);
        } else {
          updateTagForPeer(originSocketAddress, peer, tag);
        }
        break;

      case MESSAGE_ACKNOWLEDGE:
        if (Arrays.equals(peer.ourConnectionTag, tag)) {
          logger.debug("Peer '{}' acknowledged our connection tag '{}'", peer, tag);

          peer.ourConnectionTagAcknowledged = true;
          peer.numberOfTagOffers = 0;
          peer.currentlyReconnecting = false;
        } else {
          logger.warn("Peer '{}' acknowledged a tag we didn't send: {}", peer, tag);
        }
        break;

      case MESSAGE_DECLINE:
        if (Arrays.equals(peer.ourConnectionTag, tag)) {
          logger.warn("Peer '{}' declined our tag '{}' even though it was correct", peer, tag);
          peer.ourConnectionTagDeclined = true;
        } else {
          logger.warn("Peer '{}' declined tag '{}' which we didn't send", peer, tag);
        }
        break;

      case MESSAGE_RECONNECT_BY_INTERMEDIARY:
        reconnectByIntermediary(originSocketAddress, tag);
        break;

      case MESSAGE_RECONNECT_BY_INTERMEDIARY_2:
        reconnectByIntermediary2(originSocketAddress, tag, data);
        break;

      case MESSAGE_RECONNECT_REQUEST:
        reconnectPeer(originSocketAddress, peer, tag);
        break;

      default:
        logger.warn("Unknown reconnection message type: {}", messageType);
    }
  }

  private void reconnectPeer(InetSocketAddress originSocketAddress, Peer peer, byte[] tag) {
    logger.debug("Reconnect request from peer: {}", originSocketAddress);

    if (Arrays.equals(peer.connectionTag, tag)) {
      logger.debug("Ignoring reconnect request since the connection tag matches the current connection");
      return;
    }

    String oldPeerAddress = null;
    String newPeerAddress = null;

    for (Map.Entry<String, Peer> entry : peersByAddress.entrySet()) {
      Peer iteratingPeer = entry.getValue();

      if (Arrays.equals(iteratingPeer.connectionTag, tag)) {
        iteratingPeer.inetSocketAddress = originSocketAddress;

        oldPeerAddress = SocketAddressUtil.toString(iteratingPeer.inetSocketAddress);
        newPeerAddress = SocketAddressUtil.toString(originSocketAddress);
        break;
      }
    }

    if (oldPeerAddress == null) {
      logger.warn("Peer could not be found for update: {}", originSocketAddress);
      return;
    }

    peersByAddress.put(newPeerAddress, peersByAddress.remove(oldPeerAddress));
  }

  /**
   * Reads the new peer socket address from the reconnect-by-intermediary-2 and updates the peer's information in
   * memory.
   */
  private void reconnectByIntermediary2(InetSocketAddress intermediaryInetSocketAddress, byte[] tag, byte[] data) throws UnknownHostException {
    // TODO this breaks IPv4 compatibility
    InetAddress inetAddress = InetAddresses.fromLittleEndianByteArray(Arrays.copyOfRange(data, IPV4_BEGIN_INDEX, IPV4_END_INDEX));
    int port = data[28] << 8 | data[29];

    InetSocketAddress senderInetSocketAddress = new InetSocketAddress(inetAddress, port);

    logger.debug("Received reconnect-by-intermediary-2 from '{}' for '{}'", intermediaryInetSocketAddress, senderInetSocketAddress);

    boolean found = false;
    String oldPeerAddress = null;
    String newPeerAddress = null;

    for (Map.Entry<String, Peer> entry : peersByAddress.entrySet()) {
      Peer peer = entry.getValue();
      String peerAddress = entry.getKey();

      byte[] peerConnectionTag = peer.connectionTag;
      if (peerConnectionTag != null && Arrays.equals(peerConnectionTag, tag)) {
        found = true;

        if (!Objects.equals(peer.inetSocketAddress, senderInetSocketAddress)) {
          logger.debug("Updating peer address from '{}' to '{}'", peer.inetSocketAddress, senderInetSocketAddress);
          peer.inetSocketAddress = senderInetSocketAddress;

          oldPeerAddress = peerAddress;
          newPeerAddress = SocketAddressUtil.toString(senderInetSocketAddress);
        }
      }
    }

    if (!found) {
      logger.warn("Unknown peer: {}", senderInetSocketAddress);
    } else if (oldPeerAddress != null) {
      peersByAddress.put(newPeerAddress, peersByAddress.remove(oldPeerAddress));
    }
  }

  private void reconnectByIntermediary(InetSocketAddress originSocketAddress, byte[] tag) throws IOException {
    for (Map.Entry<String, Peer> entry : peersByAddress.entrySet()) {
      Peer peer = entry.getValue();

      if (!Objects.equals(peer.inetSocketAddress, originSocketAddress)) {
        logger.debug("Passing on reconnect-by-intermediary to {}", originSocketAddress);

        int port = originSocketAddress.getPort();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(RECONNECTION_ESCAPE_PREFIX.length + 1 + tag.length + 4 + 2);
        byteArrayOutputStream.write(RECONNECTION_ESCAPE_PREFIX);
        byteArrayOutputStream.write(MESSAGE_RECONNECT_REQUEST);
        byteArrayOutputStream.write(tag);
        byteArrayOutputStream.write(originSocketAddress.getAddress().getAddress());

        // Write the port as two bytes, little endian
        byteArrayOutputStream.write((byte) (port >> 8));
        byteArrayOutputStream.write((byte) port);

        byte[] buffer = byteArrayOutputStream.toByteArray();

        publicSocket.send(new DatagramPacket(buffer, buffer.length, originSocketAddress));
      }
    }
  }

  private void declineTag(SocketAddress originSocketAddress, Peer peer, byte[] theirTag) throws IOException {
    logger.debug("Declining tag offer '{}' from peer '{}'", peer, theirTag);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(RECONNECTION_ESCAPE_PREFIX.length + 1 + theirTag.length);
    byteArrayOutputStream.write(RECONNECTION_ESCAPE_PREFIX);
    byteArrayOutputStream.write(MESSAGE_DECLINE);
    byteArrayOutputStream.write(theirTag);

    byte[] buffer = byteArrayOutputStream.toByteArray();

    publicSocket.send(new DatagramPacket(buffer, buffer.length, originSocketAddress));
  }

  private void updateTagForPeer(SocketAddress originSocketAddress, Peer peer, byte[] theirTag) throws IOException {
    logger.debug("Peer '{}' offers tag  '{}'", peer, theirTag);

    peer.connectionTag = theirTag;

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(RECONNECTION_ESCAPE_PREFIX.length + 1 + theirTag.length);
    byteArrayOutputStream.write(RECONNECTION_ESCAPE_PREFIX);
    byteArrayOutputStream.write(MESSAGE_ACKNOWLEDGE);
    byteArrayOutputStream.write(theirTag);

    byte[] buffer = byteArrayOutputStream.toByteArray();

    publicSocket.send(new DatagramPacket(buffer, buffer.length, originSocketAddress));
  }

  private void redirectLocalToRemote(Peer peer) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        byte[] buffer = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

        DatagramSocket localSocket = peer.localSocket;
        localSocket.receive(datagramPacket);

        InetSocketAddress inetSocketAddress = peer.inetSocketAddress;

        datagramPacket.setAddress(inetSocketAddress.getAddress());
        datagramPacket.setPort(inetSocketAddress.getPort());

        publicSocket.send(datagramPacket);

        return null;
      }
    });
  }

  private static InetSocketAddress toInetSocketAddress(String hostAndPort) {
    int portDividerIndex = hostAndPort.lastIndexOf(":");

    String host = hostAndPort.substring(0, portDividerIndex);
    int port = Integer.parseInt(hostAndPort.substring(portDividerIndex + 1, hostAndPort.length()));

    return new InetSocketAddress(InetAddresses.forString(host), port);
  }

  public static boolean isReconnectionSequence(byte[] data) {
    if (data.length < RECONNECTION_ESCAPE_PREFIX.length) {
      return false;
    }

    for (int i = 0; i < RECONNECTION_ESCAPE_PREFIX.length; i++) {
      if (data[i] != RECONNECTION_ESCAPE_PREFIX[i]) {
        return false;
      }
    }

    return true;
  }
}
