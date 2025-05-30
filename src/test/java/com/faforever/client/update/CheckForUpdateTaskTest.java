package com.faforever.client.update;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.util.FileSizeReader;
import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
public class CheckForUpdateTaskTest extends ServiceTest {

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  private ServerSocket fakeConfigServerSocket;
  @InjectMocks
  private CheckForUpdateTask instance;

  @Mock
  private I18n i18n;

  @Mock
  private FileSizeReader fileSizeReader;

  private CountDownLatch terminateLatch;
  @Spy
  private ClientProperties clientProperties;

  @BeforeEach
  public void setUp() throws Exception {
    terminateLatch = new CountDownLatch(1);
  }

  @AfterEach
  public void tearDown() {
    IOUtils.closeQuietly(fakeConfigServerSocket);
    terminateLatch.countDown();
  }

  @Test
  @Disabled("For unknown reasons, Travis throws a SocketException probably when trying to connect to the fake server")
  public void testIsNewer() throws Exception {
    startFakeConfigServer();
    int port = fakeConfigServerSocket.getLocalPort();

    clientProperties.setClientConfigUrl("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);

    UpdateInfo updateInfo = instance.call();

    assertThat(updateInfo.size(), is(123));
    assertThat(updateInfo.fileName(), is("dfaf_windows_0_4_7-alpha.exe"));
    assertThat(updateInfo.name(), is("0.4.7-alpha"));
    assertThat(updateInfo.releaseNotesUrl(), is(new URL("https://www.example.com/")));

    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(updateInfo.url(), is(new URL(
          "https://github.com/faforever/downlords-faf-client/releases/download/v0.4.7-alpha/dfaf_windows_0_4_7-alpha.exe")));
    } else if (SystemUtils.IS_OS_LINUX) {
      assertThat(updateInfo.url(), is(new URL(
          "https://github.com/faforever/downlords-faf-client/releases/download/v0.4.7-alpha/dfaf_linux_0_4_7-alpha.tar.gz")));
    } else if (SystemUtils.IS_OS_MAC_OSX) {
      assertThat(updateInfo.url(), is(new URL(
          "https://github.com/faforever/downlords-faf-client/releases/download/v0.4.7-alpha/dfaf_mac_0_4_7-alpha.dmg")));
    } else {
      throw new IllegalStateException("Unsupported platform");
    }

  }

  private void startFakeConfigServer() throws Exception {
    fakeConfigServerSocket = new ServerSocket(0);

    WaitForAsyncUtils.async(() -> {
      try (Socket socket = fakeConfigServerSocket.accept();
           Reader sampleReader = new InputStreamReader(getClass().getResourceAsStream("/sample-github-releases-response.txt"));
           OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {

        log.debug("Accepted connection from {}", socket.getInetAddress());

        CharStreams.copy(sampleReader, outputStreamWriter);
      } catch (Exception e) {
        log.error("Exception in fake HTTP server", e);
        throw new RuntimeException(e);
      }
      log.info("Response sent");
    });
  }

  /**
   * There is no newer version on the server.
   */
  @Test
  @Disabled("For unknown reasons, Travis throws a SocketException probably when trying to connect to the fake server")
  public void testGetUpdateIsCurrent() throws Exception {
    startFakeConfigServer();
    int port = fakeConfigServerSocket.getLocalPort();
    clientProperties.setClientConfigUrl("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);

    assertThat(instance.call(), is(nullValue()));
  }
}
