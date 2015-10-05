package com.faforever.client.update;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.io.CharStreams;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static org.mockito.Mockito.when;

public class CheckForUpdateTaskTest extends AbstractPlainJavaFxTest {

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
    startFakeGitHubApiServer();

    int port = fafLobbyServerSocket.getLocalPort();
    when(environment.getProperty("github.releases.url")).thenReturn("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);
    when(environment.getProperty("github.releases.timeout", int.class)).thenReturn(100);

    instance.call();
  }

  private void startFakeGitHubApiServer() throws IOException {
    fafLobbyServerSocket = new ServerSocket(0);

    WaitForAsyncUtils.async(() -> {
      try (Socket socket = fafLobbyServerSocket.accept();
           Reader sampleReader = new InputStreamReader(getClass().getResourceAsStream("/sample-github-releases-response.txt"));
           OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream())) {

        String response = CharStreams.toString(sampleReader);

        outputStreamWriter.write(response);
        outputStreamWriter.flush();

        Thread.sleep(500);
      } catch (InterruptedException | IOException e) {
        System.out.println("Closing fake GitHub HTTP server: " + e.getMessage());
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
    startFakeGitHubApiServer();

    int port = fafLobbyServerSocket.getLocalPort();
    when(environment.getProperty("github.releases.url")).thenReturn("http://" + LOOPBACK_ADDRESS.getHostAddress() + ":" + port);
    when(environment.getProperty("github.releases.timeout", int.class)).thenReturn(100);

    instance.call();
  }
}
