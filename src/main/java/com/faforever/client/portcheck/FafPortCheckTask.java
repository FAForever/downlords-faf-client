package com.faforever.client.portcheck;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.FafServerMessage;
import com.faforever.client.legacy.domain.FafServerMessageType;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.legacy.relay.ConnectivityStateMessage;
import com.faforever.client.legacy.relay.GpgServerMessage;
import com.faforever.client.legacy.relay.GpgServerMessageType;
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
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

  private void onConnectivityStateMessage(FafServerMessage message) {
    if (message.getMessageType() == FafServerMessageType.CONNECTIVITY_STATE) {
      // The server tells us what our connectivity state is, we're done
      connectivityStateFuture.complete(((ConnectivityStateMessage) message).getState());
    }
  }

  private void onGpgServerMessage(GpgServerMessage message) {
    if (message.getTarget() != MessageTarget.CONNECTIVITY) {
      return;
    }

    if (message.getMessageType() == GpgServerMessageType.SEND_NAT_PACKET) {
      // If a SEND_NAT_PACKET is received, it means the server did not receive the expected response and wants us to send
      // a UDP packet in order to try NAT hole punching.
      udpPacketFuture.cancel(true);
      try {
        byte[] bytes = (Byte.toString((byte) 0x08) + message).getBytes(US_ASCII);
        for (int i = 0; i < UDP_PACKET_REPEATS; i++) {
          datagramSocket.send(new DatagramPacket(bytes, bytes.length));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  protected ConnectivityState call() throws Exception {
    updateTitle(i18n.get("portCheckTask.title"));

    ConnectivityState connectivityState = checkConnectivity(port);
    if (connectivityState == ConnectivityState.PUBLIC) {
      return connectivityState;
    }

    logger.info("Port is not public, trying UPnP");
    upnpService.forwardPort(port);
    return checkConnectivity(port);
  }

  @NotNull
  private ConnectivityState checkConnectivity(int port) throws IOException, ExecutionException, InterruptedException {
    logger.info("Testing connectivity of UDP port {}", port);

    connectivityStateFuture = new CompletableFuture<>();
    Consumer<GpgServerMessage> gpgMessageListener = this::onGpgServerMessage;
    Consumer<FafServerMessage> connectivityStateMessageListener = this::onConnectivityStateMessage;

    try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
      this.datagramSocket = datagramSocket;

      datagramSocket.setSoTimeout(TIMEOUT);
      udpPacketFuture = listenForPackage(datagramSocket);

      lobbyServerAccessor.addOnConnectivityStateMessageListener(connectivityStateMessageListener);
      lobbyServerAccessor.addOnGpgServerMessageListener(gpgMessageListener);
      lobbyServerAccessor.initConnectivityTest();
      try {
        DatagramPacket udpPacket = udpPacketFuture.get();

        byte[] data = udpPacket.getData();
        String message = new String(data, 1, data.length - 1, US_ASCII);
        String address = SocketAddressUtil.toString((InetSocketAddress) udpPacket.getSocketAddress());

        lobbyServerAccessor.sendGpgMessage(new ProcessNatPacketMessage(address, message));
      } catch (CancellationException e) {
        // It's ok
      } finally {
        lobbyServerAccessor.removeOnGpgServerMessageListener(gpgMessageListener);
        lobbyServerAccessor.removeOnFafServerMessageListener(connectivityStateMessageListener);
      }

      return connectivityStateFuture.get();
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
