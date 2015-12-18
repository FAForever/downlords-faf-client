package com.faforever.client.connectivity;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.domain.MessageTarget;
import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.relay.GpgServerMessage;
import com.faforever.client.relay.LocalRelayServer;
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
public class FafConnectivityCheckTask extends AbstractPrioritizedTask<ConnectivityStateMessage> implements ConnectivityCheckTask {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int TIMEOUT = 5000;

  @Resource
  I18n i18n;
  @Resource
  FafService fafService;
  @Resource
  ExecutorService executorService;
  @Resource
  LocalRelayServer localRelayServer;

  private CompletableFuture<DatagramPacket> gamePortPacketFuture;
  private CompletableFuture<ConnectivityStateMessage> connectivityStateFuture;
  private int publicPort;

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
        gamePortPacketFuture.cancel(true);
        connectivityStateFuture.complete((ConnectivityStateMessage) serverMessage);
        break;
    }
  }

  @Override
  protected ConnectivityStateMessage call() throws Exception {
    Assert.checkNullIllegalState(publicPort, "publicPort has not been set");
    return checkConnectivity();
  }

  @NotNull
  private ConnectivityStateMessage checkConnectivity() throws IOException, ExecutionException, InterruptedException {
    updateTitle(i18n.get("portCheckTask.title"));

    connectivityStateFuture = new CompletableFuture<>();

    Consumer<GpgServerMessage> connectivityStateMessageListener = this::onConnectivityStateMessage;
    fafService.addOnMessageListener(GpgServerMessage.class, connectivityStateMessageListener);

    try {
      runTestForPort(publicPort);
      return connectivityStateFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    } finally {
      fafService.removeOnMessageListener(GpgServerMessage.class, connectivityStateMessageListener);
    }
  }

  private void runTestForPort(int port) {
    logger.info("Testing public connectivity of game port: {}", port);
    try {
      gamePortPacketFuture = listenForPackage();

      fafService.initConnectivityTest(port);
      DatagramPacket udpPacket = gamePortPacketFuture.get(TIMEOUT, TimeUnit.MILLISECONDS);
      logger.info("Received UDP package on port {}", port);

      byte[] data = udpPacket.getData();
      String message = new String(data, 1, udpPacket.getLength() - 1, US_ASCII);
      InetSocketAddress address = (InetSocketAddress) udpPacket.getSocketAddress();

      ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage(address, message);
      processNatPacketMessage.setTarget(MessageTarget.CONNECTIVITY);
      fafService.sendGpgMessage(processNatPacketMessage);
    } catch (CancellationException e) {
      logger.debug("Waiting for UDP package on public game port has been cancelled");
    } catch (InterruptedException | TimeoutException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private CompletableFuture<DatagramPacket> listenForPackage() throws ExecutionException, InterruptedException {
    CompletableFuture<DatagramPacket> future = new CompletableFuture<>();
    Consumer<DatagramPacket> complete = future::complete;
    localRelayServer.addOnPacketFromOutsideListener(complete);
    return future.thenComposeAsync(datagramPacket -> {
      localRelayServer.removeOnPackedFromOutsideListener(complete);
      return CompletableFuture.completedFuture(datagramPacket);
    });
  }

  public int getPublicPort() {
    return publicPort;
  }

  @Override
  public void setPublicPort(int publicPort) {
    this.publicPort = publicPort;
  }
}
