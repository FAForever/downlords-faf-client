package com.faforever.client.legacy.update;

import com.faforever.client.legacy.writer.JsonSerializer;
import com.faforever.client.legacy.writer.ServerWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;

public class UpdateServerAccessorImpl implements UpdateServerAccessor {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Environment environment;

  private ServerWriter serverWriter;

  private CountDownLatch serverResponseLatch;

  @PostConstruct
  void postConstruct() {
  }

  @Override
  public void connect() {
//    updateServerConnectionTask = new Task<Void>() {
//      @Override
//      protected Void call() throws Exception {
//        while (!isCancelled()) {
//          String updateServerHost = environment.getProperty("update.host");
//          Integer updateServerPort = environment.getProperty("update.port", int.class);
//
//          logger.info("Trying to connect to FAF update server at {}:{}", updateServerHost, updateServerPort);
//
//          try (Socket fafServerSocket = new Socket(updateServerHost, updateServerPort);
//               OutputStream outputStream = fafServerSocket.getOutputStream()) {
//            this.fafServerSocket = fafServerSocket;
//
//            fafServerSocket.setKeepAlive(true);
//
//            logger.info("FAF server connection established");
//            if (onLobbyConnectedListener != null) {
//              Platform.runLater(onLobbyConnectedListener::onFaConnected);
//            }
//
//            serverWriter = createServerWriter(outputStream);
//
//            writeToServer(ClientMessage.askSession(username));
//
//            blockingReadServer(fafServerSocket);
//          } catch (IOException e) {
//            if (isCancelled()) {
//              logger.debug("Login has been cancelled");
//            } else {
//              logger.warn("Lost connection to FAF server, trying to reconnect in " + RECONNECT_DELAY / 1000 + "s", e);
//              if (onLobbyDisconnectedListener != null) {
//                Platform.runLater(onLobbyDisconnectedListener::onFaDisconnected);
//              }
//              Thread.sleep(RECONNECT_DELAY);
//            }
//          }
//        }
//        return null;
//      }
//
//      @Override
//      protected void cancelled() {
//        try {
//          if (fafServerSocket != null) {
//            serverWriter.close();
//            fafServerSocket.close();
//          }
//          logger.debug("Closed connection to FAF lobby server");
//        } catch (IOException e) {
//          logger.warn("Could not close fafServerSocket", e);
//        }
//      }
//    };
//    executeInBackground(fafConnectionTask);
  }

  @Override
  public void requestBinFilesToUpdate(String modName) throws IOException {
    serverResponseLatch = new CountDownLatch(1);
//    serverWriter.write(new RequestFilesToUpdateMessage(modName));
  }

  @Override
  public void requestGameDataFilesToUpdate(String modName) {
//    serverWriter.write(new RequestFilesToUpdateMessage(modName));
  }

  private ServerWriter createServerWriter(OutputStream outputStream) throws IOException {
    ServerWriter serverWriter = new ServerWriter(outputStream);
    serverWriter.registerObjectWriter(new JsonSerializer<>(), RequestFilesToUpdateMessage.class);
    return serverWriter;
  }
}
