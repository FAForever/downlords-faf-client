package com.faforever.client.connectivity;

import com.faforever.client.fa.relay.GpgServerMessage;
import com.faforever.client.i18n.I18n;
import com.faforever.client.relay.ConnectivityStateMessage;
import com.faforever.client.relay.ProcessNatPacketMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.MessageTarget;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Detects the connectivity state in cooperation with the FAF server. <p> <ol> <li>Step: </li> </ol> </p>
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConnectivityCheckTask extends CompletableTask<ConnectivityStateMessage> {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  I18n i18n;
  @Resource
  FafService fafService;
  int connectivityCheckTimeout = 60_000;

  private DatagramGateway datagramGateway;
  private CompletableFuture<DatagramPacket> gamePortPacketFuture;
  private CompletableFuture<ConnectivityStateMessage> connectivityStateFuture;
  private Integer publicPort;

  public ConnectivityCheckTask() {
    super(Priority.LOW);
  }

  public void setDatagramGateway(DatagramGateway datagramGateway) {
    this.datagramGateway = datagramGateway;
  }

  public int getPublicPort() {
    return publicPort;
  }

  public void setPublicPort(int publicPort) {
    this.publicPort = publicPort;
  }

  private void onConnectivityStateMessage(GpgServerMessage message) {
    if (message.getTarget() != MessageTarget.CONNECTIVITY) {
      return;
    }

    switch (message.getMessageType()) {
      case SEND_NAT_PACKET:
        // The server did not receive the expected response and wants us to send a UDP packet in order hole punch the
        // NAT. This is done by connectivity service.
        gamePortPacketFuture.cancel(true);
        break;

      case CONNECTIVITY_STATE:
        // The server tells us what our connectivity state is, we're done
        gamePortPacketFuture.cancel(true);
        connectivityStateFuture.complete((ConnectivityStateMessage) message);
        break;
    }
  }

  @Override
  protected ConnectivityStateMessage call() throws Exception {
    Assert.checkNullIllegalState(publicPort, "publicPort has not been set");

    updateTitle(i18n.get("portCheckTask.title"));

    connectivityStateFuture = new CompletableFuture<>();

    Consumer<GpgServerMessage> connectivityStateMessageListener = this::onConnectivityStateMessage;
    fafService.addOnMessageListener(GpgServerMessage.class, connectivityStateMessageListener);

    try {
      runTestForPort(publicPort);
      return connectivityStateFuture.get(connectivityCheckTimeout, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      logger.warn("Connectivity state not received from server within " + connectivityCheckTimeout + "ms");
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
      DatagramPacket udpPacket = gamePortPacketFuture.get(connectivityCheckTimeout, TimeUnit.MILLISECONDS);
      String message = new String(udpPacket.getData(), 1, udpPacket.getLength() - 1, US_ASCII);

      logger.info("Received UDP package on port {}: ", port, message);

      InetSocketAddress address = (InetSocketAddress) udpPacket.getSocketAddress();

      ProcessNatPacketMessage processNatPacketMessage = new ProcessNatPacketMessage(address, message);
      processNatPacketMessage.setTarget(MessageTarget.CONNECTIVITY);
      fafService.sendGpgGameMessage(processNatPacketMessage);
    } catch (CancellationException e) {
      logger.debug("Waiting for UDP package on public game port has been cancelled");
    } catch (TimeoutException e) {
      logger.debug("Waiting for UDP package on public game port timed out");
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private CompletableFuture<DatagramPacket> listenForPackage() {
    CompletableFuture<DatagramPacket> future = new CompletableFuture<>();
    Consumer<DatagramPacket> complete = future::complete;

    datagramGateway.addOnPacketListener(complete);
    return future.thenComposeAsync(datagramPacket -> {
      datagramGateway.removeOnPacketListener(complete);
      return CompletableFuture.completedFuture(datagramPacket);
    });
  }
}
