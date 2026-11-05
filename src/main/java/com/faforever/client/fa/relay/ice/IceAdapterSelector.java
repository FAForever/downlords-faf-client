package com.faforever.client.fa.relay.ice;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Picks the network for each game: the game's own links on the fafdevelop mod when the player turned them on, the Java
 * adapter otherwise. All players of a fafdevelop game run the same exe, so nobody else's client needs to know.
 */
@Component
@Primary
@Lazy
@Slf4j
@RequiredArgsConstructor
public class IceAdapterSelector implements IceAdapter {

  private final IceAdapterImpl javaAdapter;
  private final NativeIceAdapter nativeAdapter;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private volatile IceAdapter active;

  private IceAdapter active() {
    return active != null ? active : javaAdapter;
  }

  @Override
  public void prepare(GameLaunchResponse gameLaunchResponse) {
    boolean useNative = forgedAlliancePrefs.isNativeNetwork() && KnownFeaturedMod.FAF_DEVELOP.getTechnicalName()
                                                                                             .equals(gameLaunchResponse.getFeatureMod());
    active = useNative ? nativeAdapter : javaAdapter;
    log.info("Network for game {}: {}", gameLaunchResponse.getUid(), useNative ? "native" : "Java adapter");
    active.prepare(gameLaunchResponse);
  }

  @Override
  public List<String> gameArguments() {
    return active().gameArguments();
  }

  @Override
  public void onIceAdapterStateChanged(String newState) {
    active().onIceAdapterStateChanged(newState);
  }

  @Override
  public void onGpgGameMessage(GpgGameOutboundMessage message) {
    active().onGpgGameMessage(message);
  }

  @Override
  public CompletableFuture<Integer> start(int gameId, boolean forceRelay) {
    return active().start(gameId, forceRelay);
  }

  @Override
  public void stop() {
    active().stop();
  }

  @Override
  public void setIceServers(Collection<CoturnServer> coturnServers) {
    active().setIceServers(coturnServers);
  }

  @Override
  public boolean canRejoin() {
    return active().canRejoin();
  }

  @Override
  public List<String> rejoinArguments() {
    return active().rejoinArguments();
  }
}
