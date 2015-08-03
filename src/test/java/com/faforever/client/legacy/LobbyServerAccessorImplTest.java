package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientObjectType;
import com.faforever.client.legacy.domain.InitSessionMessage;
import com.faforever.client.legacy.domain.ServerObject;
import com.faforever.client.legacy.io.QDataReader;
import com.faforever.client.legacy.relay.LobbyAction;
import com.faforever.client.legacy.relay.RelayServerActionDeserializer;
import com.faforever.client.legacy.relay.RelayServerMessageSerializer;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.Accept1v1Match;
import com.faforever.client.rankedmatch.RankedMatchNotification;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LobbyServerAccessorImplTest extends AbstractPlainJavaFxTest {

  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

  private static final int RECEIVE_TIMEOUT = 1000000;
  private static final TimeUnit RECEIVE_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  private LobbyServerAccessorImpl instance;
  private int lobbyServerPort;
  private ServerSocket lobbyServerSocket;
  private boolean stopped;
  private BlockingQueue<ClientMessage> messagesReceivedByFafServer;
  private ServerWriter serverToClientWriter;
  private CountDownLatch clientConnectedLatch;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(100);

    instance = new LobbyServerAccessorImpl();
    instance.environment = mock(Environment.class);
    instance.preferencesService = mock(PreferencesService.class);
    Preferences preferences = mock(Preferences.class);
    LoginPrefs loginPrefs = mock(LoginPrefs.class);

    when(instance.preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getLogin()).thenReturn(loginPrefs);
    when(loginPrefs.getUsername()).thenReturn("junit");
    when(loginPrefs.getPassword()).thenReturn("password");

    clientConnectedLatch = new CountDownLatch(1);
    instance.setOnLobbyConnectedListener(clientConnectedLatch::countDown);

    startFakeLobbyServer();

    when(instance.environment.getProperty("lobby.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(instance.environment.getProperty("lobby.port", int.class)).thenReturn(lobbyServerPort);

    instance.connectAndLogInInBackground(null);
    clientConnectedLatch.await();
  }

  @Test
  public void testAccept1v1Match() throws Exception {
    ClientMessage clientMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(clientMessage, instanceOf(InitSessionMessage.class));

    instance.accept1v1Match(Faction.AEON);

    clientMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(clientMessage, instanceOf(Accept1v1Match.class));
    assertEquals(((Accept1v1Match) clientMessage).factionchosen, Faction.AEON);
  }

  @Test
  public void testRankedMatchNotification() throws Exception {
    RankedMatchNotification message = new RankedMatchNotification(true);

    CompletableFuture<RankedMatchNotification> serviceStateDoneFuture = new CompletableFuture<>();

    WaitForAsyncUtils.waitForAsyncFx(200, () -> instance.addOnRankedMatchNotificationListener(
        serviceStateDoneFuture::complete
    ));

    sendFromServer(message);

    RankedMatchNotification rankedMatchNotification = serviceStateDoneFuture.get();

    assertThat(rankedMatchNotification.potential, is(true));
  }

  private void startFakeLobbyServer() throws IOException {
    lobbyServerSocket = new ServerSocket(0);
    lobbyServerPort = lobbyServerSocket.getLocalPort();

    WaitForAsyncUtils.async(() -> {
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(LobbyAction.class, new RelayServerActionDeserializer())
          .registerTypeAdapter(Faction.class, new FactionDeserializer())
          .create();

      try (Socket socket = lobbyServerSocket.accept()) {
        QDataReader qDataReader = new QDataReader(new DataInputStream(socket.getInputStream()));
        serverToClientWriter = new ServerWriter(socket.getOutputStream());
        serverToClientWriter.registerMessageSerializer(new RelayServerMessageSerializer(), ServerObject.class);

        while (!stopped) {
          qDataReader.skipBlockSize();
          String json = qDataReader.readQString();
          String username = qDataReader.readQString();
          String sessionId = qDataReader.readQString();

          ClientMessage clientMessage = gson.fromJson(json, ClientMessage.class);

          ClientObjectType clientObjectType = ClientObjectType.fromString(clientMessage.command);

          dispatchClientMessage(clientObjectType, json, gson);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void dispatchClientMessage(ClientObjectType clientObjectType, String json, Gson gson) {
    switch (clientObjectType) {
      case ASK_SESSION:
        InitSessionMessage initSessionMessage = gson.fromJson(json, InitSessionMessage.class);
        messagesReceivedByFafServer.add(initSessionMessage);
        break;

      case ACCEPT_1V1_MATCH:
        Accept1v1Match accept1v1Match = gson.fromJson(json, Accept1v1Match.class);
        messagesReceivedByFafServer.add(accept1v1Match);
        break;
    }
  }

  /**
   * Sends the specified message to the local client as if it was sent by the FAF server.
   */
  private void sendFromServer(ServerObject message) throws IOException {
    serverToClientWriter.write(message);
  }
}
