package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.ServerWriter;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class LocalRelayServerImpl implements LocalRelayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  private Environment environment;

  private boolean p2pProxyEnabled;
  private int port;

  @PostConstruct
  void init() {
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        start();
        return null;
      }
    });
  }

  /**
   * Starts a local, GPG-like server in background that SupCom can connect to. Received data is forwarded to the FAF
   * server.
   */
  @Override
  public void start() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {

      port = serverSocket.getLocalPort();

      logger.info("Relay server listening on port {}", port);

      while (!isCancelled()) {
        try (Socket supComSocket = serverSocket.accept()) {
          logger.debug("SupCom connected to relay server from {}:{}", supComSocket.getInetAddress(), supComSocket.getPort());

          try (Socket fafSocket = new Socket(environment.getProperty("relay.host"), environment.getProperty("relay.port", int.class));
               FaDataInputStream supComInput = createFaInputStream(supComSocket.getInputStream());
               FaDataOutputStream supComOutput = createFaOutputStream(supComSocket.getOutputStream());
               ServerWriter serverWriter = new ServerWriter(fafSocket.getOutputStream());
               ServerReader serverReader = new ServerReader(fafSocket.getInputStream())) {

            startFaReader(supComInput, serverWriter);
            startServerReader(serverReader, supComOutput);
          }
        }
      }
    }
  }

  private FaDataInputStream createFaInputStream(InputStream inputStream) throws IOException {
    return new FaDataInputStream(inputStream);
  }

  private FaDataOutputStream createFaOutputStream(OutputStream outputStream) {
    return new FaDataOutputStream(outputStream);
  }

  /**
   * Starts a background task that reads data from SupCom and redirects it to the given ServerWriter.
   */
  private void startFaReader(final FaDataInputStream faInputStream, ServerWriter serverWriter) {
    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        redirectFaToFaf(faInputStream, serverWriter);
        return null;
      }
    });
  }

  /**
   * Connects to the FAF relay server and forwards any data received to the given output stream.
   */
  private void startServerReader(ServerReader serverReader, FaDataOutputStream supComOutput) throws IOException {
    serverReader.setFaOutputStream(supComOutput);
    serverReader.blockingRead();
  }

  private void redirectFaToFaf(FaDataInputStream faInputStream, ServerWriter serverWriter) throws IOException {
    String action;

    while (!isCancelled()) {
      action = faInputStream.readString();
      List<Object> chunks = faInputStream.readChunks();
      RelayClientMessage relayClientMessage = RelayClientMessage.create(action, chunks);

      serverWriter.write(relayClientMessage);
    }
  }


  private boolean isCancelled() {
    return false;
  }

  @Override
  public int getPort() {
    return port;
  }
}
