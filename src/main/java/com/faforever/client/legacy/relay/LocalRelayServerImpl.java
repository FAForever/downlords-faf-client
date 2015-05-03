package com.faforever.client.legacy.relay;

import com.faforever.client.legacy.ServerWriter;
import com.faforever.client.legacy.proxy.Proxy;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static com.faforever.client.util.ConcurrentUtil.executeInBackground;

public class LocalRelayServerImpl implements LocalRelayServer, Proxy.OnProxyInitializedListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Autowired
  Proxy proxyServer;

  @Autowired
  Environment environment;

  private boolean p2pProxyEnabled;
  private int port;

  @Override
  public int getPort() {
    return port;
  }

  /**
   * Starts a local, GPG-like server in background that FA can connect to. Received data is forwarded to the FAF server
   * and vice-versa.
   */
  @Override
  @PostConstruct
  public void startInBackground() throws IOException {
    proxyServer.addOnProxyInitializedListener(this);

    executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        start();
        return null;
      }
    });
  }

  private void start() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {

      port = serverSocket.getLocalPort();

      logger.info("Relay server listening on port {}", port);

      while (!isCancelled()) {
        try (Socket supComSocket = serverSocket.accept()) {
          logger.debug("Forged Alliance connected to relay server from {}:{}", supComSocket.getInetAddress(), supComSocket.getPort());

          try (Socket fafSocket = new Socket(environment.getProperty("relay.host"), environment.getProperty("relay.port", int.class));
               FaDataInputStream supComInput = createFaInputStream(supComSocket.getInputStream());
               FaDataOutputStream faOutputStream = createFaOutputStream(supComSocket.getOutputStream());
               ServerWriter serverWriter = new ServerWriter(fafSocket.getOutputStream());
               ServerReader serverReader = new ServerReader(fafSocket.getInputStream(), proxyServer, faOutputStream, serverWriter)) {

            startFaReader(supComInput, serverWriter);

            serverReader.blockingRead();
          }
        }
      }
    }
  }

  private boolean isCancelled() {
    return false;
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
        try {
          redirectFaToFaf(faInputStream, serverWriter);
        } catch (EOFException e) {
          logger.info("Forged Alliance disconnected from local relay server (EOF)");
        }
        return null;
      }
    });
  }

  /**
   * Redirects any data read from the #faInputStream to the specified #serverWriter.
   *
   * @param faInputStream game input stream
   * @param serverWriter FAF relay server writer
   *
   * @throws IOException
   */
  private void redirectFaToFaf(FaDataInputStream faInputStream, ServerWriter serverWriter) throws IOException {

    while (!isCancelled()) {
      RelayServerAction action = RelayServerAction.fromString(faInputStream.readString());
      List<Object> chunks = faInputStream.readChunks();
      RelayClientMessage relayClientMessage = new RelayClientMessage(action, chunks);

      if (p2pProxyEnabled) {
        updateProxyState(relayClientMessage);
      }

      serverWriter.write(relayClientMessage);
    }
  }

  private void updateProxyState(RelayClientMessage relayClientMessage) {
    RelayServerAction action = relayClientMessage.getAction();
    List<Object> chunks = relayClientMessage.getChunks();

    logger.debug("Received '{}' with chunks: {}", action.getString(), chunks);

    switch (action) {
      case PROCESS_NAT_PACKET:
        chunks.set(0, proxyServer.translateToPublic((String) chunks.get(0)));
        break;
      case DISCONNECTED:
        proxyServer.updateConnectedState((Integer) chunks.get(0), false);
        break;
      case CONNECTED:
        proxyServer.updateConnectedState((Integer) chunks.get(0), true);
        break;
      case GAME_STATE:
        switch ((String) chunks.get(0)) {
          case "Launching":
            proxyServer.setGameLaunched(true);
            break;
          case "Lobby":
            proxyServer.setGameLaunched(false);
            break;
        }
        break;
      case BOTTLENECK:
        proxyServer.setBottleneck(true);
        break;
      case BOTTLENECK_CLEARED:
        proxyServer.setBottleneck(false);
        break;
      case PONG:
        logger.warn("Server sent PONG which is unhandled");
        break;
      case UNKNOWN:
        logger.warn("Ignoring unknown ");
        break;

      default:
        throw new IllegalStateException("Known but unhandled relay server action: " + action);
    }
  }

  @Override
  public void onProxyInitialized() {
    p2pProxyEnabled = true;
  }
}
