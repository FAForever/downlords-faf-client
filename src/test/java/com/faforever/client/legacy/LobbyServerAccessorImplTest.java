package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.SessionInfo;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.relay.LobbyAction;
import com.faforever.client.legacy.relay.RelayServerActionDeserializer;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Callback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LobbyServerAccessorImplTest extends AbstractPlainJavaFxTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final long TIMEOUT = 100000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  @Mock
  PreferencesService preferencesService;
  @Mock
  Preferences preferences;
  @Mock
  Environment environment;
  private LobbyServerAccessorImpl instance;
  private LoginPrefs loginPrefs;
  private ServerSocket fafLobbyServerSocket;
  private Socket localToServerSocket;
  private ServerWriter serverToClientWriter;
  private boolean stopped;
  private BlockingQueue<ClientMessage> messagesReceivedByFafServer;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    startFakeFafLobbyServer();

    instance = new LobbyServerAccessorImpl();
    instance.preferencesService = preferencesService;
    instance.environment = environment;

    loginPrefs = new LoginPrefs();
    loginPrefs.setUsername("junit");
    loginPrefs.setPassword("password");

    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getLogin()).thenReturn(loginPrefs);
    when(environment.getProperty("lobby.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(environment.getProperty("lobby.port", int.class)).thenReturn(fafLobbyServerSocket.getLocalPort());

    preferencesService.getPreferences().getLogin();
  }

  private void startFakeFafLobbyServer() throws IOException {
    fafLobbyServerSocket = new ServerSocket(0);
    logger.info("Fake server listening on " + fafLobbyServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(ClientMessageType.class, new ClientMessageTypeTypeAdapter())
          .registerTypeAdapter(LobbyAction.class, new RelayServerActionDeserializer())
          .create();

      try (Socket socket = fafLobbyServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToClientWriter = new ServerWriter(socket.getOutputStream());
        serverToClientWriter.registerMessageSerializer(new ClientMessageSerializer(), ServerMessage.class);

        while (!stopped) {
          qDataInputStream.skipBlockSize();
          String json = qDataInputStream.readQString();

          ClientMessage clientMessage = gson.fromJson(json, ClientMessage.class);

          messagesReceivedByFafServer.add(clientMessage);
        }
      } catch (IOException e) {
        System.out.println("Closing fake FAF lobby server: " + e.getMessage());
        throw new RuntimeException(e);
      }
    });
  }

  @After
  public void tearDown() {
    IOUtils.closeQuietly(fafLobbyServerSocket);
    IOUtils.closeQuietly(localToServerSocket);
  }

  @Test
  public void testConnectAndLogInInBackground() throws Exception {
    @SuppressWarnings("unchecked")
    Callback<SessionInfo> callback = mock(Callback.class);

    instance.connectAndLogInInBackground(callback);

    ClientMessage clientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(clientMessage.getCommand(), is(ClientMessageType.ASK_SESSION));
  }
}
