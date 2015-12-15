package com.faforever.client.connectivity;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.relay.GpgServerMessage;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.task.AbstractPrioritizedTask;
import com.faforever.client.util.Assert;
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

/**
 * Detects the connectivity state in cooperation with the FAF server. <p> <ol> <li>Step: </li> </ol> </p>
 */
public class FafConnectivityCheckTask extends AbstractPrioritizedTask<ConnectivityState> implements ConnectivityCheckTask {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TIMEOUT = 5000;

  @Resource
  I18n i18n;
  @Resource
  FafService fafService;
  @Resource
  ExecutorService executorService;

  private CompletableFuture<DatagramPacket> gamePortPacketFuture;
  private CompletableFuture<ConnectivityState> connectivityStateFuture;
  private DatagramSocket publicSocket;

  public FafConnectivityCheckTask() {
    super(Priority.LOW);
  }

  private void onConnectivityStateMessage(GpgServerMessage serverMessage) {
    if (serverMessage.getTarget() != MessageTarget.CONNECTIVITY) {
      return;
    }

    switch (serverMessage.getMessageType()) {
      case SEND_NAT_PACKET:
        // The server did not receive the expected response and wants us to send a UDP packet in order hole punch the
        // NAT. This is done by connectivity service.
        gamePortPacketFuture.cancel(true);
        break;

      case CONNECTIVITY_STATE:
        // The server tells us what our connectivity state is, we're done
        ConnectivityState state = ((ConnectivityStateMessage) serverMessage).getState();
        logger.debug("Received connectivity state from server: " + state);
        connectivityStateFuture.complete(state);
        gamePortPacketFuture.cancel(true);
        break;
    }
  }

  @Override
  protected ConnectivityState call() throws Exception {
    Assert.checkNullIllegalState(publicSocket, "publicSocket has not been set");
    return checkConnectivity();
  }

  @NotNull
  private ConnectivityState checkConnectivity() throws IOException, ExecutionException, InterruptedException {
    updateTitle(i18n.get("portCheckTask.title"));

    connectivityStateFuture = new CompletableFuture<>();

    Consumer<GpgServerMessage> connectivityStateMessageListener = this::onConnectivityStateMessage;
    fafService.addOnMessageListener(GpgServerMessage.class, connectivityStateMessageListener);

    try {
      if (isGamePortPublic(publicSocket.getLocalPort())) {
        return ConnectivityState.PUBLIC;
      }

      return connectivityStateFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    } finally {
      fafService.removeOnMessageListener(GpgServerMessage.class, connectivityStateMessageListener);
    }
  }

  private boolean isGamePortPublic(int port) {
    logger.info("Testing connectivity of game port: {}", port);
    gamePortPacketFuture = listenForPackage(publicSocket);

    fafService.initConnectivityTest(port);
    try {
      DatagramPacket udpPacket = gamePortPacketFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
      logger.debug("Received UPD package from server on {}", publicSocket);

      byte[] data = udpPacket.getData();
      String message = new String(data, 0, udpPacket.getLength(), US_ASCII);
      InetSocketAddress address = (InetSocketAddress) udpPacket.getSocketAddress();

      ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage(address, message);
      processNatPacketMessage.setTarget(MessageTarget.CONNECTIVITY);
      fafService.sendGpgMessage(processNatPacketMessage);
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
    byte[] buffer = new byte[64];
    DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length);
    return CompletableFuture.supplyAsync(() -> {

      try {
        datagramSocket.receive(datagramPacket);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return datagramPacket;
    }, executorService);
  }

  @Override
  public void setPublicSocket(DatagramSocket publicSocket) {
    this.publicSocket = publicSocket;
  }

  @Override
  public DatagramSocket getPublicSocket() {
    return publicSocket;
  }
}
