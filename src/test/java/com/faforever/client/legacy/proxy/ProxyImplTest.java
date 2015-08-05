package com.faforever.client.legacy.proxy;

import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.io.QDataOutputStream;
import com.faforever.client.legacy.relay.FaDataInputStream;
import com.faforever.client.legacy.relay.FaDataOutputStream;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.SocketAddressUtil;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProxyImplTest extends AbstractPlainJavaFxTest {

  private class ProxyPackage {

    int length;
    int playerNumber;
    int uid;
    int dataLength;

    ProxyPackage(int length, int playerNumber, int uid, int dataLength) {
      this.length = length;
      this.playerNumber = playerNumber;
      this.uid = uid;
      this.dataLength = dataLength;
    }
  }

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  public static final int TIMEOUT = 2000000;
  public static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final int OTHER_UID_1 = 111;
  private static final int GAME_PORT = 6112;

  private ProxyImpl instance;
  private ServerSocket fafProxyServerSocket;
  private QDataOutputStream serverToLocalOutputStream;
  private boolean stopped;
  private BlockingQueue<ProxyPackage> packagesReceivedByFafProxyServer;
  private BlockingQueue<byte[]> dataReceivedByGame;
  private Socket gameToLocalProxySocket;
  private FaDataOutputStream gameToLocalProxyOutputStream;
  private FaDataInputStream gameFromLocalProxyInputStream;
  private CountDownLatch fafProxyConnectedLatch;
  private CountDownLatch fakeGameTerminatedLatch;
  private CountDownLatch fakeFafProxyTerminatedLatch;
  private DatagramSocket gameDatagramSocket;

  @Before
  public void setUp() throws Exception {
    instance = new ProxyImpl();
    instance.environment = mock(Environment.class);
    instance.preferencesService = mock(PreferencesService.class);
    Preferences preferences = mock(Preferences.class);
    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);

    startFakeFafProxyServer();

    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
    when(instance.preferencesService.getPreferences()).thenReturn(preferences);
    when(instance.environment.getProperty("proxy.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(instance.environment.getProperty("proxy.port", int.class)).thenReturn(fafProxyServerSocket.getLocalPort());
  }

  @After
  public void tearDown() throws Exception {
    stopped = true;
    IOUtils.closeQuietly(fafProxyServerSocket);
    IOUtils.closeQuietly(gameToLocalProxySocket);
    IOUtils.closeQuietly(gameDatagramSocket);
    if (fakeGameTerminatedLatch != null) {
      fakeGameTerminatedLatch.await();
    }
    if (fakeFafProxyTerminatedLatch != null) {
      fakeFafProxyTerminatedLatch.await();
    }
  }

  @Test
  public void testClose() throws Exception {
    instance.bindAndGetProxySocketAddress(1, 1234);
    assertThat(instance.proxySocketsByPlayerNumber.values(), hasSize(1));

    fafProxyConnectedLatch.await(TIMEOUT, TIMEOUT_UNIT);

    DatagramSocket peerSocket = instance.proxySocketsByPlayerNumber.get(1);
    assertThat(peerSocket.isClosed(), is(false));
    assertThat(gameToLocalProxySocket.isClosed(), is(false));

    instance.close();

    fakeFafProxyTerminatedLatch.await();

    assertThat(instance.proxySocketsByPlayerNumber.values(), empty());
    assertThat(peerSocket.isClosed(), is(true));
    assertThat(gameToLocalProxySocket.isClosed(), is(true));
  }

  @Test
  public void testUpdateConnectedStateUnknownPeer() throws Exception {
    instance.setUidForPeer("64.1.1.1:6112", OTHER_UID_1);
    instance.updateConnectedState(OTHER_UID_1, true);

    assertThat(instance.peersByUid.values(), empty());
  }

  @Test
  public void testUpdateConnectedStateTrue() throws Exception {
    instance.registerP2pPeerIfNecessary("64.1.1.1:6112");
    instance.setUidForPeer("64.1.1.1:6112", OTHER_UID_1);
    instance.updateConnectedState(OTHER_UID_1, true);

    assertThat(instance.peersByUid.values(), hasSize(1));

    Peer peer = instance.peersByUid.values().iterator().next();
    assertThat(peer.connected, is(true));
  }

  @Test
  public void testUpdateConnectedStateFalse() throws Exception {
    instance.registerP2pPeerIfNecessary("64.1.1.1:6112");
    instance.setUidForPeer("64.1.1.1:6112", OTHER_UID_1);

    assertThat(instance.peersByUid.values(), hasSize(1));
    Peer peer = instance.peersByUid.values().iterator().next();

    instance.updateConnectedState(OTHER_UID_1, false);

    assertThat(instance.peersByUid.values(), hasSize(0));
    assertThat(peer.connected, is(false));
  }

  @Test
  public void testSetGameLaunchedTrue() throws Exception {
    instance.setGameLaunched(true);
    assertThat(instance.gameLaunched, is(true));
  }

  @Test
  public void testSetGameLaunchedFalse() throws Exception {
    instance.setGameLaunched(false);
    assertThat(instance.gameLaunched, is(false));
  }

  @Test
  public void testSetBottleneckTrue() throws Exception {
    instance.setBottleneck(true);
    assertThat(instance.bottleneck, is(true));
  }

  @Test
  public void testSetBottleneckFalse() throws Exception {
    instance.setBottleneck(false);
    assertThat(instance.gameLaunched, is(false));
    assertThat(instance.bottleneck, is(false));
  }

  @Test
  public void testTranslate() throws Exception {
    instance.registerP2pPeerIfNecessary("64.1.1.1:6112");
    String localAddress = instance.translateToLocal("64.1.1.1:6112");
    String publicAddress = instance.translateToPublic(localAddress);
    assertThat(publicAddress, is("64.1.1.1:6112"));
  }

  @Test
  public void testRegisterPeerIfNecessary() throws Exception {
    assertNull(instance.peersByAddress.get("64.1.1.1:6112"));
    instance.registerP2pPeerIfNecessary("64.1.1.1:6112");
    assertNotNull(instance.peersByAddress.get("64.1.1.1:6112"));

    // TODO check if forwarding works
  }

  @Test
  public void testInitializeP2pProxy() throws Exception {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    instance.addOnP2pProxyInitializedListener(countDownLatch::countDown);

    instance.initializeP2pProxy();
    countDownLatch.await(1, TimeUnit.SECONDS);

    assertThat("P2P proxy has not initialized within timeout", countDownLatch.getCount(), is(0L));

    // TODO test that game port has been opened
  }

  @Test
  public void testSetUidForPeer() throws Exception {
    instance.registerP2pPeerIfNecessary("64.1.1.1:6112");
    instance.setUidForPeer("64.1.1.1:6112", 1);

    assertThat(instance.peersByUid.values(), hasSize(1));
    assertThat(instance.peersByUid.get(1).uid, is(1));
    assertThat(SocketAddressUtil.toString(instance.peersByUid.get(1).inetSocketAddress), is("64.1.1.1:6112"));
  }

  @Test
  public void testSetUid() throws Exception {
    assertThat(instance.uid, is(0));
    instance.setUid(123);
    assertThat(instance.uid, is(123));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetPort() throws Exception {
    instance.getPort();
  }

  @Test
  public void testBindAndGetProxySocketAddress() throws Exception {
    startFakeGameProcess();

    assertThat(instance.proxySocketsByPlayerNumber.values(), hasSize(0));
    int playerNumber = 4;

    instance.bindAndGetProxySocketAddress(playerNumber, OTHER_UID_1);

    assertThat(instance.proxySocketsByPlayerNumber.values(), hasSize(1));
    assertThat(instance.proxySocketsByPlayerNumber.containsKey(playerNumber), is(true));

    fafProxyConnectedLatch.await(TIMEOUT, TIMEOUT_UNIT);

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 8, 7, 6, 5, 4, 3, 2, 1};
    sendFromServer(playerNumber, data);

    byte[] dataReceived = dataReceivedByGame.poll(TIMEOUT, TIMEOUT_UNIT);
    assertArrayEquals(data, dataReceived);

    data = new byte[]{9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    sendFromGame(playerNumber, data);

    ProxyPackage proxyPackage = packagesReceivedByFafProxyServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(proxyPackage.length, is(data.length + 13));
    assertThat(proxyPackage.dataLength, is(data.length));
    assertThat(proxyPackage.playerNumber, is(playerNumber));
    assertThat(proxyPackage.uid, is(OTHER_UID_1));
  }

  @Test
  public void testIsReconnectionSequenceYes() throws Exception {
    assertTrue(ProxyImpl.isReconnectionSequence(new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}));
  }

  @Test(expected = NullPointerException.class)
  public void testIsReconnectionSequenceNullThrowsException() throws Exception {
    ProxyImpl.isReconnectionSequence(null);
  }

  @Test
  public void testIsReconnectionSequenceNoButSameLength() throws Exception {
    assertFalse(ProxyImpl.isReconnectionSequence(new byte[]{0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}));
  }

  @Test
  public void testIsReconnectionSequenceNoShorterLength() throws Exception {
    assertFalse(ProxyImpl.isReconnectionSequence(new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}));
  }

  /**
   * Sends the specified data to the local proxy server as if it was sent by FA.
   */
  private void sendFromGame(int playerNumber, byte[] data) throws IOException {
    DatagramSocket localProxySocket = instance.proxySocketsByPlayerNumber.get(playerNumber);
    DatagramPacket datagramPacket = new DatagramPacket(data, data.length, localProxySocket.getLocalSocketAddress());

    gameDatagramSocket.send(datagramPacket);
  }

  /**
   * Sends the specified data to the local proxy server as if it was sent by the FAF proxy server.
   */
  private void sendFromServer(int playerNumber, byte[] data) throws IOException {
    // Should be the block size, but is ignored anyway
    serverToLocalOutputStream.writeInt(1234);
    serverToLocalOutputStream.writeShort(playerNumber);
    serverToLocalOutputStream.writeQByteArray(data);
    serverToLocalOutputStream.flush();
  }

  private void startFakeFafProxyServer() throws IOException {
    packagesReceivedByFafProxyServer = new ArrayBlockingQueue<>(10);
    fafProxyConnectedLatch = new CountDownLatch(1);
    fakeFafProxyTerminatedLatch = new CountDownLatch(1);

    fafProxyServerSocket = new ServerSocket(0);

    WaitForAsyncUtils.async(() -> {
      try (Socket socket = fafProxyServerSocket.accept()) {
        this.gameToLocalProxySocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToLocalOutputStream = new QDataOutputStream(new DataOutputStream(socket.getOutputStream()));

        fafProxyConnectedLatch.countDown();

        qDataInputStream.skipBlockSize();
        int uidOfConnectedPlayer = qDataInputStream.readShort();

        while (!stopped) {
          // Number of bytes for port, uid and QByteArray (prefix stuff plus data length)
          int length = qDataInputStream.readInt();
          int playerNumber = qDataInputStream.readShort();
          int uidOfTargetPlayer = qDataInputStream.readShort();

          byte[] buffer = new byte[1024];
          int dataLength = qDataInputStream.readQByteArray(buffer);

          packagesReceivedByFafProxyServer.add(new ProxyPackage(length, playerNumber, uidOfTargetPlayer, dataLength));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        fakeFafProxyTerminatedLatch.countDown();
      }
    });
  }

  private void startFakeGameProcess() throws IOException {
    dataReceivedByGame = new ArrayBlockingQueue<>(10);
    fakeGameTerminatedLatch = new CountDownLatch(1);

    WaitForAsyncUtils.async(() -> {
      byte[] datagramBuffer = new byte[1024];
      DatagramPacket datagramPacket = new DatagramPacket(datagramBuffer, datagramBuffer.length);

      try (DatagramSocket datagramSocket = new DatagramSocket(GAME_PORT)) {
        this.gameDatagramSocket = datagramSocket;
        while (!stopped) {
          datagramSocket.receive(datagramPacket);

          dataReceivedByGame.add(Arrays.copyOfRange(datagramBuffer, datagramPacket.getOffset(), datagramPacket.getLength()));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        fakeGameTerminatedLatch.countDown();
      }
    });
  }
}
