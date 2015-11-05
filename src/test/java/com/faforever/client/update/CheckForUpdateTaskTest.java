package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.io.CharStreams;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static org.mockito.Mockito.when;

public class CheckForUpdateTaskTest extends AbstractPlainJavaFxTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  private ServerSocket fafLobbyServerSocket;
  private CheckForUpdateTask instance;

  @Mock
  private Environment environment;

  @Mock
  private I18n i18n;

  @Before
  public void setUp() throws Exception {
    instance = new CheckForUpdateTask();
    instance.i18n = i18n;
    instance.environment = environment;
  }

  @After
  public void tearDown() throws Exception {
    if (fafLobbyServerSocket != null) {
      fafLobbyServerSocket.close();
    }
  }

  /**
   * Never version is available on server.
   */
  @Test
  public void testIsNewer() throws Exception {
    instance.setCurrentVersion(new ComparableVersion("0.4.8-alpha"));

    CountDownLatch exitLatch = new CountDownLatch(1);
    Future<Void> serverFuture = startFakeGitHubApiServer(exitLatch);
    int port = fafLobbyServerSocket.getLocalPort();
    when(environment.getProperty("github.releases.url")).thenReturn("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);
    when(environment.getProperty("github.releases.timeout", int.class)).thenReturn(3000);

    instance.call();
    exitLatch.countDown();
    serverFuture.get();
  }

  private Future<Void> startFakeGitHubApiServer(CountDownLatch exitLatch) throws Exception {
    fafLobbyServerSocket = new ServerSocket(0);

    return WaitForAsyncUtils.async(() -> {
      try (Socket socket = fafLobbyServerSocket.accept();
           Reader sampleReader = new InputStreamReader(getClass().getResourceAsStream("/sample-github-releases-response.txt"));
           OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {

        String response = CharStreams.toString(sampleReader);

        outputStreamWriter.write(response);
        outputStreamWriter.close();
        exitLatch.await();
      } catch (InterruptedException | IOException e) {
        logger.error("Exception in fake HTTP server", e);
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * There is no newer version on the server.
   */
  @Test
  public void testGetUpdateIsCurrent() throws Exception {
    instance.setCurrentVersion(new ComparableVersion("0.4.8.1-alpha"));

    CountDownLatch exitLatch = new CountDownLatch(1);
    Future<Void> serverFuture = startFakeGitHubApiServer(exitLatch);
    int port = fafLobbyServerSocket.getLocalPort();
    when(environment.getProperty("github.releases.url")).thenReturn("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);
    when(environment.getProperty("github.releases.timeout", int.class)).thenReturn(3000);

    instance.call();
    exitLatch.countDown();
    serverFuture.get();
  }
}
