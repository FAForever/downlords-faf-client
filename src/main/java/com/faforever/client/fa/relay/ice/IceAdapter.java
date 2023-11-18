package com.faforever.client.fa.relay.ice;

import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GpgGameOutboundMessage;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Starts or stops the ICE adapter process.
 */
public interface IceAdapter {

  void onIceAdapterStateChanged(String newState);

  void onGpgGameMessage(GpgGameOutboundMessage message);

  CompletableFuture<Integer> start(int gameId);

  void stop();

  void setIceServers(Collection<CoturnServer> coturnServers);

  void onGameCloseRequested();
}
