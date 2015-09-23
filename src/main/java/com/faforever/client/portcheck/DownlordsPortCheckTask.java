package com.faforever.client.portcheck;

import com.faforever.client.i18n.I18n;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.upnp.UpnpService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class DownlordsPortCheckTask extends AbstractPrioritizedTask<Boolean> implements PortCheckTask {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TIMEOUT = 2000;
  private static final String EXPECTED_ANSWER = "OK";

  @Autowired
  UpnpService upnpService;

  @Autowired
  Environment environment;

  @Autowired
  I18n i18n;

  private int port;

  public DownlordsPortCheckTask() {
    super(Priority.LOW);
  }

  @Override
  protected Boolean call() throws Exception {
    updateTitle(i18n.get("portCheckTask.title"));

    Boolean successful = checkPort(port);
    if (!successful) {
      logger.info("Port check failed, trying UPnP");
      upnpService.forwardPort(port);
      successful = checkPort(port);
    }
    return successful;
  }

  @NotNull
  private Boolean checkPort(int port) throws IOException {
    String remoteHost = environment.getProperty("portCheck.host");
    Integer remotePort = environment.getProperty("portCheck.port", Integer.class);

    logger.info("Testing reachability of UDP port {} using port test service at {}:{}", port, remoteHost, remotePort);

    try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
      datagramSocket.setSoTimeout(TIMEOUT);

      sendPacketRequest(remoteHost, remotePort, port);

      byte[] buffer = new byte[EXPECTED_ANSWER.length()];
      datagramSocket.receive(new DatagramPacket(buffer, buffer.length));

      logger.info("UDP port {} is reachable", port);

      return true;
    } catch (SocketTimeoutException e) {
      logger.info("UDP port {} is unreachable ({})", port, e.getMessage());
      return false;
    }
  }

  /**
   * Sends a request to the specified external port checker server to send a UDP package to the given port.
   */
  private void sendPacketRequest(final String remoteHost, final Integer remotePort, final int port) throws IOException {
    try (Socket socket = new Socket(remoteHost, remotePort);
         DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
      dataOutputStream.writeInt(port);
    }
  }

  public void setPort(int port) {
    this.port = port;
  }
}
