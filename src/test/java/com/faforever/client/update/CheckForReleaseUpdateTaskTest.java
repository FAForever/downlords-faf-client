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
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.testfx.util.WaitForAsyncUtils;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@Slf4j
public class CheckForReleaseUpdateTaskTest extends AbstractPlainJavaFxTest {

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  private ServerSocket fakeConfigServerSocket;
  private CheckForReleaseUpdateTask instance;

  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private RestTemplateBuilder restTemplateBuilder;
  @Mock
  private RestTemplate restTemplate;

  private CountDownLatch terminateLatch;
  private ClientProperties clientProperties;

  @Before
  public void setUp() throws Exception {
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    clientProperties = new ClientProperties();
    instance = new CheckForReleaseUpdateTask(i18n, preferencesService, restTemplateBuilder);

    terminateLatch = new CountDownLatch(1);
  }

  @After
  public void tearDown() {
    IOUtils.closeQuietly(fakeConfigServerSocket);
    terminateLatch.countDown();
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
