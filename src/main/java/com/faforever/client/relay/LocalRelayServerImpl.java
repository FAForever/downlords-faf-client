package com.faforever.client.relay;

import com.faforever.client.game.GameType;
import com.faforever.client.ice.WindowsIceAdapter;
import com.faforever.client.ice.event.GpgGameMessageEvent;
import com.faforever.client.relay.event.GameFullEvent;
import com.faforever.client.relay.event.RehostRequestEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.SdpServerMessage;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Acts as a proxy between the game and the "outside world" (server and peers). See <a
 * href="https://github.com/micheljung/downlords-faf-client/wiki/Application-Design#connection-overview">the wiki
 * page</a> for a graphical explanation.</p> <p>Being a proxy includes rewriting the sender/receiver of all outgoing and
 * incoming packages. Apart from being necessary, this makes us IPv6 compatible.</p>
 */
public class LocalRelayServerImpl implements LocalRelayServer {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Resource
  UserService userService;
  @Resource
  FafService fafService;
  @Resource
  EventBus eventBus;
  @Resource
  WindowsIceAdapter windowsIceAdapter;

  private LobbyMode lobbyMode;

  public LocalRelayServerImpl() {
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @Subscribe
  public void onGpgGameMessage(GpgGameMessageEvent event) {
    GpgGameMessage gpgGameMessage = event.getGpgGameMessage();
    GpgClientCommand command = gpgGameMessage.getCommand();

    if (command == GpgClientCommand.REHOST) {
      eventBus.post(new RehostRequestEvent());
      return;
    } else if (command == GpgClientCommand.GAME_FULL) {
      eventBus.post(new GameFullEvent());
      return;
    }

    fafService.sendGpgGameMessage(gpgGameMessage);
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    fafService.addOnMessageListener(GpgServerMessage.class, this::writeToFa);
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    fafService.addOnMessageListener(HostGameMessage.class, (hostGameMessage) -> windowsIceAdapter.hostGame(hostGameMessage.getMap()));
    fafService.addOnMessageListener(JoinGameMessage.class, (joinGameMessage) -> windowsIceAdapter.joinGame(joinGameMessage.getUsername(), joinGameMessage.getPeerUid()));
    fafService.addOnMessageListener(ConnectToPeerMessage.class, (connectToPeerMessage) -> windowsIceAdapter.connectToPeer(connectToPeerMessage.getUsername(), connectToPeerMessage.getPeerUid()));
    fafService.addOnMessageListener(DisconnectFromPeerMessage.class, (disconnectFromPeerMessage) -> windowsIceAdapter.disconnectFromPeer(disconnectFromPeerMessage.getUid()));
    fafService.addOnMessageListener(SdpServerMessage.class, sdpServerMessage -> windowsIceAdapter.setSdp(sdpServerMessage.getSender(), sdpServerMessage.getRecord()));
  }

  private void writeToFa(GpgServerMessage gpgServerMessage) {
    windowsIceAdapter.sendToGpgNet(gpgServerMessage.getMessageType().getString(), gpgServerMessage.getArgs());
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessage gameLaunchMessage) {
    if (GameType.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  @Override
  public CompletableFuture<Void> start() {
    return windowsIceAdapter.start();
  }

  @Override
  public void stop() {
    windowsIceAdapter.stop();
  }
}
