package com.faforever.client.portcheck;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.relay.ConnectivityStateMessage;
import com.faforever.client.legacy.relay.GpgServerMessage;
import com.faforever.client.legacy.relay.ProcessNatPacketMessage;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.upnp.UpnpService;
import com.faforever.client.util.SocketAddressUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class FafPortCheckTask extends AbstractPrioritizedTask<ConnectivityState> implements PortCheckTask {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TIMEOUT = 10000;
  private static final int UDP_PACKET_REPEATS = 3;

  @Resource
  UpnpService upnpService;
  @Resource
  I18n i18n;
  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  ExecutorService executorService;

  private int port;
  private CompletableFuture<DatagramPacket> udpPacketFuture;
  private DatagramSocket datagramSocket;
  private CompletableFuture<ConnectivityState> connectivityStateFuture;

  public FafPortCheckTask() {
    super(Priority.LOW);
  }

  private void onConnectivityStateMessage(GpgServerMessage message) {
    if (message.getTarget() != MessageTarget.CONNECTIVITY) {
      return;
    }

    switch (message.getMessageType()) {
      case SEND_NAT_PACKET:
        // The server did not receive the expected response and wants us to send a UDP packet in order hole punch the NAT.
        try {
          byte[] bytes = (Byte.toString((byte) 0x08) + message).getBytes(US_ASCII);
          for (int i = 0; i < UDP_PACKET_REPEATS; i++) {
            datagramSocket.send(new DatagramPacket(bytes, bytes.length));
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        break;

      case CONNECTIVITY_STATE:
        // The server tells us what our connectivity state is, we're done
        connectivityStateFuture.complete(((ConnectivityStateMessage) message).getState());
        break;
    }
  }

  @Override
  protected ConnectivityState call() throws Exception {
    updateTitle(i18n.get("portCheckTask.title"));
    upnpService.forwardPort(port);
    return checkConnectivity(port);
  }

  @NotNull
  private ConnectivityState checkConnectivity(int port) throws IOException, ExecutionException, InterruptedException {
    logger.info("Testing connectivity of UDP port {}", port);

    connectivityStateFuture = new CompletableFuture<>();
    Consumer<GpgServerMessage> connectivityStateMessageListener = this::onConnectivityStateMessage;

    try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
      this.datagramSocket = datagramSocket;

      datagramSocket.setSoTimeout(TIMEOUT);
      udpPacketFuture = listenForPackage(datagramSocket);

      lobbyServerAccessor.addOnGameMessageListener(connectivityStateMessageListener);
      lobbyServerAccessor.initConnectivityTest();
      try {
        DatagramPacket udpPacket = udpPacketFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
        logger.debug("Received UPD package from server on port {}", port);

        byte[] data = udpPacket.getData();
        String message = new String(data, 1, udpPacket.getLength() - 1, US_ASCII);
        String address = SocketAddressUtil.toString((InetSocketAddress) udpPacket.getSocketAddress());

        ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage(address, message);
        processNatPacketMessage.setTarget(MessageTarget.CONNECTIVITY);
        lobbyServerAccessor.sendGpgMessage(processNatPacketMessage);

        return connectivityStateFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        logger.debug("Timed out while waiting for answer from server");
        return ConnectivityState.BLOCKED;
      } finally {
        lobbyServerAccessor.removeOnConnectivityMessageListener(connectivityStateMessageListener);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private CompletableFuture<DatagramPacket> listenForPackage(DatagramSocket datagramSocket) {
    return CompletableFuture.supplyAsync(() -> {
      byte[] buffer = new byte[64];
      DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);

      try {
        datagramSocket.receive(datagramPacket);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return datagramPacket;
    }, executorService);
  }

  public void setPort(int port) {
    this.port = port;
  }
}
