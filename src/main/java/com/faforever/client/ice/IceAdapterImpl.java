package com.faforever.client.ice;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.FeaturedMod;
import com.faforever.client.ice.event.GpgGameMessageEvent;
import com.faforever.client.ice.event.IceAdapterStateChanged;
import com.faforever.client.player.PlayerService;
import com.faforever.client.relay.ConnectToPeerMessage;
import com.faforever.client.relay.CreateLobbyServerMessage;
import com.faforever.client.relay.DisconnectFromPeerMessage;
import com.faforever.client.relay.GpgClientCommand;
import com.faforever.client.relay.GpgGameMessage;
import com.faforever.client.relay.GpgServerMessage;
import com.faforever.client.relay.HostGameMessage;
import com.faforever.client.relay.JoinGameMessage;
import com.faforever.client.relay.LobbyMode;
import com.faforever.client.relay.event.GameFullEvent;
import com.faforever.client.relay.event.RehostRequestEvent;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.SdpServerMessage;
import com.faforever.client.util.Assert;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.nbarraille.jjsonrpc.JJsonPeer;
import com.nbarraille.jjsonrpc.TcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.SocketUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.os.OsUtils.gobbleLines;
import static java.util.Arrays.asList;

public class IceAdapterImpl implements IceAdapter {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Value("${stun.host}")
  String stunServerAddress;

  @Value("${turn.host}")
  String turnServerAddress;

  @Resource
  ApplicationContext applicationContext;
  @Resource
  PlayerService playerService;
  @Resource
  EventBus eventBus;
  @Resource
  FafService fafService;

  private CompletableFuture<Integer> iceAdapterClientFuture;
  private Process process;
  private IceAdapterApi iceAdapterProxy;

  private LobbyMode lobbyMode;

  public IceAdapterImpl() {
    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    fafService.addOnMessageListener(GpgServerMessage.class, this::writeToFa);
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    fafService.addOnMessageListener(HostGameMessage.class, (hostGameMessage) -> iceAdapterProxy.hostGame(hostGameMessage.getMap()));
    fafService.addOnMessageListener(JoinGameMessage.class, (joinGameMessage) -> iceAdapterProxy.joinGame(joinGameMessage.getUsername(), joinGameMessage.getPeerUid()));
    fafService.addOnMessageListener(ConnectToPeerMessage.class, (connectToPeerMessage) -> iceAdapterProxy.connectToPeer(connectToPeerMessage.getUsername(), connectToPeerMessage.getPeerUid()));
    fafService.addOnMessageListener(DisconnectFromPeerMessage.class, (disconnectFromPeerMessage) -> iceAdapterProxy.disconnectFromPeer(disconnectFromPeerMessage.getUid()));
    fafService.addOnMessageListener(SdpServerMessage.class, sdpServerMessage -> iceAdapterProxy.setSdp(sdpServerMessage.getSender(), sdpServerMessage.getRecord()));
  }

  private void writeToFa(GpgServerMessage gpgServerMessage) {
    Assert.checkNullIllegalState(iceAdapterProxy, "Adapter is not ready");
    iceAdapterProxy.sendToGpgNet(gpgServerMessage.getMessageType().getString(), gpgServerMessage.getArgs());
  }

  @Subscribe
  public void onIceAdapterStateChanged(IceAdapterStateChanged event) {
    switch (event.getNewState()) {
      case "Disconnected":
        iceAdapterProxy.quit();
        break;
    }
  }

  @Subscribe
  public void onGpgGameMessage(GpgGameMessageEvent event) {
    GpgGameMessage gpgGameMessage = event.getGpgGameMessage();
    GpgClientCommand command = gpgGameMessage.getCommand();

    if (isIdleLobbyMessage(gpgGameMessage)) {
      PlayerInfoBean currentPlayer = playerService.getCurrentPlayer();
      int playerId = currentPlayer.getId();
      String username = currentPlayer.getUsername();
      if (lobbyMode == null) {
        throw new IllegalStateException("lobbyMode has not been set");
      }

      int faGamePort = SocketUtils.findAvailableUdpPort();
      logger.debug("Picked port for FA to listen: {}", faGamePort);

      writeToFa(new CreateLobbyServerMessage(lobbyMode, faGamePort, username, playerId, 1));
    } else if (command == GpgClientCommand.REHOST) {
      eventBus.post(new RehostRequestEvent());
      return;
    } else if (command == GpgClientCommand.GAME_FULL) {
      eventBus.post(new GameFullEvent());
      return;
    }

    fafService.sendGpgGameMessage(gpgGameMessage);
  }

  /**
   * Returns {@code true} if the game lobby is "idle", which basically means the game has been started (into lobby) and
   * does now need to be told on which port to listen on.
   */
  private boolean isIdleLobbyMessage(GpgGameMessage gpgGameMessage) {
    return gpgGameMessage.getCommand() == GpgClientCommand.GAME_STATE
        && gpgGameMessage.getArgs().get(0).equals("Idle");
  }

  @Override
  public CompletableFuture<Integer> start() {
    iceAdapterClientFuture = new CompletableFuture<>();
    new Thread(() -> {
      // TODO rename to nativeDir and reuse in UID service
      String uidDir = System.getProperty("uid.dir", "lib");

      int adapterPort = SocketUtils.findAvailableTcpPort();
      int gpgPort = SocketUtils.findAvailableTcpPort();

      PlayerInfoBean currentPlayer = playerService.getCurrentPlayer();
      String[] cmd = new String[]{
          // FIXME make linux compatible
          Paths.get(uidDir, "faf-ice-adapter.exe").toAbsolutePath().toString(),
          "-s", stunServerAddress,
          "-t", turnServerAddress,
          "-p", String.valueOf(adapterPort),
          "-i", String.valueOf(currentPlayer.getId()),
          "-l", currentPlayer.getUsername(),
          "-g", String.valueOf(gpgPort)
      };

      try {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmd);

        logger.debug("Starting ICE adapter with command: {}", asList(cmd));
        process = processBuilder.start();
        gobbleLines(process.getInputStream(), logger::debug);
        gobbleLines(process.getErrorStream(), logger::error);

        IceAdapterCallbacks iceAdapterCallbacks = applicationContext.getBean(IceAdapterCallbacks.class);

        TcpClient tcpClient = new TcpClient("localhost", adapterPort, iceAdapterCallbacks);
        iceAdapterProxy = newIceAdapterProxy(tcpClient.getPeer());

        iceAdapterClientFuture.complete(gpgPort);

        int exitCode = process.waitFor();
        if (exitCode == 0) {
          logger.debug("ICE adapter terminated normally");
        } else {
          logger.warn("ICE adapter terminated with exit code: {}", exitCode);
        }
      } catch (Exception e) {
        iceAdapterClientFuture.completeExceptionally(e);
      }
    }).start();

    return iceAdapterClientFuture;
  }

  private IceAdapterApi newIceAdapterProxy(JJsonPeer peer) {
    return (IceAdapterApi) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IceAdapterApi.class},
        (Object proxy, Method method, Object[] args) -> {
          if ("toString".equals(method.getName())) {
            return "ICE adapter proxy";
          }
          List<Object> argList = args == null ? Collections.emptyList() : asList(args);
          if (!peer.isAlive()) {
            logger.warn("Ignoring call to ICE adapter as we are not connected: {}({})", method.getName(), argList);
            return null;
          }
          logger.debug("Calling {}({})", method.getName(), argList);
          peer.sendAsyncRequest(method.getName(), argList, null, false);
          return null;
        }
    );
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessage gameLaunchMessage) {
    if (FeaturedMod.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  @Override
  public void stop() {
    iceAdapterProxy.quit();
  }
}
