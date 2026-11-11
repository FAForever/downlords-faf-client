package com.faforever.client.fa.relay.ice;

import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class IceAdapterTest {

  /** Only what every adapter must implement; the rest comes from the interface's defaults. */
  private final IceAdapter plain = new IceAdapter() {
    @Override
    public void onIceAdapterStateChanged(String newState) {}

    @Override
    public void onGpgGameMessage(GpgGameOutboundMessage message) {}

    @Override
    public CompletableFuture<Integer> start(int gameId, boolean forceRelay) {
      return CompletableFuture.completedFuture(0);
    }

    @Override
    public void stop() {}

    @Override
    public void setIceServers(Collection<CoturnServer> coturnServers) {}
  };

  @Test
  public void anAdapterWithoutItsOwnNetworkAddsNothingAndCantBringAGameBack() {
    plain.prepare(mock(GameLaunchResponse.class));

    assertEquals(List.of(), plain.gameArguments());
    assertFalse(plain.canRejoin());
    assertEquals(List.of(), plain.rejoinArguments());
  }
}
