package com.faforever.client.connectivity;

import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.io.QDataOutputStream;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TurnClientImplTest extends AbstractPlainJavaFxTest {

  private class ProxyPackage {

    final int length;
    final int playerNumber;
    final int uid;
    final int dataLength;

    ProxyPackage(int length, int playerNumber, int uid, int dataLength) {
      this.length = length;
      this.playerNumber = playerNumber;
      this.uid = uid;
      this.dataLength = dataLength;
    }
  }
  public static final int TIMEOUT = 1000;
  public static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private static final int OTHER_UID_1 = 111;
  private static final int GAME_PORT = 61112;

  private TurnClientImpl instance;
  private ServerSocket fafProxyServerSocket;
  private QDataOutputStream serverToLocalOutputStream;
  private boolean stopped;
  private BlockingQueue<ProxyPackage> packagesReceivedByFafProxyServer;
  private BlockingQueue<byte[]> dataReceivedByGame;
  private Socket gameToLocalProxySocket;
  private CountDownLatch fafProxyConnectedLatch;
  private CountDownLatch fakeGameTerminatedLatch;
  private CountDownLatch fakeFafProxyTerminatedLatch;
  private DatagramSocket gameDatagramSocket;

  @Before
  public void setUp() throws Exception {
    instance = new TurnClientImpl();
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

  private void startFakeGameProcess() {
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
