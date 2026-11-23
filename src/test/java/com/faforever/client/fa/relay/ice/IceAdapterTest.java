package com.faforever.client.fa.relay.ice;

import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class IceAdapterTest {

  /** Only what every adapter must implement; the rest comes from the interface's defaults. */
  private final IceAdapter plain = new IceAdapter() {
    @Override
    public void onIceAdapterStateChanged(String newState) {
      // Not part of the defaults under test.
    }

    @Override
    public void onGpgGameMessage(GpgGameOutboundMessage message) {
      // Not part of the defaults under test.
    }

    @Override
    public CompletableFuture<Integer> start(int gameId, boolean forceRelay) {
      return CompletableFuture.completedFuture(0);
    }

    @Override
    public void stop() {
      // Not part of the defaults under test.
    }

    @Override
    public void setIceServers(Collection<CoturnServer> coturnServers) {
      // Not part of the defaults under test.
    }

    @Override
    public void hostGame(String mapName) {
      // Not part of the defaults under test.
    }

    @Override
    public void joinGame(String remotePlayerLogin, int remotePlayerId) {
      // Not part of the defaults under test.
    }

    @Override
    public void connectToPeer(String remotePlayerLogin, int remotePlayerId, boolean offer) {
      // Not part of the defaults under test.
    }

    @Override
    public void disconnectFromPeer(int remotePlayerId) {
      // Not part of the defaults under test.
    }

    @Override
    public void iceMsg(int remotePlayerId, Object message) {
      // Not part of the defaults under test.
    }
  };

  @Test
  public void anAdapterWithoutItsOwnNetworkAddsNothing() {
    plain.prepare(mock(GameLaunchResponse.class));

    assertEquals(List.of(), plain.gameArguments());
  }
}
