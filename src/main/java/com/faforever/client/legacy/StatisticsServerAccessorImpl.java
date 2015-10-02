package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.AskPlayerStatsDaysMessage;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ServerCommand;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.gson.LocalDateDeserializer;
import com.faforever.client.legacy.gson.LocalTimeDeserializer;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.StatisticsMessage;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.legacy.domain.ServerMessageType.STATS;
import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class StatisticsServerAccessorImpl extends AbstractServerAccessor implements StatisticsServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;
  @Autowired
  Environment environment;
  private CompletableFuture<PlayerStatistics> playerStatisticsCallback;
  private ServerWriter serverWriter;
  private Task<Void> connectionTask;

  public StatisticsServerAccessorImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(ServerMessageType.class, new ServerMessageTypeTypeAdapter())
        .registerTypeAdapter(StatisticsType.class, new StatisticsTypeTypeAdapter())
        .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
        .registerTypeAdapter(LocalTime.class, new LocalTimeDeserializer())
        .create();
  }

  @Override
  public CompletableFuture<PlayerStatistics> requestPlayerStatistics(String username, StatisticsType type) {
    // FIXME this is not safe (as well aren't similar implementations in other accessors)
    playerStatisticsCallback = new CompletableFuture<>();

    writeToServer(new AskPlayerStatsDaysMessage(username, type));
    return playerStatisticsCallback;
  }

  private void writeToServer(ClientMessage clientMessage) {
    connectionTask = new Task<Void>() {
      Socket serverSocket;

      @Override
      protected Void call() throws Exception {
        String lobbyHost = environment.getProperty("stats.host");
        Integer lobbyPort = environment.getProperty("stats.port", int.class);

        logger.info("Trying to connect to statistics server at {}:{}", lobbyHost, lobbyPort);

        try (Socket fafServerSocket = new Socket(lobbyHost, lobbyPort);
             OutputStream outputStream = fafServerSocket.getOutputStream()) {
          this.serverSocket = fafServerSocket;

          logger.info("Statistics server connection established");

          serverWriter = createServerWriter(outputStream);
          serverWriter.write(clientMessage);

          blockingReadServer(fafServerSocket);
        } catch (IOException e) {
          logger.warn("Lost connection to statistics server", e);
        }
        return null;
      }

      @Override
      protected void cancelled() {
        IOUtils.closeQuietly(serverWriter);
        IOUtils.closeQuietly(serverSocket);
          logger.debug("Closed connection to statistics server");
      }
    };
    executeInBackground(connectionTask);
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new ClientMessageSerializer(), ClientMessage.class);
    serverWriter.registerMessageSerializer(new StringSerializer(), String.class);
    return serverWriter;
  }

  @Override
  protected void onServerMessage(String message) {
    ServerCommand serverCommand = ServerCommand.fromString(message);
    if (serverCommand != null) {
      throw new IllegalStateException("Didn't expect an unknown server message from the statistics server");
    }

    try {
      ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

      ServerMessageType serverMessageType = serverMessage.getServerMessageType();

      if (serverMessageType != STATS) {
        throw new IllegalStateException("Unexpected object type: " + serverMessageType);
      }

      StatisticsMessage statisticsMessage = gson.fromJson(message, StatisticsMessage.class);
      dispatchStatisticsObject(message, statisticsMessage);
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + message, e);
    }
  }

  private void dispatchStatisticsObject(String jsonString, StatisticsMessage statisticsMessage) {
    switch (statisticsMessage.getStatisticsType()) {
      case LEAGUE_TABLE:
        // TODO remove it it's never going to be implemented
        logger.warn("league table is not yet implemented");
        break;

      case GLOBAL_90_DAYS:
      case GLOBAL_365_DAYS:
        PlayerStatistics playerStatistics = gson.fromJson(jsonString, PlayerStatistics.class);
        onPlayerStats(playerStatistics);
        break;

      default:
        logger.warn("Unhandled statistics object of type: {}", statisticsMessage.getStatisticsType());
    }
  }

  private void onPlayerStats(PlayerStatistics playerStatistics) {
    Platform.runLater(() -> {
      if (playerStatisticsCallback != null) {
        playerStatisticsCallback.complete(playerStatistics);
        playerStatisticsCallback = null;
      }
    });
  }

  @PreDestroy
  void disconnect() {
    if (connectionTask != null) {
      connectionTask.cancel(true);
    }
  }
}
