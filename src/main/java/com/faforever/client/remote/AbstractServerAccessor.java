package com.faforever.client.remote;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.remote.domain.inbound.faf.FafInboundMessage;
import com.faforever.client.remote.io.QDataInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.DisposableBean;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Super class for all server accessors.
 */
@Slf4j
public abstract class AbstractServerAccessor implements DisposableBean {

  private boolean stopped;
  private QDataInputStream dataInput;

  /**
   * Reads data received from the server and dispatches it. So far, there are two types of data sent by the server:
   * <ol>
   * <li><strong>Server messages</strong> are simple words like ACK or PING, followed by some bytes..</li>
   * <li><strong>Objects</strong> are JSON-encoded objects like preferences or player information. Those are converted
   * into a
   * {@link FafInboundMessage}</li> </ol> I'm not yet happy with those terms, so any suggestions are welcome.
   */
  protected void blockingReadServer(Socket socket) throws IOException {
    JavaFxUtil.assertBackgroundThread();

    dataInput = new QDataInputStream(new DataInputStream(new BufferedInputStream(socket.getInputStream())));
    while (!stopped && !socket.isInputShutdown()) {
      dataInput.skipBlockSize();
      String message = dataInput.readQString();

      log.debug("Message from server: {}", message);

      try {
        onServerMessage(message);
      } catch (Exception e) {
        log.warn("Error while handling server message: " + message, e);
      }
    }

    log.info("Connection to server {} has been closed", socket.getRemoteSocketAddress());
  }

  protected abstract void onServerMessage(String message) throws IOException;

  @Override
  public void destroy() throws IOException {
    stopped = true;
    IOUtils.closeQuietly(dataInput);
  }

}
