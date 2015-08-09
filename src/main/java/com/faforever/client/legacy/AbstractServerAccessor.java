package com.faforever.client.legacy;

import com.faforever.client.legacy.domain.ServerObject;
import com.faforever.client.legacy.io.QDataInputStream;
import com.faforever.client.util.JavaFxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.Socket;

/**
 * Super class for all server accessors.
 */
public abstract class AbstractServerAccessor {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean stopped;
  private QDataInputStream dataInput;

  /**
   * Reads data received from the server and dispatches it. So far, there are two types of data sent by the server: <ol>
   * <li><strong>Server messages</strong> are simple words like ACK or PING, followed by some bytes..</li>
   * <li><strong>Objects</strong> are JSON-encoded objects like game or player information. Those are converted into a
   * {@link ServerObject}</li> </ol> I'm not yet happy with those terms, so any suggestions are welcome.
   */
  protected void blockingReadServer(Socket socket) throws IOException {
    JavaFxUtil.assertBackgroundThread();

    try (QDataInputStream dataInput = new QDataInputStream(new DataInputStream(new BufferedInputStream(socket.getInputStream())))) {
      this.dataInput = dataInput;
      while (!stopped && !socket.isInputShutdown()) {
        dataInput.skipBlockSize();
        String message = dataInput.readQString();

        logger.debug("Message from server: {}", message);

        onServerMessage(message);
      }
    }

    logger.info("Connection to server {} has been closed", socket.getRemoteSocketAddress());
  }

  protected abstract void onServerMessage(String message) throws IOException;

  protected String readNextString() throws IOException {
    return dataInput.readQString();
  }
}
