package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.AskPlayerStatsMessage;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.domain.ServerObject;
import com.faforever.client.legacy.domain.ServerObjectType;
import com.faforever.client.legacy.domain.StatisticsType;
import com.faforever.client.legacy.gson.LocalDateDeserializer;
import com.faforever.client.legacy.gson.LocalTimeDeserializer;
import com.faforever.client.legacy.gson.StatisticsTypeTypeAdapter;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.stats.PlayerStatistics;
import com.faforever.client.stats.StatisticsObject;
import com.faforever.client.util.Callback;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;

import static com.faforever.client.legacy.domain.ServerObjectType.STATS;
import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class StatisticsServerAccessorImpl extends AbstractServerAccessor implements StatisticsServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  private final Gson gson;
  private OnPlayerStatsListener onPlayerStatsListener;
  private Callback<PlayerStatistics> playerStatisticsCallback;
  private Task<Void> connectionTask;
  private ServerWriter serverWriter;

  public StatisticsServerAccessorImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(StatisticsType.class, new StatisticsTypeTypeAdapter())
        .registerTypeAdapter(LocalDate.class, new LocalDateDeserializer())
        .registerTypeAdapter(LocalTime.class, new LocalTimeDeserializer())
        .create();
  }

  @Override
  public void requestPlayerStatistics(String username, Callback<PlayerStatistics> callback) {
    // FIXME this is not safe (as well aren't similar implementations in other accessors)
    playerStatisticsCallback = callback;
    writeToServer(new AskPlayerStatsMessage(username));
  }

  private void onPlayerStats(PlayerStatistics playerStatistics) {
    Platform.runLater(() -> {
      if (playerStatisticsCallback != null) {
        playerStatisticsCallback.success(playerStatistics);
        playerStatisticsCallback = null;
      }
    });
  }

  private void dispatchStatisticsObject(String jsonString, StatisticsObject statisticsObject) {
    switch (statisticsObject.type) {
      case LEAGUE_TABLE:
        // TODO remove it it's never going to be implemented
        logger.warn("league table is not yet implemented");
        break;

      case GLOBAL_90_DAYS:
        PlayerStatistics playerStatistics = gson.fromJson(jsonString, PlayerStatistics.class);
        onPlayerStats(playerStatistics);
        break;

      default:
        logger.warn("Unhandled statistics object of type: {}", statisticsObject.type);
    }
  }

  @Override
  public void onServerMessage(String message) {
    ServerMessageType serverMessageType = ServerMessageType.fromString(message);
    if (serverMessageType != null) {
      throw new IllegalStateException("Didn't expect an unknown server message from the statistics server");
    }

    try {
      ServerObject serverObject = gson.fromJson(message, ServerObject.class);

      ServerObjectType serverObjectType = ServerObjectType.fromString(serverObject.command);

      if (serverObjectType != STATS) {
        throw new IllegalStateException("Unexpected object type: " + serverObjectType);
      }

      StatisticsObject statisticsObject = gson.fromJson(message, StatisticsObject.class);
      dispatchStatisticsObject(message, statisticsObject);
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + message, e);
    }
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
        try {
          if (serverSocket != null) {
            serverWriter.close();
            serverSocket.close();
          }
          logger.debug("Closed connection to statistics server");
        } catch (IOException e) {
          logger.warn("Could not close statistics socket", e);
        }
      }
    };
    executeInBackground(connectionTask);
  }

  protected ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerObjectWriter(new ClientMessageSerializer(), ClientMessage.class);
    serverWriter.registerObjectWriter(new StringSerializer(), String.class);
    return serverWriter;
  }
}
