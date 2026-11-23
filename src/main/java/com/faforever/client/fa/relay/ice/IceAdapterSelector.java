package com.faforever.client.fa.relay.ice;

import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.RebuiltEngineExecutable;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Picks the network for each game: the game's own links when its exe is the rebuilt engine and the player kept the
 * setting on, the Java adapter otherwise. The game's own links connect to players on the Java adapter too, so this
 * needs nothing from the other players. Passes the lobby server's commands for the game to the chosen adapter.
 */
@Component
@Primary
@Lazy
@Slf4j
@RequiredArgsConstructor
public class IceAdapterSelector implements IceAdapter, InitializingBean {

  private final IceAdapterImpl javaAdapter;
  private final NativeIceAdapter nativeAdapter;
  private final ForgedAllianceLaunchService forgedAllianceLaunchService;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final FafServerAccessor fafServerAccessor;

  /** The current game's adapter, from {@link #prepare} until {@link #stop}. */
  private volatile IceAdapter selectedAdapter;

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.getEvents(JoinGameGpgCommand.class)
                     .doOnNext(message -> toGame("JoinGame", adapter -> adapter.joinGame(message.getUsername(),
                                                                                         message.getPeerUid())))
                     .doOnError(throwable -> log.warn("Unable to join game", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(HostGameGpgCommand.class)
                     .doOnNext(message -> toGame("HostGame", adapter -> adapter.hostGame(message.getMap())))
                     .doOnError(throwable -> log.warn("Unable to host game", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(ConnectToPeerGpgCommand.class)
                     .doOnNext(message -> toGame("ConnectToPeer", adapter -> adapter.connectToPeer(message.getUsername(),
                                                                                                   message.getPeerUid(),
                                                                                                   message.isOffer())))
                     .doOnError(throwable -> log.warn("Unable to connect to peer", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(DisconnectFromPeerGpgCommand.class)
                     .doOnNext(message -> toGame("DisconnectFromPeer",
                                                 adapter -> adapter.disconnectFromPeer(message.getUid())))
                     .doOnError(throwable -> log.warn("Unable to disconnect from peer", throwable))
                     .retry()
                     .subscribe();
    fafServerAccessor.getEvents(IceMsgGpgCommand.class)
                     .doOnNext(message -> toGame("IceMsg", adapter -> adapter.iceMsg(message.getSender(),
                                                                                     message.getRecord())))
                     .doOnError(throwable -> log.warn("Unable to ice msg", throwable))
                     .retry()
                     .subscribe();
  }

  /** A lobby server command that arrives after a game ended, or before one starts, is dropped as the adapter did. */
  private void toGame(String command, Consumer<IceAdapter> call) {
    IceAdapter adapter = selectedAdapter;
    if (adapter == null) {
      log.info("Ignoring {} from the server: no game is being started or played", command);
      return;
    }
    call.accept(adapter);
  }

  private IceAdapter selected() {
    IceAdapter adapter = selectedAdapter;
    if (adapter == null) {
      throw new IllegalStateException("No network was prepared for a game");
    }
    return adapter;
  }

  /** Chooses the network for one game. Once per game: the next game can prepare after {@link #stop}. */
  @Override
  public synchronized void prepare(GameLaunchResponse gameLaunchResponse) {
    if (selectedAdapter != null) {
      throw new IllegalStateException("A network was already prepared for a game, not preparing game "
                                      + gameLaunchResponse.getUid());
    }
    boolean useNative = forgedAlliancePrefs.isUseNativeNetwork()
        && RebuiltEngineExecutable.isRebuiltEngine(forgedAllianceLaunchService.getExecutablePath());
    IceAdapter adapter = useNative ? nativeAdapter : javaAdapter;
    log.info("Network for game {}: {}", gameLaunchResponse.getUid(), useNative ? "native" : "Java adapter");
    adapter.prepare(gameLaunchResponse);
    selectedAdapter = adapter;
  }

  @Override
  public List<String> gameArguments() {
    return selected().gameArguments();
  }

  @Override
  public void onIceAdapterStateChanged(String newState) {
    selected().onIceAdapterStateChanged(newState);
  }

  @Override
  public void onGpgGameMessage(GpgGameOutboundMessage message) {
    selected().onGpgGameMessage(message);
  }

  @Override
  public CompletableFuture<Integer> start(int gameId, boolean forceRelay) {
    return selected().start(gameId, forceRelay);
  }

  /** Stops the current game's adapter and ends its selection. Without a prepared game there is nothing to stop. */
  @Override
  public synchronized void stop() {
    IceAdapter adapter = selectedAdapter;
    if (adapter == null) {
      return;
    }
    try {
      adapter.stop();
    } finally {
      selectedAdapter = null;
    }
  }

  @Override
  public void setIceServers(Collection<CoturnServer> coturnServers) {
    selected().setIceServers(coturnServers);
  }

  @Override
  public void hostGame(String mapName) {
    selected().hostGame(mapName);
  }

  @Override
  public void joinGame(String remotePlayerLogin, int remotePlayerId) {
    selected().joinGame(remotePlayerLogin, remotePlayerId);
  }

  @Override
  public void connectToPeer(String remotePlayerLogin, int remotePlayerId, boolean offer) {
    selected().connectToPeer(remotePlayerLogin, remotePlayerId, offer);
  }

  @Override
  public void disconnectFromPeer(int remotePlayerId) {
    selected().disconnectFromPeer(remotePlayerId);
  }

  @Override
  public void iceMsg(int remotePlayerId, Object message) {
    selected().iceMsg(remotePlayerId, message);
  }
}
