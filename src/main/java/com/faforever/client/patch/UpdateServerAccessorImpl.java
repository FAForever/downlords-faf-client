package com.faforever.client.patch;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.AbstractServerAccessor;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.patch.domain.IncrementModDownloadCountRequest;
import com.faforever.client.patch.domain.ModPatchRequest;
import com.faforever.client.patch.domain.ModVersionRequest;
import com.faforever.client.patch.domain.PatchRequest;
import com.faforever.client.patch.domain.PathRequest;
import com.faforever.client.patch.domain.RequestRequest;
import com.faforever.client.patch.domain.SimPathRequest;
import com.faforever.client.patch.domain.UpdateFileRequest;
import com.faforever.client.patch.domain.UpdateServerRequest;
import com.faforever.client.patch.domain.VersionRequest;
import com.faforever.client.util.ConcurrentUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UpdateServerAccessorImpl extends AbstractServerAccessor implements UpdateServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Gson gson;
  @Autowired
  Environment environment;
  @Autowired
  I18n i18n;
  @Autowired
  NotificationService notificationService;
  private Socket socket;
  private ArrayList<String> filesToUpdate;
  private ServerWriter serverWriter;
  private CompletableFuture<List<String>> filesToUpdateFuture;
  private CompletableFuture<String> requestSimPathFuture;

  private UpdateServerResponseListener updateServerResponseListener;
  private boolean disconnectedGracefully;

  public UpdateServerAccessorImpl() {
    gson = new GsonBuilder().create();
  }

  @Override
  protected void onServerMessage(String message) throws IOException {
    UpdateServerMessageType type = UpdateServerMessageType.valueOf(message);

    switch (type) {
      case PATH_TO_SIM_MOD:
        requestSimPathFuture.complete(readNextString());
        break;

      case SIM_MOD_NOT_FOUND:
        requestSimPathFuture.completeExceptionally(new Exception(readNextString()));
        break;

      case LIST_FILES_TO_UP:
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        this.filesToUpdate = gson.fromJson(readNextString(), listType);
        filesToUpdateFuture.complete(filesToUpdate);
        break;

      case UP_TO_DATE:
        updateServerResponseListener.onFileUpToDate(readNextString());
        break;

      case SEND_FILE_PATH:
        String path = readNextString();
        String fileToCopy = readNextString();
        String url = readNextString();
        updateServerResponseListener.onFileUrl(path, fileToCopy, url);
        break;

      case SEND_PATCH_URL:
        String destination = readNextString();
        String fileToUpdate = readNextString();
        url = readNextString();

        updateServerResponseListener.onPatchUrl(destination, fileToUpdate, url);
        break;

      case VERSION_PATCH_NOT_FOUND:
        String response = readNextString();
        logger.warn("Patch version not found for '{}'", response);
        updateServerResponseListener.onVersionPatchNotFound(response);
        break;

      case VERSION_MOD_PATCH_NOT_FOUND:
        response = readNextString();
        logger.warn("Mod Patch version not found for '{}'", response);
        updateServerResponseListener.onVersionModPatchNotFound(response);
        break;

      case PATCH_NOT_FOUND:
        response = readNextString();
        logger.warn("Patch not found for '{}'", response);
        updateServerResponseListener.onPatchNotFound(response);
        break;

      case UNKNOWN_APP:
      case ERROR_FILE:
        logger.warn("{}: {}", type, readNextString());
        break;
    }
  }

  @Override
  public void connect(UpdateServerResponseListener updateServerResponseListener) {
    this.updateServerResponseListener = updateServerResponseListener;
    disconnectedGracefully = false;

    String host = environment.getProperty("update.host");
    int port = environment.getProperty("update.port", int.class);

    try {
      logger.debug("Connecting to update server {}:{}", host, port);
      this.socket = new Socket(host, port);

      logger.debug("Connection to update server established");

      serverWriter = createServerWriter(socket.getOutputStream());

      readInBackground();
    } catch (IOException e) {
      notificationService.addNotification(new PersistentNotification(i18n.get("update.error.connectionFailed"), Severity.WARN));
    }
  }

  @Override
  @PreDestroy
  public void disconnect() {
    disconnectedGracefully = true;
    updateServerResponseListener = null;
    IOUtils.closeQuietly(socket);
    logger.info("Disconnected from update server");
  }

  @Override
  public CompletionStage<List<String>> requestFilesToUpdate(String fileGroup) {
    filesToUpdateFuture = new CompletableFuture<>();
    writeToServer(new GetFilesToUpdateMessage(fileGroup));
    return filesToUpdateFuture;
  }

  private void writeToServer(UpdateServerRequest updateServerRequest) {
    serverWriter.write(updateServerRequest);
  }

  @Override
  public void requestVersion(String targetDirectoryName, String filename, String targetVersion) {
    writeToServer(new VersionRequest(targetDirectoryName, filename, targetVersion));
  }

  @Override
  public void requestModVersion(String targetDirectoryName, String filename, Map<String, Integer> modVersions) {
    String modVersionsJson = gson.toJson(modVersions);
    writeToServer(new ModVersionRequest(targetDirectoryName, filename, modVersionsJson));
  }

  @Override
  public void requestPath(String targetDirectoryName, String filename) {
    writeToServer(new PathRequest(targetDirectoryName, filename));
  }

  @Override
  public void patchTo(String targetDirectoryName, String filename, String targetVersion) {
    writeToServer(new PatchRequest(targetDirectoryName, filename, targetVersion));
  }

  @Override
  public void modPatchTo(String targetDirectoryName, String filename, Map<String, Integer> modVersions) {
    String modVersionsJson = gson.toJson(modVersions);
    writeToServer(new ModPatchRequest(targetDirectoryName, filename, modVersionsJson));
  }

  @Override
  public void update(String targetDirectoryName, String filename, String actualMd5) {
    writeToServer(new UpdateFileRequest(targetDirectoryName, filename, actualMd5));
  }

  @Override
  public CompletableFuture<String> requestSimPath(String uid) {
    requestSimPathFuture = new CompletableFuture<>();
    writeToServer(new SimPathRequest(uid));
    return requestSimPathFuture;
  }

  @Override
  public void incrementModDownloadCount(String uid) {
    writeToServer(new IncrementModDownloadCountRequest(uid));
  }

  @Override
  public void request(String targetDirectoryName, String response) {
    writeToServer(new RequestRequest(targetDirectoryName, response));
  }

  private ServerWriter createServerWriter(OutputStream outputStream) {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerMessageSerializer(UpdateServerRequestSerializer.INSTANCE, UpdateServerRequest.class);
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
}
