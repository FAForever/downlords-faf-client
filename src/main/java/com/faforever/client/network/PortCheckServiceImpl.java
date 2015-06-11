package com.faforever.client.network;

import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import static com.faforever.client.task.PrioritizedTask.Priority.LOW;
import static com.faforever.client.task.TaskGroup.NET_LIGHT;

public class PortCheckServiceImpl implements PortCheckService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int REQUEST_DELAY = 1000;
  private static final int TIMEOUT = 5000;
  private static final String EXPECTED_ANSWER = "OK";

  @Autowired
  TaskService taskService;

  @Autowired
  Environment environment;

  @Override
  public void checkUdpPortInBackground(int port, Callback<Boolean> callback) {
    taskService.submitTask(NET_LIGHT, new PrioritizedTask<Boolean>(LOW) {
      @Override
      protected Boolean call() throws Exception {
        String remoteHost = environment.getProperty("portCheck.host");
        Integer remotePort = environment.getProperty("portCheck.port", Integer.class);

        logger.info("Checking reachability of UDP port {} using port checker service at {}:{}", port, remoteHost, remotePort);

        sendDelayedPackageRequest(remoteHost, remotePort, port);

        try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
          datagramSocket.setSoTimeout(TIMEOUT);

          byte[] buffer = new byte[EXPECTED_ANSWER.length()];
          datagramSocket.receive(new DatagramPacket(buffer, buffer.length));

          logger.info("UDP port {} is reachable", port);

          return true;
        } catch (SocketTimeoutException e) {
          logger.info("UDP port {} is unreachable", port);
          return false;
        }
      }
    }, callback);
  }

  /**
   * Sends a request to the given external port checker server to send a UDP package to the given port. This request is
   * sent with a short delay to allow the UDP port to be opened.
   */
  private void sendDelayedPackageRequest(final String remoteHost, final Integer remotePort, final int port) {
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          try (Socket socket = new Socket(remoteHost, remotePort);
               DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {
            dataOutputStream.writeInt(port);
          }
        } catch (ConnectException e) {
          logger.warn("Port check server is not reachable");
        } catch(IOException e) {
          throw new RuntimeException(e);
        }
      }
    }, 1000);
  }
}
