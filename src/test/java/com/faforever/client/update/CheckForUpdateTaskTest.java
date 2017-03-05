package com.faforever.client.update;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.io.CharStreams;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.util.WaitForAsyncUtils;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class CheckForUpdateTaskTest extends AbstractPlainJavaFxTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  private ServerSocket fakeGithubServerSocket;
  private CheckForUpdateTask instance;

  @Mock
  private I18n i18n;

  private CountDownLatch terminateLatch;

  @Before
  public void setUp() throws Exception {
    instance = new CheckForUpdateTask();
    instance.i18n = i18n;
    instance.clientProperties = new ClientProperties();

    terminateLatch = new CountDownLatch(1);
  }

  @After
  public void tearDown() throws Exception {
    IOUtils.closeQuietly(fakeGithubServerSocket);
    terminateLatch.countDown();
  }

  @Test
  @Ignore("Still not sure why this fails on travis with SocketException")
  public void testIsNewer() throws Exception {
    instance.setCurrentVersion(new ComparableVersion("0.4.8-alpha"));

    startFakeGitHubApiServer();
    int port = fakeGithubServerSocket.getLocalPort();

    instance.clientProperties.getGitHub().setReleasesUrl("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);
    instance.clientProperties.getGitHub().setTimeout(3000);

    instance.call();
  }

  private void startFakeGitHubApiServer() throws Exception {
    fakeGithubServerSocket = new ServerSocket(0);

    WaitForAsyncUtils.async(() -> {
      try (Socket socket = fakeGithubServerSocket.accept();
           Reader sampleReader = new InputStreamReader(getClass().getResourceAsStream("/sample-github-releases-response.txt"));
           OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {

        logger.debug("Accepted connection from {}", socket.getInetAddress());

        String response = CharStreams.toString(sampleReader);

        outputStreamWriter.write(response);
      } catch (Exception e) {
        logger.error("Exception in fake HTTP server", e);
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * There is no newer version on the server.
   */
  @Test
  @Ignore("Still not sure why this fails on travis with SocketException")
  public void testGetUpdateIsCurrent() throws Exception {
    instance.setCurrentVersion(new ComparableVersion("0.4.8.1-alpha"));

    startFakeGitHubApiServer();
    int port = fakeGithubServerSocket.getLocalPort();
    instance.clientProperties.getGitHub().setReleasesUrl("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);
    instance.clientProperties.getGitHub().setTimeout(3000);

    instance.call();
  }
}
