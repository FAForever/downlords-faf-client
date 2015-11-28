package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ClientMessageType;
import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.gson.ClientMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.stats.PlayerStatisticsMessageLobby;
import com.faforever.client.stats.RatingInfo;
import com.faforever.client.stats.StatisticsMessageLobby;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class StatisticsServerAccessorImplTest extends AbstractPlainJavaFxTest {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
    logger.info("Fake statistics server listening on " + fafStatisticsServerSocket.getLocalPort());

    WaitForAsyncUtils.async(() -> {
      Gson gson = new GsonBuilder()
          .registerTypeAdapter(FafServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
          .registerTypeAdapter(ClientMessageType.class, ClientMessageTypeTypeAdapter.INSTANCE)
          .create();

      try (Socket socket = fafStatisticsServerSocket.accept()) {
        localToServerSocket = socket;
        QDataInputStream qDataInputStream = new QDataInputStream(new DataInputStream(socket.getInputStream()));
        serverToClientWriter = new ServerWriter(socket.getOutputStream());
        serverToClientWriter.registerMessageSerializer(new ServerMessageSerializer(), FafServerMessage.class);

        serverWriterReadyLatch.countDown();

        while (!stopped) {
          int blockSize = qDataInputStream.readInt();
          String json = qDataInputStream.readQString();

          if (blockSize > json.length() * 2) {
            // Username
            qDataInputStream.readQString();
            // Session ID
            qDataInputStream.readQString();
          }

          ClientMessage clientMessage = gson.fromJson(json, ClientMessage.class);

          messagesReceivedByFafServer.add(clientMessage);
        }
      } catch (IOException e) {
        logger.info("Closing fake FAF lobby server: " + e.getMessage());
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
    String username = "junit";
    CompletableFuture<PlayerStatisticsMessageLobby> future = instance.requestPlayerStatistics(StatisticsType.GLOBAL_90_DAYS, username);

    ClientMessage clientMessage = messagesReceivedByFafServer.poll(TIMEOUT, TIMEOUT_UNIT);
    assertThat(clientMessage.getCommand(), is(ClientMessageType.STATISTICS));

    PlayerStatisticsMessageLobby playerStatisticsMessage = new PlayerStatisticsMessageLobby();
    playerStatisticsMessage.setPlayer(username);
    playerStatisticsMessage.setStatisticsType(StatisticsType.GLOBAL_90_DAYS);
    playerStatisticsMessage.setValues(Arrays.asList(new RatingInfo(), new RatingInfo()));
    sendFromServer(playerStatisticsMessage);

    PlayerStatisticsMessageLobby result = future.get(TIMEOUT, TIMEOUT_UNIT);

    assertThat(result.getValues(), hasSize(2));
    assertThat(result.getStatisticsType(), is(StatisticsType.GLOBAL_90_DAYS));
    assertThat(result.getPlayer(), is(username));
    assertThat(result.getMessageType(), is(FafServerMessageType.STATS));
  }

  /**
   * Writes the specified message to the client if it was sent by the FAF server.
   */
  private void sendFromServer(StatisticsMessageLobby statisticsMessage) throws InterruptedException {
    serverWriterReadyLatch.await();
    serverToClientWriter.write(statisticsMessage);
  }
}
