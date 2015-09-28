package com.faforever.client.replay;

import com.faforever.client.legacy.AbstractServerAccessor;
import com.faforever.client.legacy.ClientMessageSerializer;
import com.faforever.client.legacy.StringSerializer;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ServerCommand;
import com.faforever.client.legacy.writer.ServerWriter;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class ReplayServerAccessorImpl extends AbstractServerAccessor implements ReplayServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;
  @Autowired
  Environment environment;
  private Task<Void> connectionTask;
  private ServerWriter serverWriter;
  private CompletableFuture<List<ReplayInfoBean>> replayListCallback;

  public ReplayServerAccessorImpl() {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create();
  }

  @Override
  public CompletableFuture<List<ReplayInfoBean>> requestOnlineReplays() {
    // FIXME this is not safe (as well aren't similar implementations in other accessors)
    replayListCallback = new CompletableFuture<>();
    writeToServer(new ListReplaysMessage());
    return replayListCallback;
  }

  private void writeToServer(ClientMessage clientMessage) {
    connectionTask = new Task<Void>() {
      Socket serverSocket;

      @Override
      protected Void call() throws Exception {
        String replayHost = environment.getProperty("replay.host");
        Integer replayPort = environment.getProperty("replay.port", int.class);

        logger.info("Trying to connect to replay server at {}:{}", replayHost, replayPort);

        try (Socket replayServerSocket = new Socket(replayHost, replayPort);
             OutputStream outputStream = replayServerSocket.getOutputStream()) {
          this.serverSocket = replayServerSocket;

          logger.info("Replay server connection established");

          serverWriter = createServerWriter(outputStream);
          serverWriter.write(clientMessage);

          blockingReadServer(replayServerSocket);
        } catch (IOException e) {
          logger.warn("Lost connection to replay server", e);
        }
        return null;
      }

      @Override
      protected void cancelled() {
        IOUtils.closeQuietly(serverSocket);
        IOUtils.closeQuietly(serverSocket);
        logger.debug("Closed connection to statistics server");
      }
    };
    executeInBackground(connectionTask);
  }

  protected ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new ClientMessageSerializer(), ClientMessage.class);
    serverWriter.registerMessageSerializer(new StringSerializer(), String.class);
    return serverWriter;
  }

  @Override
  public void onServerMessage(String message) {
    ServerCommand serverCommand = ServerCommand.fromString(message);
    if (serverCommand != null) {
      throw new IllegalStateException("Didn't expect an unknown server message from the statistics server");
    }

    try {
      // Who knows why, but these messages do not have a "command" like all other messages but an "action"
      ReplayServerObject replayServerObject = gson.fromJson(message, ReplayServerObject.class);

      if (replayListCallback != null) {
        replayListCallback.complete(replayInfoBeans(replayServerObject.getReplays()));
      }
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + message, e);
    }
  }

  private List<ReplayInfoBean> replayInfoBeans(List<ServerReplayInfo> replayInfos) {
    return replayInfos.stream()
        .map(ReplayInfoBean::new)
        .collect(Collectors.toList());
  }

  @PreDestroy
  void disconnect() {
    if (connectionTask != null) {
      connectionTask.cancel(true);
    }
  }
}
