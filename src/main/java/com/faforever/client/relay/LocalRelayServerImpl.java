package com.faforever.client.relay;

import com.faforever.client.game.GameType;
import com.faforever.client.ice.IceAdapterClient;
import com.faforever.client.ice.event.GpgGameMessageEvent;
import com.faforever.client.relay.event.GameFullEvent;
import com.faforever.client.relay.event.RehostRequestEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.user.UserService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.SocketUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;

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
  IceAdapterClient iceAdapterClient;

  private LobbyMode lobbyMode;

  public LocalRelayServerImpl() {
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @Subscribe
  public void onGpgGameMessage(GpgGameMessageEvent event) {
    GpgGameMessage gpgGameMessage = event.getGpgGameMessage();
    GpgClientCommand command = gpgGameMessage.getCommand();

    if (isIdleLobbyMessage(gpgGameMessage)) {
      String username = userService.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      int faGamePort = SocketUtils.findAvailableUdpPort();
      logger.debug("Picked port for FA to listen: {}", faGamePort);

      writeToFa(new CreateLobbyServerMessage(lobbyMode, faGamePort, username, userService.getUid(), 1));
    } else if (command == GpgClientCommand.REHOST) {
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
    fafService.addOnMessageListener(GpgServerMessage.class, this::writeToFa);
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    fafService.addOnMessageListener(JoinGameMessage.class, (joinGameMessage) -> iceAdapterClient.joinGame(joinGameMessage.getUsername(), joinGameMessage.getPeerUid()));
    fafService.addOnMessageListener(ConnectToPeerMessage.class, (connectToPeerMessage) -> iceAdapterClient.connectToPeer(connectToPeerMessage.getUsername(), connectToPeerMessage.getPeerUid()));
    fafService.addOnMessageListener(DisconnectFromPeerMessage.class, (disconnectFromPeerMessage) -> iceAdapterClient.disconnectFromPeer(disconnectFromPeerMessage.getUid()));
  }

  /**
   * Returns {@code true} if the game lobby is "idle", which basically means the game has been started (into lobby) and
   * does now need to be told on which port to listen on.
   */
  private boolean isIdleLobbyMessage(GpgGameMessage gpgGameMessage) {
    return gpgGameMessage.getCommand() == GpgClientCommand.GAME_STATE
        && gpgGameMessage.getArgs().get(0).equals("Idle");
  }

  private void writeToFa(GpgServerMessage gpgServerMessage) {
    iceAdapterClient.sendToGpgNet(gpgServerMessage.getMessageType().getString(), gpgServerMessage.getArgs());
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessage gameLaunchMessage) {
    if (GameType.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }
}
