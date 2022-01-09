package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.relay.event.CloseGameEvent;
import com.faforever.client.fa.relay.event.GameFullEvent;
import com.faforever.client.fa.relay.event.RehostRequestEvent;
import com.faforever.client.fa.relay.ice.event.GpgOutboundMessageEvent;
import com.faforever.client.fa.relay.ice.event.IceAdapterStateChanged;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.os.OsUtils;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.IceServer;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.faforever.commons.lobby.LobbyMode;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.nbarraille.jjsonrpc.JJsonPeer;
import com.nbarraille.jjsonrpc.TcpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Arrays.asList;

@Component
@Lazy
@Slf4j
@RequiredArgsConstructor
public class IceAdapterImpl implements IceAdapter, InitializingBean, DisposableBean {

  private static final int CONNECTION_ATTEMPTS = 50;
  private static final int CONNECTION_ATTEMPT_DELAY_MILLIS = 100;

  private static final Logger advancedLogger = LoggerFactory.getLogger("faf-ice-adapter-advanced");

  private final ApplicationContext applicationContext;
  private final ClientProperties clientProperties;
  private final PlayerService playerService;
  private final EventBus eventBus;
  private final FafServerAccessor fafServerAccessor;
  private final PreferencesService preferencesService;

  private final IceAdapterApi iceAdapterProxy = newIceAdapterProxy();
  private CompletableFuture<Integer> iceAdapterClientFuture;
  private Process process;
  private LobbyMode lobbyInitMode;
  private JJsonPeer peer;

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
    fafServerAccessor.addEventListener(JoinGameGpgCommand.class, message -> iceAdapterProxy.joinGame(message.getUsername(), message.getPeerUid()));
    fafServerAccessor.addEventListener(HostGameGpgCommand.class, message -> iceAdapterProxy.hostGame(message.getMap()));
    fafServerAccessor.addEventListener(ConnectToPeerGpgCommand.class, message -> iceAdapterProxy.connectToPeer(message.getUsername(), message.getPeerUid(), message.isOffer()));
    fafServerAccessor.addEventListener(GameLaunchResponse.class, this::updateLobbyModeFromGameInfo);
    fafServerAccessor.addEventListener(DisconnectFromPeerGpgCommand.class, message -> iceAdapterProxy.disconnectFromPeer(message.getUid()));
    fafServerAccessor.addEventListener(IceMsgGpgCommand.class, message -> iceAdapterProxy.iceMsg(message.getSender(), message.getRecord()));
  }

  /**
   * Converts an incoming ice server message to a list of ice servers
   *
   * @return the resulting list of ice servers, each ice server maps from key (e.g. username, credential, url(s)) ->
   * value where value can can be a string or list of strings
   */
  private List<Map<String, Object>> toIceServers(Collection<IceServer> iceServers) {
    List<Map<String, Object>> result = new LinkedList<>();
    for (IceServer iceServer : iceServers) {
      Map<String, Object> map = new HashMap<>();
      List<String> urls = new LinkedList<>();
      if (iceServer.getUrl() != null && !iceServer.getUrl().equals("null")) {
        urls.add(iceServer.getUrl());
      }
      if (iceServer.getUrls() != null) {
        urls.addAll(iceServer.getUrls());
      }

      map.put("urls", urls);

      map.put("credential", iceServer.getCredential());
      map.put("credentialType", "token");
      map.put("username", iceServer.getUsername());

      result.add(map);
    }

    return (result);
  }

  @Subscribe
  public void onIceAdapterStateChanged(IceAdapterStateChanged event) {
    if ("Disconnected".equals(event.getNewState())) {
      iceAdapterProxy.quit();
    }
  }

  @Subscribe
  public void onGpgGameMessage(GpgOutboundMessageEvent event) {
    GpgGameOutboundMessage gpgMessage = event.getGpgMessage();
    String command = gpgMessage.getCommand();

    if (command.equals("Rehost")) {
      eventBus.post(new RehostRequestEvent());
      return;
    }
    if (command.equals("GameFull")) {
      eventBus.post(new GameFullEvent());
      return;
    }

    fafServerAccessor.sendGpgMessage(gpgMessage);
  }

  @Override
  public CompletableFuture<Integer> start() {
    iceAdapterClientFuture = new CompletableFuture<>();
    Thread thread = new Thread(() -> {
      String nativeDir = System.getProperty("nativeDir", "lib");

      int adapterPort = SocketUtils.findAvailableTcpPort();
      int gpgPort = SocketUtils.findAvailableTcpPort();

      PlayerBean currentPlayer = playerService.getCurrentPlayer();

      Path workDirectory = Path.of(nativeDir).toAbsolutePath();

      List<String> cmd = Lists.newArrayList(
          Path.of(System.getProperty("java.home")).resolve("bin").resolve(org.bridj.Platform.isWindows() ? "java.exe" : "java").toAbsolutePath().toString(),
          "-jar",
          getBinaryName(workDirectory),
          "--id", String.valueOf(currentPlayer.getId()),
          "--login", currentPlayer.getUsername(),
          "--rpc-port", String.valueOf(adapterPort),
          "--gpgnet-port", String.valueOf(gpgPort)
      );

      if (preferencesService.getPreferences().getForgedAlliance().isForceRelay()) {
        cmd.add("--force-relay");
        log.warn("Forcing ice adapter relay connection");
      }

      if (clientProperties.isShowIceAdapterDebugWindow()) {
        cmd.add("--debug-window");
        cmd.add("--info-window");
      }

      try {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(workDirectory.toFile());
        processBuilder.command(cmd);
        processBuilder.environment().put("LOG_DIR", LoggingService.FAF_ICE_LOG_DIRECTORY.toAbsolutePath().toString());

        log.info("Starting ICE adapter with command: {}", cmd);
        boolean advancedIceLogEnabled = preferencesService.getPreferences().isAdvancedIceLogEnabled();
        if (advancedIceLogEnabled) {
          advancedLogger.info("\n\n");
        }
        process = processBuilder.start();
        OsUtils.gobbleLines(process.getInputStream(), msg -> {
          if (advancedIceLogEnabled) {
            advancedLogger.info(msg);
          }
        });
        OsUtils.gobbleLines(process.getErrorStream(), msg -> {
          if (advancedIceLogEnabled) {
            advancedLogger.error(msg);
          }
        });

        IceAdapterCallbacks iceAdapterCallbacks = applicationContext.getBean(IceAdapterCallbacks.class);

        for (int attempt = 0; attempt < CONNECTION_ATTEMPTS; attempt++) {
          try {
            TcpClient tcpClient = new TcpClient("localhost", adapterPort, iceAdapterCallbacks);
            peer = tcpClient.getPeer();

            setIceServers();
            setLobbyInitMode();
            break;
          } catch (ConnectException e) {
            log.debug("Could not connect to ICE adapter (attempt {}/{})", attempt + 1, CONNECTION_ATTEMPTS);
          }

          // Wait as the socket fails too fast on unix/linux not giving the adapter enough time to start
          try {
            Thread.sleep(CONNECTION_ATTEMPT_DELAY_MILLIS);
          } catch (InterruptedException e) {
            log.warn("Error while waiting for ice adapter", e);
          }
        }

        iceAdapterClientFuture.complete(gpgPort);

        int exitCode = process.waitFor();
        if (exitCode == 0) {
          log.info("ICE adapter terminated normally");
        } else {
          log.warn("ICE adapter terminated with exit code: {}", exitCode);
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
    return workDirectory.resolve("faf-ice-adapter.jar").toString();
  }

  private void setIceServers() {
    fafServerAccessor.getIceServers()
        .thenAccept(iceServers -> iceAdapterProxy.setIceServers(toIceServers(iceServers)))
        .exceptionally(throwable -> {
          log.warn("Could not get ICE servers", throwable);
          return null;
        });
  }

  private void setLobbyInitMode() {
    switch (lobbyInitMode) {
      case DEFAULT_LOBBY -> iceAdapterProxy.setLobbyInitMode("normal");
      case AUTO_LOBBY -> iceAdapterProxy.setLobbyInitMode("auto");
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

  private void updateLobbyModeFromGameInfo(GameLaunchResponse gameLaunchMessage) {
    // TODO: Replace with game type. Needs https://github.com/FAForever/server/issues/685
    lobbyInitMode = gameLaunchMessage.getLobbyMode();
  }

  @Override
  public void destroy() {
    stop();
  }

  public void stop() {
    Optional.ofNullable(iceAdapterProxy).ifPresent(IceAdapterApi::quit);
    peer = null;
  }

  @Subscribe
  public void onGameCloseRequested(CloseGameEvent event) {
    stop();
  }
}
