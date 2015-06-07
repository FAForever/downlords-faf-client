package com.faforever.client.legacy.update;

import com.faforever.client.game.ModInfoBean;
import com.faforever.client.legacy.writer.ServerWriter;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.update.UpdateService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.DigestUtils;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class LegacyUpdateService implements UpdateService, UpdateServerResponseListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private enum StandardFileGroup {
    FAF,
    FAFGAMEDATA;
  }

  @Autowired
  Environment environment;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  UpdateServerAccessor updateServerAccessor;

  private CountDownLatch serverResponseLatch;
  private Path destinationDirectory;
  private ServerWriter serverWriter;

  @Override
  public Service<Void> updateInBackground(ModInfoBean modInfoBean, Callback<Void> callback) {
    return ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        update( modInfoBean);
        return null;
      }
    }, callback);
  }

  private void update(ModInfoBean modInfoBean) throws IOException {
    String host = environment.getProperty("update.host");
    int port = environment.getProperty("update.port", Integer.class);

    logger.info("Updating files using update server {}:{}", host, port);

    while (!isCancelled()) {
      try (Socket updateServerSocket = new Socket(host, port);
           ServerWriter serverWriter = new ServerWriter(updateServerSocket.getOutputStream())) {
        this.serverWriter = serverWriter;

        startUpdateServerReaderInBackground(updateServerSocket.getInputStream());

        updateServerAccessor.requestBinFilesToUpdate(modInfoBean);
        updateServerAccessor.requestGameDataFilesToUpdate(modInfoBean);
      }
    }
  }

  private void updateFiles(Path destination, ModInfoBean modInfoBean) throws IOException {

    try {
      serverResponseLatch.await();
    } catch (InterruptedException e) {
      logger.warn("Got interrupted while waiting for file list", e);
    }
  }

  private void startUpdateServerReaderInBackground(InputStream inputStream) throws IOException {
    try (UpdateServerReader updateServerReader = new UpdateServerReader(inputStream, this)) {
      ConcurrentUtil.executeInBackground(new Task<Void>() {
        @Override
        protected Void call() throws Exception {
          updateServerReader.blockingRead();
          return null;
        }
      });
    }
  }

  private void requestSimPath(ServerWriter serverWriter) {
    serverWriter.write(new RequestSimPathMessage());
  }

  public boolean isCancelled() {
    return false;
  }

  @Override
  public void onSimModPath(String s) {

  }

  @Override
  public void onSimModNotFound() {

  }

  @Override
  public void onFilesToUpdate(Set<String> filesToUpdate) throws IOException {
    serverResponseLatch.countDown();

    Path targetDirectory = preferencesService.getFafGameDataDirectory();
    Files.createDirectories(targetDirectory);

    for (String fileToUpdate : filesToUpdate) {
      Path targetFile = destinationDirectory.resolve(fileToUpdate);

      if(Files.notExists(targetFile)) {
//        if(isModRequested()) {
//          serverWriter.write(UpdateClientMessage.requestModVersion());
//        } else {
//          serverWriter.write(UpdateClientMessage.requestModVersion());
//        }
      }

      String md5OfFileToUpdate = DigestUtils.md5(targetFile);

    }
  }

  @Override
  public void onUnknownApp() {

  }

  @Override
  public void onServerBusy() {

  }

  @Override
  public void onVersionPatchNotFound(String s) {

  }

  @Override
  public void onModPatchNotFound(String s) {

  }

  @Override
  public void onPatchNotFound(String s) {

  }

  @Override
  public void onFileUpToDate(String s) {

  }

  @Override
  public void onFileNotFound(String s) {

  }

  @Override
  public void onSendFilePath(String path, String fileToCopy, String url) {

  }

  @Override
  public void onSendFile(String s) {

  }
}
