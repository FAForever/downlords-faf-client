package com.faforever.client.fa.relay.ice;

import com.faforever.client.fa.relay.ConnectToPeerMessage;
import com.faforever.client.fa.relay.DisconnectFromPeerMessage;
import com.faforever.client.fa.relay.GpgClientCommand;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.fa.relay.HostGameMessage;
import com.faforever.client.fa.relay.JoinGameMessage;
import com.faforever.client.fa.relay.LobbyMode;
import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.event.GpgGameMessageEvent;
import com.faforever.client.fa.relay.ice.event.IceAdapterStateChanged;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafService;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.SdpRecordServerMessage;
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
import javax.annotation.PreDestroy;
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
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    fafService.addOnMessageListener(HostGameMessage.class, (hostGameMessage) -> iceAdapterProxy.hostGame(hostGameMessage.getMap()));
    fafService.addOnMessageListener(JoinGameMessage.class, (joinGameMessage) -> iceAdapterProxy.joinGame(joinGameMessage.getUsername(), joinGameMessage.getPeerUid()));
    fafService.addOnMessageListener(ConnectToPeerMessage.class, (connectToPeerMessage) -> iceAdapterProxy.connectToPeer(connectToPeerMessage.getUsername(), connectToPeerMessage.getPeerUid()));
    fafService.addOnMessageListener(DisconnectFromPeerMessage.class, (disconnectFromPeerMessage) -> iceAdapterProxy.disconnectFromPeer(disconnectFromPeerMessage.getUid()));
    fafService.addOnMessageListener(SdpRecordServerMessage.class, sdpRecordServerMessage -> iceAdapterProxy.setSdp(sdpRecordServerMessage.getSender(), sdpRecordServerMessage.getRecord()));
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

    if (command == GpgClientCommand.REHOST) {
      eventBus.post(new RehostRequestEvent());
      return;
    } else if (command == GpgClientCommand.GAME_FULL) {
      eventBus.post(new GameFullEvent());
      return;
    }

    fafService.sendGpgGameMessage(gpgGameMessage);
  }

  @Override
  public CompletableFuture<Integer> start() {
    iceAdapterClientFuture = new CompletableFuture<>();
    Thread thread = new Thread(() -> {
      // TODO rename to nativeDir and reuse in UID service
      String uidDir = System.getProperty("uid.dir", "lib");

      int adapterPort = SocketUtils.findAvailableTcpPort();
      int gpgPort = SocketUtils.findAvailableTcpPort();

      Player currentPlayer = playerService.getCurrentPlayer();
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
        Logger logger = LoggerFactory.getLogger("faf-ice-adapter");
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
    });
    thread.setDaemon(true);
    thread.start();

    return iceAdapterClientFuture;
  }

  private IceAdapterApi newIceAdapterProxy(JJsonPeer peer) {
    return (IceAdapterApi) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IceAdapterApi.class},
        (Object proxy, Method method, Object[] args) -> {
          if ("toString".equals(method.getName())) {
            return "ICE adapter proxy";
          }
          List<Object> argList = args == null ? Collections.emptyList() : asList(args);
          if (!peer.isAlive() && !"quit".equals(method.getName())) {
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
    if (KnownFeaturedMod.LADDER_1V1.getString().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  @Override
  @PreDestroy
  public void stop() {
    iceAdapterProxy.quit();
  }
}
