package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Ice;
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
import com.faforever.client.remote.domain.IceServerMessage;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.nbarraille.jjsonrpc.JJsonPeer;
import com.nbarraille.jjsonrpc.TcpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.os.OsUtils.gobbleLines;
import static java.util.Arrays.asList;

@Component
@Lazy
public class IceAdapterImpl implements IceAdapter {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final int CONNECTION_ATTEMPTS = 5;
  private static final boolean ICE_ADAPTER_ENABLED = false;

  // TODO ask muellni to accept these values
  private final String stunServerAddress;
  private final String turnServerAddress;
  private final ApplicationContext applicationContext;
  private final PlayerService playerService;
  private final EventBus eventBus;
  private final FafService fafService;

  private CompletableFuture<Integer> iceAdapterClientFuture;
  private Process process;
  private IceAdapterApi iceAdapterProxy;

  // TODO pass to ICE adapter
  private LobbyMode lobbyMode;

  @Inject
  public IceAdapterImpl(ClientProperties clientProperties, ApplicationContext applicationContext, PlayerService playerService,
                        EventBus eventBus, FafService fafService) {
    Ice ice = clientProperties.getIce();
    this.stunServerAddress = ice.getStun().getHost();
    this.turnServerAddress = ice.getTurn().getHost();

    this.applicationContext = applicationContext;
    this.playerService = playerService;
    this.eventBus = eventBus;
    this.fafService = fafService;

    lobbyMode = LobbyMode.DEFAULT_LOBBY;
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
    if (ICE_ADAPTER_ENABLED) {
      fafService.addOnMessageListener(HostGameMessage.class, (message) -> iceAdapterProxy.hostGame(message.getMap()));
      fafService.addOnMessageListener(JoinGameMessage.class, (message) -> iceAdapterProxy.joinGame(message.getUsername(), message.getPeerUid()));
      // FIXME add when enabling ICE
//      fafService.addOnMessageListener(ConnectToPeerMessage.class, (message) -> iceAdapterProxy.connectToPeer(message.getUsername(), message.getPeerUid(), message.isOffer()));
      fafService.addOnMessageListener(DisconnectFromPeerMessage.class, (message) -> iceAdapterProxy.disconnectFromPeer(message.getUid()));
      fafService.addOnMessageListener(IceServerMessage.class, message -> iceAdapterProxy.iceMsg(message.getSender(), message.getRecord()));
    }
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
      String nativeDir = System.getProperty("nativeDir", "lib");

      int adapterPort = SocketUtils.findAvailableTcpPort();
      int gpgPort = SocketUtils.findAvailableTcpPort();
      int lobbyPort = SocketUtils.findAvailableUdpPort();

      Player currentPlayer = playerService.getCurrentPlayer()
          .orElseThrow(() -> new IllegalStateException("Player has not been set"));

      Path workDirectory = Paths.get(nativeDir, "faf-ice-adapter");
      String[] cmd = new String[]{
          // FIXME make linux compatible
          workDirectory.resolve("node.exe").toString(), "faf-ice-adapter.js",
          "--id", String.valueOf(currentPlayer.getId()),
          "--login", currentPlayer.getUsername(),
          "--rpc_port", String.valueOf(adapterPort),
          "--gpgnet_port", String.valueOf(gpgPort),
          "--lobby_port", String.valueOf(lobbyPort),
      };

      try {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workDirectory.toFile());
        processBuilder.command(cmd);

        logger.debug("Starting ICE adapter with command: {}", asList(cmd));
        process = processBuilder.start();
        Logger logger = LoggerFactory.getLogger("faf-ice-adapter");
        gobbleLines(process.getInputStream(), logger::debug);
        gobbleLines(process.getErrorStream(), logger::error);

        IceAdapterCallbacks iceAdapterCallbacks = applicationContext.getBean(IceAdapterCallbacks.class);

        for (int attempt = 0; attempt < CONNECTION_ATTEMPTS; attempt++) {
          try {
            TcpClient tcpClient = new TcpClient("localhost", adapterPort, iceAdapterCallbacks);
            iceAdapterProxy = newIceAdapterProxy(tcpClient.getPeer());
            break;
          } catch (ConnectException e) {
            logger.debug("Could not connect to ICE adapter (attempt {}/{})", attempt, CONNECTION_ATTEMPTS);
          }
        }

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
    if (KnownFeaturedMod.LADDER_1V1.getTechnicalName().equals(gameLaunchMessage.getMod())) {
      lobbyMode = LobbyMode.NO_LOBBY;
    } else {
      lobbyMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  @Override
  @PreDestroy
  public void stop() {
    Optional.ofNullable(iceAdapterProxy).ifPresent(IceAdapterApi::quit);
  }
}
