package com.faforever.client.fa.relay.ice;

import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GpgGameOutboundMessage;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Starts or stops the ICE adapter process.
 */
public interface IceAdapter {

  int start(int gameId, int clientGpgPort, String accessToken);

  void stop();
}
