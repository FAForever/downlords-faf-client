package com.faforever.client.update;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.io.CharStreams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
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
import static org.junit.Assert.assertThat;

@Slf4j
public class CheckForUpdateTaskTest extends AbstractPlainJavaFxTest {

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  private ServerSocket fakeConfigServerSocket;
  private CheckForUpdateTask instance;

  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;

  private CountDownLatch terminateLatch;
  private ClientProperties clientProperties;

  @Before
  public void setUp() throws Exception {
    clientProperties = new ClientProperties();
    instance = new CheckForUpdateTask(i18n, preferencesService);

    terminateLatch = new CountDownLatch(1);
  }

  @After
  public void tearDown() {
    IOUtils.closeQuietly(fakeConfigServerSocket);
    terminateLatch.countDown();
  }

  @Test
  @Ignore("For unknown reasons, Travis throws a SocketException probably when trying to connect to the fake server")
  public void testIsNewer() throws Exception {
    startFakeConfigServer();
    int port = fakeConfigServerSocket.getLocalPort();

    clientProperties.setClientConfigUrl("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);

    UpdateInfo updateInfo = instance.call();

    assertThat(updateInfo.getSize(), is(123));
    assertThat(updateInfo.getFileName(), is("dfaf_windows_0_4_7-alpha.exe"));
    assertThat(updateInfo.getName(), is("0.4.7-alpha"));
    assertThat(updateInfo.getReleaseNotesUrl(), is(new URL("https://www.example.com/")));

    if (org.bridj.Platform.isWindows()) {
      assertThat(updateInfo.getUrl(), is(new URL("https://github.com/faforever/downlords-faf-client/releases/download/v0.4.7-alpha/dfaf_windows_0_4_7-alpha.exe")));
    } else if (org.bridj.Platform.isLinux()) {
      assertThat(updateInfo.getUrl(), is(new URL("https://github.com/faforever/downlords-faf-client/releases/download/v0.4.7-alpha/dfaf_linux_0_4_7-alpha.tar.gz")));
    } else if (org.bridj.Platform.isMacOSX()) {
      assertThat(updateInfo.getUrl(), is(new URL("https://github.com/faforever/downlords-faf-client/releases/download/v0.4.7-alpha/dfaf_mac_0_4_7-alpha.dmg")));
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
  @Ignore("For unknown reasons, Travis throws a SocketException probably when trying to connect to the fake server")
  public void testGetUpdateIsCurrent() throws Exception {
    startFakeConfigServer();
    int port = fakeConfigServerSocket.getLocalPort();
    clientProperties.setClientConfigUrl("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);

    assertThat(instance.call(), is(nullValue()));
  }
}
