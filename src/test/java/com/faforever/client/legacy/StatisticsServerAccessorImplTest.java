package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.relay.LobbyAction;
import com.faforever.client.legacy.relay.RelayServerActionDeserializer;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsObject;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Callback;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.core.env.Environment;
import org.testfx.util.WaitForAsyncUtils;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StatisticsServerAccessorImplTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 100000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();
  private StatisticsServerAccessorImpl instance;

  @Mock
  private Environment environment;
  private ServerSocket fafStatisticsServerSocket;
  private Socket localToServerSocket;
  private boolean stopped;
  private BlockingQueue<ClientMessage> messagesReceivedByFafServer;
  private ServerWriter serverToClientWriter;
  private CountDownLatch serverWriterReadyLatch;

  @Before
  public void setUp() throws Exception {
    messagesReceivedByFafServer = new ArrayBlockingQueue<>(10);
    serverWriterReadyLatch = new CountDownLatch(1);

    instance = new StatisticsServerAccessorImpl();
    instance.environment = environment;

    startFakeFafStatisticsServer();

    when(environment.getProperty("stats.host")).thenReturn(LOOPBACK_ADDRESS.getHostAddress());
    when(environment.getProperty("stats.port", int.class)).thenReturn(fafStatisticsServerSocket.getLocalPort());
  }

  private void startFakeFafStatisticsServer() throws IOException {
    fafStatisticsServerSocket = new ServerSocket(0);
    System.out.println("Fake server listening on " + fafStatisticsServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(ServerMessageType.class, new ServerMessageTypeTypeAdapter())
          .registerTypeAdapter(ClientMessageType.class, new ClientMessageTypeTypeAdapter())
          .registerTypeAdapter(LobbyAction.class, new RelayServerActionDeserializer())
          .create();

      try (Socket socket = fafStatisticsServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToClientWriter = new ServerWriter(socket.getOutputStream());
        serverToClientWriter.registerMessageSerializer(new ServerMessageSerializer(), ServerMessage.class);

        serverWriterReadyLatch.countDown();

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
  public void tearDown() throws Exception {
    IOUtils.closeQuietly(fafStatisticsServerSocket);
    IOUtils.closeQuietly(localToServerSocket);
  }

  @Test
  public void testRequestPlayerStatistics() throws Exception {
    @SuppressWarnings("unchecked")
    Callback<PlayerStatistics> callback = mock(Callback.class);

    String username = "junit";
    instance.requestPlayerStatistics(username, callback, StatisticsType.GLOBAL_90_DAYS);

    PlayerStatistics playerStatistics = new PlayerStatistics();
    playerStatistics.setPlayer(username);
    playerStatistics.setStatisticsType(StatisticsType.GLOBAL_90_DAYS);
    playerStatistics.setValues(Arrays.asList(new RatingInfo(), new RatingInfo()));

    sendFromServer(playerStatistics);

    ClientMessage clientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);

    assertThat(clientMessage.getCommand(), is(ClientMessageType.STATISTICS));
    assertThat(clientMessage.getAction(), nullValue());

    ArgumentCaptor<PlayerStatistics> captor = ArgumentCaptor.forClass(PlayerStatistics.class);
    verify(callback).success(captor.capture());

    assertThat(captor.getValue().getValues(), hasSize(2));
    assertThat(captor.getValue().getStatisticsType(), is(StatisticsType.GLOBAL_90_DAYS));
    assertThat(captor.getValue().getPlayer(), is(username));
    assertThat(captor.getValue().getServerMessageType(), is(ServerMessageType.STATS));
  }

  /**
   * Writes the specified message to the local relay server as if it was sent by the FAF server.
   */
  private void sendFromServer(StatisticsObject statisticsObject) throws InterruptedException {
    serverWriterReadyLatch.await();
    serverToClientWriter.write(statisticsObject);
  }
}
