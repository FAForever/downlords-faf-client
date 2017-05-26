package com.faforever.client.fa.relay.ice;

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
import com.faforever.client.remote.domain.IceServersServerMessage;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.nbarraille.jjsonrpc.JJsonPeer;
import com.nbarraille.jjsonrpc.TcpClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bridj.Platform;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.faforever.client.os.OsUtils.gobbleLines;
import static java.util.Arrays.asList;

@Component
@Lazy
@Slf4j
public class IceAdapterImpl implements IceAdapter {

  private static final int CONNECTION_ATTEMPTS = 5;
  private static final boolean ICE_ADAPTER_ENABLED = false;

  private final ApplicationContext applicationContext;
  private final PlayerService playerService;
  private final EventBus eventBus;
  private final FafService fafService;
  private final IceAdapterApi iceAdapterProxy;

  private CompletableFuture<Integer> iceAdapterClientFuture;
  private Process process;
  private LobbyMode lobbyInitMode;
  private JJsonPeer peer;

  @Inject
  public IceAdapterImpl(ApplicationContext applicationContext, PlayerService playerService,
                        EventBus eventBus, FafService fafService) {
    this.applicationContext = applicationContext;
    this.playerService = playerService;
    this.eventBus = eventBus;
    this.fafService = fafService;

    iceAdapterProxy = newIceAdapterProxy();
  }

  @PostConstruct
  void postConstruct() {
    eventBus.register(this);
    fafService.addOnMessageListener(JoinGameMessage.class, message -> iceAdapterProxy.joinGame(message.getUsername(), message.getPeerUid()));
    fafService.addOnMessageListener(HostGameMessage.class, message -> iceAdapterProxy.hostGame(message.getMap()));
    // FIXME add when enabling ICE
//      fafService.addOnMessageListener(ConnectToPeerMessage.class, (message) -> iceAdapterProxy.connectToPeer(message.getUsername(), message.getPeerUid(), message.isOffer()));
    fafService.addOnMessageListener(GameLaunchMessage.class, this::updateLobbyModeFromGameInfo);
  }

  @SneakyThrows
  private List<Map<String, String>> toIceServers(List<IceServersServerMessage.IceServer> iceServers) {
    return iceServers.stream()
        .map(this::toIceServer)
        .collect(Collectors.toList());
  }

  @NotNull
  private Map<String, String> toIceServer(IceServersServerMessage.IceServer iceServer) {
    Map<String, String> map = new HashMap<>();
    map.put("url", iceServer.getUrl());

    if (iceServer.getCredential() != null) {
      map.put("credential", iceServer.getCredential());
      map.put("credentialType", iceServer.getCredentialType());
    }
    if (iceServer.getUsername() != null) {
      map.put("username", iceServer.getUsername());
    }
    return map;
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
    }
    if (command == GpgClientCommand.GAME_FULL) {
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

      Player currentPlayer = playerService.getCurrentPlayer()
          .orElseThrow(() -> new IllegalStateException("Player has not been set"));


      Path workDirectory = Paths.get(nativeDir).toAbsolutePath();
      String[] cmd = new String[]{
          getBinaryName(workDirectory),
          "--id", String.valueOf(currentPlayer.getId()),
          "--login", currentPlayer.getUsername(),
          "--rpc-port", String.valueOf(adapterPort),
          "--gpgnet-port", String.valueOf(gpgPort)
      };

      try {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workDirectory.toFile());
        processBuilder.command(cmd);

        log.debug("Starting ICE adapter with command: {}", asList(cmd));
        process = processBuilder.start();
        Logger logger = LoggerFactory.getLogger("faf-ice-adapter");
        gobbleLines(process.getInputStream(), logger::debug);
        gobbleLines(process.getErrorStream(), logger::error);

        IceAdapterCallbacks iceAdapterCallbacks = applicationContext.getBean(IceAdapterCallbacks.class);

        for (int attempt = 0; attempt < CONNECTION_ATTEMPTS; attempt++) {
          try {
            TcpClient tcpClient = new TcpClient("localhost", adapterPort, iceAdapterCallbacks);
            peer = tcpClient.getPeer();

            setIceServers();
            setLobbyInitMode();
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

  private String getBinaryName(Path workDirectory) {
    return Platform.isWindows() ? workDirectory.resolve("faf-ice-adapter.exe").toString() : "./faf-ice-adapter";
  }

  private void setIceServers() {
    fafService.getIceServers()
        .thenAccept(iceServers -> iceAdapterProxy.setIceServers(toIceServers(iceServers)))
        .exceptionally(throwable -> {
          log.warn("Could not get ICE servers", throwable);
          return null;
        });
  }

  private void setLobbyInitMode() {
    switch (lobbyInitMode) {
      case DEFAULT_LOBBY:
        iceAdapterProxy.setLobbyInitMode("normal");
        break;
      case NO_LOBBY:
        iceAdapterProxy.setLobbyInitMode("auto");
        break;
    }
  }

  private IceAdapterApi newIceAdapterProxy() {
    return (IceAdapterApi) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IceAdapterApi.class},
        (Object proxy, Method method, Object[] args) -> {
          if ("toString".equals(method.getName())) {
            return "ICE adapter proxy";
          }

          List<Object> argList = args == null ? Collections.emptyList() : asList(args);
          if (peer == null || !peer.isAlive() && !"quit".equals(method.getName())) {
            log.warn("Ignoring call to ICE adapter as we are not connected: {}({})", method.getName(), argList);
            return null;
          }
          log.debug("Calling {}({})", method.getName(), argList);
          if (method.getReturnType() == void.class) {
            peer.sendAsyncRequest(method.getName(), argList, null, true);
            return null;
          } else {
            return peer.sendSyncRequest(method.getName(), argList, true);
          }
        }
    );
  }

  private void updateLobbyModeFromGameInfo(GameLaunchMessage gameLaunchMessage) {
    if (KnownFeaturedMod.LADDER_1V1.getTechnicalName().equals(gameLaunchMessage.getMod())) {
      lobbyInitMode = LobbyMode.DEFAULT_LOBBY;
    } else {
      lobbyInitMode = LobbyMode.DEFAULT_LOBBY;
    }
  }

  @Override
  @PreDestroy
  public void stop() {
    Optional.ofNullable(iceAdapterProxy).ifPresent(IceAdapterApi::quit);
    peer = null;
  }
}
