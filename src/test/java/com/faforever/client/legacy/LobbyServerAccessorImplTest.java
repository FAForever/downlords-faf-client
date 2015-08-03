package com.faforever.client.legacy;

import com.faforever.client.game.Faction;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.InitSessionMessage;
import com.faforever.client.legacy.domain.ServerObject;
import com.faforever.client.legacy.io.QDataReader;
import com.faforever.client.legacy.relay.LobbyAction;
import com.faforever.client.legacy.relay.RelayServerActionDeserializer;
import com.faforever.client.legacy.relay.RelayServerMessageSerializer;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.LoginPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.rankedmatch.Accept1v1MatchMessage;
import com.faforever.client.rankedmatch.MatchMakerMessage;
import com.faforever.client.rankedmatch.RankedMatchNotification;
import com.faforever.client.rankedmatch.SearchRanked1v1Message;
import com.faforever.client.rankedmatch.StopSearchRanked1v1Message;
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
  public static final int GAME_PORT = 6112;

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
    ForgedAlliancePrefs forgedAlliancePrefs = mock(ForgedAlliancePrefs.class);

    when(instance.preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(preferences.getLogin()).thenReturn(loginPrefs);
    when(forgedAlliancePrefs.getPort()).thenReturn(GAME_PORT);
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
    assertThat(clientMessage, instanceOf(Accept1v1MatchMessage.class));
    assertEquals(((Accept1v1MatchMessage) clientMessage).factionchosen, Faction.AEON);
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

  @Test
  public void startSearchRanked1v1WithAeon() throws Exception {
    ClientMessage clientMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(clientMessage, instanceOf(InitSessionMessage.class));

    instance.startSearchRanked1v1(Faction.AEON, null);

    clientMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(clientMessage, instanceOf(StopSearchRanked1v1Message.class));

    clientMessage = messagesReceivedByFafServer.poll(RECEIVE_TIMEOUT, RECEIVE_TIMEOUT_UNIT);
    assertThat(clientMessage, instanceOf(SearchRanked1v1Message.class));
    assertEquals(((SearchRanked1v1Message) clientMessage).faction, "/aeon");
    assertEquals(((SearchRanked1v1Message) clientMessage).gameport, GAME_PORT);
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

          ClientMessageType clientMessageType = ClientMessageType.fromString(clientMessage.command);

          dispatchClientMessage(clientMessageType, json, gson);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void dispatchClientMessage(ClientMessageType clientMessageType, String json, Gson gson) {
    switch (clientMessageType) {
      case ASK_SESSION:
        InitSessionMessage initSessionMessage = gson.fromJson(json, InitSessionMessage.class);
        messagesReceivedByFafServer.add(initSessionMessage);
        break;

      case GAME_MATCH_MAKING:
        MatchMakerMessage matchMakerMessage = gson.fromJson(json, MatchMakerMessage.class);
        if (matchMakerMessage.mod.equals("matchmaker")) {
          if (matchMakerMessage.state.equals("askingtostop")) {
            matchMakerMessage = gson.fromJson(json, StopSearchRanked1v1Message.class);
          } else if (matchMakerMessage.state.equals("faction")) {
            matchMakerMessage = gson.fromJson(json, Accept1v1MatchMessage.class);
          }
        } else if (matchMakerMessage.state.equals("start")) {
          matchMakerMessage = gson.fromJson(json, SearchRanked1v1Message.class);
        }
        messagesReceivedByFafServer.add(matchMakerMessage);

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
