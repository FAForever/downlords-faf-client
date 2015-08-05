package com.faforever.client.legacy.proxy;

import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.io.QDataOutputStream;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.SocketAddressUtil;
import com.google.common.annotations.VisibleForTesting;
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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
   * This byte marks a "connect by intermediary" domain. (Description copied from python code) This is needed for "UDP
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

  @VisibleForTesting
  Map<Integer, Peer> peersByUid;

  /**
   * Maps peer addresses (local and public) to peers.
   */
  @VisibleForTesting
  Map<String, Peer> peersByAddress;

  @VisibleForTesting
  boolean gameLaunched;
  boolean bottleneck;
  private InetAddress localInetAddr;
  /**
   * Public UDP socket that receives game data if p2p proxy is enabled (default port 6112).
   */
  private DatagramSocket publicSocket;
  private final Random random;
  int uid;
  /**
   * Socket to the FAF proxy server.
   */
  private Socket fafProxySocket;
  private Set<OnP2pProxyInitializedListener> onP2pProxyInitializedListeners;

  /**
   * Holds UDP sockets that represent other players. Key is the player's number (0 - 11).
   */
  @VisibleForTesting
  final Map<Integer, DatagramSocket> proxySocketsByPlayerNumber;
  private QDataOutputStream fafProxyOutputStream;
  private QDataInputStream fafProxyReader;

  /**
   * Lock to synchronize multiple threads trying to read/write/open a FAF proxy connection
   */
  private final Object proxyLock;

  public ProxyImpl() {
    proxyLock = new Object();
    random = new Random();
    peersByUid = new HashMap<>();
    peersByAddress = new HashMap<>();
    localInetAddr = InetAddress.getLoopbackAddress();
    onP2pProxyInitializedListeners = new HashSet<>();
    proxySocketsByPlayerNumber = new ConcurrentHashMap<>();
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
      logger.debug("Closing connection FAF proxy");
      fafProxySocket.close();
    }
  }

  /**
   * Starts a background reader that reads all incoming UDP data (from FA) of the given socket and forwards it to the
   * FAF proxy. If the connection fails, it does not reconnect automatically.
   *
   * @param proxySocket a local UDP socket representing another player
   */
  private void startFaReaderInBackground(int playerNumber, int playerUid, final DatagramSocket proxySocket) throws IOException {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ensureFafProxyConnection();

        byte[] buffer = new byte[1024];
        DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

        while (!isCancelled()) {
          try {
            proxySocket.receive(datagramPacket);

            logger.trace("Received {} bytes from FA for player #{}, forwarding to FAF proxy", datagramPacket.getLength(), playerNumber);
          } catch (SocketException | EOFException e) {
            logger.info("Proxy socket for player #{} has been closed ({})", e.getMessage());
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

  /**
   * Starts a reader in background that reads the FAF proxy data and forwards that data to FA as if it was sent by a
   * specific player. We're the man-in-the-middle.
   */
  private void startFafProxyReaderInBackground() {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        int port = preferencesService.getPreferences().getForgedAlliance().getPort();

        byte[] payloadBuffer = new byte[1024];
        byte[] datagramBuffer = new byte[1024];
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
          logger.debug("Connection closed");
        }
        return null;
      }
    });
  }

  private void sendUid(int uid) throws IOException {
    logger.debug("Sending UID to server: {}", uid);

    synchronized (proxyLock) {
      fafProxyOutputStream.writeInt(Short.BYTES);
      fafProxyOutputStream.writeShort(uid);
      fafProxyOutputStream.flush();
    }
  }

  @Override
  public void updateConnectedState(int uid, boolean connected) {
    Peer peer = peersByUid.get(uid);
    if (peer == null) {
      logger.warn("Can't update connected state for unknown peer: {}", uid);
      return;
    }
    if (!connected) {
      peersByUid.remove(uid);
    }
    peer.connected = connected;
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

    return SocketAddressUtil.toString((InetSocketAddress) peer.localSocket.getLocalSocketAddress());
  }

  @Override
  public void registerP2pPeerIfNecessary(String publicAddress) {
    if (peersByAddress.containsKey(publicAddress)) {
      logger.debug("P2P peer '{}' is already registered", publicAddress);
      return;
    }

    logger.debug("Registering P2P peer '{}'", publicAddress);

    try {
      DatagramSocket localSocket = new DatagramSocket(new InetSocketAddress(localInetAddr, 0));

      Peer peer = new Peer();
      peer.inetSocketAddress = toInetSocketAddress(publicAddress);
      peer.localSocket = localSocket;

      redirectLocalToRemote(peer);

      String localAddress = SocketAddressUtil.toString((InetSocketAddress) peer.localSocket.getLocalSocketAddress());

      peersByAddress.put(publicAddress, peer);
      peersByAddress.put(localAddress, peer);
    } catch (SocketException e) {
      logger.warn("Could not create a local UDP socket", e);
    }
  }

  @Override
  public void initializeP2pProxy() throws SocketException {
    int port = preferencesService.getPreferences().getForgedAlliance().getPort();
    publicSocket = new DatagramSocket(port);
    readPublicSocketInBackground(publicSocket);

    onP2pProxyInitializedListeners.forEach(OnP2pProxyInitializedListener::onP2pProxyInitialized);
  }

  @Override
  public void setUidForPeer(String publicAddress, int peerUid) {
    Peer peer = peersByAddress.get(publicAddress);

    if (peer == null) {
      logger.warn("Got UID for unknown peer: {}", publicAddress);
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

  @Override
  public void addOnP2pProxyInitializedListener(OnP2pProxyInitializedListener listener) {
    this.onP2pProxyInitializedListeners.add(listener);
  }

  private void readPublicSocketInBackground(DatagramSocket publicSocket) {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        while (isCancelled()) {
          byte[] buffer = new byte[1024];
          DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

          publicSocket.receive(datagramPacket);

          InetSocketAddress publicAddress = (InetSocketAddress) datagramPacket.getSocketAddress();

          String publicAddressString = SocketAddressUtil.toString(publicAddress);

          Peer peer = peersByAddress.get(publicAddressString);
          registerP2pPeerIfNecessary(publicAddressString);

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

  @VisibleForTesting
  static boolean isReconnectionSequence(byte[] data) {
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
