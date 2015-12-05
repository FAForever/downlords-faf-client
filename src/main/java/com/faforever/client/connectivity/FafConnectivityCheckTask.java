package com.faforever.client.connectivity;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.relay.GpgServerMessage;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.relay.SendNatPacketMessage;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.upnp.UpnpService;
import com.faforever.client.util.SocketAddressUtil;
import com.google.common.primitives.Bytes;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.US_ASCII;

public class FafConnectivityCheckTask extends AbstractPrioritizedTask<ConnectivityState> implements ConnectivityCheckTask {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TIMEOUT = 5000;

  @Resource
  UpnpService upnpService;
  @Resource
  I18n i18n;
  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  ExecutorService executorService;

  private int port;
  private CompletableFuture<DatagramPacket> gamePortPacketFuture;
  private CompletableFuture<ConnectivityState> connectivityStateFuture;
  private DatagramSocket publicSocket;

  public FafConnectivityCheckTask() {
    super(Priority.LOW);
  }

  private void onConnectivityStateMessage(GpgServerMessage serverMessage) {
    switch (serverMessage.getMessageType()) {
      case SEND_NAT_PACKET:
        // The server did not receive the expected response and wants us to send a UDP packet in order hole punch the NAT.
        gamePortPacketFuture.cancel(true);

        onSendNatPacket((SendNatPacketMessage) serverMessage);
        break;

      case CONNECTIVITY_STATE:
        // The server tells us what our connectivity state is, we're done
        ConnectivityState state = ((ConnectivityStateMessage) serverMessage).getState();
        logger.debug("Received connectivity state from server: " + state);
        connectivityStateFuture.complete(state);
        break;
    }
  }

  private void onSendNatPacket(SendNatPacketMessage sendNatPacketMessage) {
    InetSocketAddress publicAddress = sendNatPacketMessage.getPublicAddress();
    String message = sendNatPacketMessage.getMessage();

    logger.debug("Sending NAT packet to {}: ", publicAddress, message);

    byte[] bytes = Bytes.concat(new byte[]{(byte) 0x08}, message.getBytes(US_ASCII));
    DatagramPacket datagramPacket = new DatagramPacket(bytes, bytes.length);
    datagramPacket.setSocketAddress(publicAddress);
    try {
      publicSocket.send(datagramPacket);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ConnectivityState call() throws Exception {
    updateTitle(i18n.get("portCheckTask.tryingUpnp"));
    upnpService.forwardPort(port);
    return checkConnectivity();
  }

  @NotNull
  private ConnectivityState checkConnectivity() throws IOException, ExecutionException, InterruptedException {
    updateTitle(i18n.get("portCheckTask.title"));

    connectivityStateFuture = new CompletableFuture<>();

    Consumer<GpgServerMessage> connectivityStateMessageListener = this::onConnectivityStateMessage;
    lobbyServerAccessor.addOnMessageListener(ConnectivityStateMessage.class, this::onConnectivityStateMessage);

    try (DatagramSocket datagramSocket = new DatagramSocket(port)) {
      this.publicSocket = datagramSocket;
      try {
        if (isGamePortPublic(port)) {
          return ConnectivityState.PUBLIC;
        }

        return connectivityStateFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        throw new RuntimeException(e);
      } finally {
        lobbyServerAccessor.removeOnMessageListener(ConnectivityStateMessage.class, this::onConnectivityStateMessage);
      }
    }
  }

  private boolean isGamePortPublic(int port) {
    logger.info("Testing connectivity of game port: {}", publicSocket.getPort());
    gamePortPacketFuture = listenForPackage(publicSocket);

    lobbyServerAccessor.initConnectivityTest(port);
    try {
      DatagramPacket udpPacket = gamePortPacketFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
      logger.debug("Received UPD package from server on port {}: {}", this.port, udpPacket.getData());

      byte[] data = udpPacket.getData();
      String message = new String(data, 0, udpPacket.getLength(), US_ASCII);
      String address = SocketAddressUtil.toString((InetSocketAddress) udpPacket.getSocketAddress());

      ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage(address, message);
      processNatPacketMessage.setTarget(MessageTarget.CONNECTIVITY);
      lobbyServerAccessor.sendGpgMessage(processNatPacketMessage);
    } catch (CancellationException e) {
      logger.debug("Waiting for UDP package on public game port has been cancelled");
      return false;
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      throw new RuntimeException(e);
    }

    try {
      return connectivityStateFuture.get(TIMEOUT, TimeUnit.MILLISECONDS) == ConnectivityState.PUBLIC;
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
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
