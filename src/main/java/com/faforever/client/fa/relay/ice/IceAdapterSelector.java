package com.faforever.client.fa.relay.ice;

import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.RebuiltEngineExecutable;
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
 * Picks the network for each game: the game's own links on the fafdevelop mod when its exe is the rebuilt engine and
 * the player kept the setting on, the Java adapter otherwise. All players of a fafdevelop game run the same exe, so nobody else's client needs to know,
 * and the original exe on fafdevelop keeps the adapter.
 */
@Component
@Primary
@Lazy
@Slf4j
@RequiredArgsConstructor
public class IceAdapterSelector implements IceAdapter {

  private final IceAdapterImpl javaAdapter;
  private final NativeIceAdapter nativeAdapter;
  private final ForgedAllianceLaunchService forgedAllianceLaunchService;
  private final ForgedAlliancePrefs forgedAlliancePrefs;

  private volatile IceAdapter active;
  // The rebuilt engine on fafdevelop with the setting off: the Java adapter, but the game still takes others back.
  private volatile boolean rebuiltEngineOnAdapter;

  private IceAdapter active() {
    return active != null ? active : javaAdapter;
  }

  @Override
  public void prepare(GameLaunchResponse gameLaunchResponse) {
    boolean rebuiltEngineOnFafDevelop = KnownFeaturedMod.FAF_DEVELOP.getTechnicalName().equals(gameLaunchResponse.getFeatureMod())
        && RebuiltEngineExecutable.isRebuiltEngine(forgedAllianceLaunchService.getExecutablePath());
    boolean useNative = rebuiltEngineOnFafDevelop && forgedAlliancePrefs.isUseNativeNetwork();
    rebuiltEngineOnAdapter = rebuiltEngineOnFafDevelop && !useNative;
    active = useNative ? nativeAdapter : javaAdapter;
    log.info("Network for game {}: {}", gameLaunchResponse.getUid(), useNative ? "native" : "Java adapter");
    active.prepare(gameLaunchResponse);
  }

  @Override
  public List<String> gameArguments() {
    if (rebuiltEngineOnAdapter) {
      // A game offers a crashed player a return only when every player's engine announces it takes returning players;
      // this one can't come back itself on the adapter, but it mustn't take the return away from everyone else.
      return List.of("/con", "net_PeerFeatures=1;net_RejoinAccept=1");
    }
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
