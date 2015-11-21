package com.faforever.client.legacy;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.ClientMessage;
import com.faforever.client.legacy.domain.ModInfo;
import com.faforever.client.legacy.domain.ModSearchResult;
import com.faforever.client.legacy.domain.SearchModMessage;
import com.faforever.client.legacy.domain.ServerCommand;
import com.faforever.client.legacy.domain.ServerMessage;
import com.faforever.client.legacy.domain.ServerMessageType;
import com.faforever.client.legacy.gson.ServerMessageTypeTypeAdapter;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.util.ConcurrentUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import javafx.concurrent.Task;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.legacy.domain.ServerMessageType.MOD_RESULT_LIST;

public class ModsServerAccessorImpl extends AbstractServerAccessor implements ModsServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;
  @Resource
  Environment environment;
  @Resource
  NotificationService notificationService;
  @Resource
  I18n i18n;
  private ServerWriter serverWriter;
  private boolean disconnectedGracefully;
  private Socket socket;
  private CompletableFuture<List<ModInfo>> searchModFuture;

  public ModsServerAccessorImpl() {
    gson = new GsonBuilder()
        .registerTypeAdapter(ServerMessageType.class, ServerMessageTypeTypeAdapter.INSTANCE)
        .create();
  }

  @Override
  public void connect() {
    if (socket != null && !socket.isClosed()) {
      return;
    }

    disconnectedGracefully = false;

    String host = environment.getProperty("mods.host");
    int port = environment.getProperty("mods.port", int.class);

    try {
      logger.debug("Connecting to mods server {}:{}", host, port);
      this.socket = new Socket(host, port);

      logger.debug("Connection to mods server established");

      serverWriter = createServerWriter(socket.getOutputStream());

      readInBackground();
    } catch (IOException e) {
      notificationService.addNotification(new PersistentNotification(i18n.get("modVault.serverConnectionFailed"), Severity.WARN));
    }
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(new ClientMessageSerializer(), ClientMessage.class);
    serverWriter.registerMessageSerializer(new StringSerializer(), String.class);
    return serverWriter;
  }

  private void readInBackground() {
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        try {
          blockingReadServer(socket);
        } catch (SocketException e) {
          if (!disconnectedGracefully) {
            throw e;
          }
        }
        return null;
      }
    });
  }

  @Override
  @PreDestroy
  public void disconnect() {
    disconnectedGracefully = true;
    IOUtils.closeQuietly(socket);
    logger.info("Disconnected from mods server");
  }

  @Override
  public CompletableFuture<List<ModInfo>> searchMod(String name) {
    searchModFuture = new CompletableFuture<>();
    writeToServer(new SearchModMessage(name, SearchModMessage.ModType.ALL));
    return searchModFuture;
  }

  private void writeToServer(ClientMessage message) {
    serverWriter.write(message);
  }

  @Override
  protected void onServerMessage(String message) {
    ServerCommand serverCommand = ServerCommand.fromString(message);
    if (serverCommand != null) {
      throw new IllegalStateException("Didn't expect an unknown server message from mods server");
    }

    try {
      ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);

      ServerMessageType serverMessageType = serverMessage.getServerMessageType();

      if (serverMessageType != MOD_RESULT_LIST) {
        throw new IllegalStateException("Unexpected object type: " + serverMessageType);
      }

      ModSearchResult modSearchResult = gson.fromJson(message, ModSearchResult.class);
      dispatchStatisticsObject(message, modSearchResult);
    } catch (JsonSyntaxException e) {
      logger.warn("Could not deserialize message: " + message, e);
    }
  }

  private void dispatchStatisticsObject(String jsonString, ModSearchResult modSearchResult) {
    searchModFuture.complete(modSearchResult.getModList());
    searchModFuture = null;
  }
}
