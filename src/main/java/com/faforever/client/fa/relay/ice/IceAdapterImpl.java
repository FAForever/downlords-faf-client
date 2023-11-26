package com.faforever.client.fa.relay.ice;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fa.GameFullNotifier;
import com.faforever.client.mapstruct.IceServerMapper;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.os.OsUtils;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.util.JavaUtil;
import com.faforever.commons.api.dto.CoturnServer;
import com.faforever.commons.lobby.ConnectToPeerGpgCommand;
import com.faforever.commons.lobby.DisconnectFromPeerGpgCommand;
import com.faforever.commons.lobby.GameLaunchResponse;
import com.faforever.commons.lobby.GameType;
import com.faforever.commons.lobby.GpgGameOutboundMessage;
import com.faforever.commons.lobby.HostGameGpgCommand;
import com.faforever.commons.lobby.IceMsgGpgCommand;
import com.faforever.commons.lobby.JoinGameGpgCommand;
import com.nbarraille.jjsonrpc.JJsonPeer;
import com.nbarraille.jjsonrpc.TcpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Component
@Lazy
@Slf4j
@RequiredArgsConstructor
public class IceAdapterImpl implements IceAdapter, InitializingBean, DisposableBean {

  private static final int CONNECTION_ATTEMPTS = 50;
  private static final int CONNECTION_ATTEMPT_DELAY_MILLIS = 250;

  private static final Logger advancedLogger = LoggerFactory.getLogger("faf-ice-adapter-advanced");

  private final OperatingSystem operatingSystem;
  private final PlayerService playerService;
  private final FafServerAccessor fafServerAccessor;
  private final IceServerMapper iceServerMapper;
  private final Preferences preferences;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final ObjectFactory<IceAdapterCallbacks> iceAdapterCallbacksFactory;
  @Lazy
  private final GameFullNotifier gameFullNotifier;

  private final IceAdapterApi iceAdapterProxy = newIceAdapterProxy();
  private GameType gameType;
  private JJsonPeer peer;

  @Override
  public void afterPropertiesSet() {
    fafServerAccessor.addEventListener(JoinGameGpgCommand.class, message -> iceAdapterProxy.joinGame(message.getUsername(), message.getPeerUid()));
    fafServerAccessor.addEventListener(HostGameGpgCommand.class, message -> iceAdapterProxy.hostGame(message.getMap()));
    fafServerAccessor.addEventListener(ConnectToPeerGpgCommand.class, message -> iceAdapterProxy.connectToPeer(message.getUsername(), message.getPeerUid(), message.isOffer()));
    fafServerAccessor.addEventListener(GameLaunchResponse.class, this::updateGameTypeFromGameInfo);
    fafServerAccessor.addEventListener(DisconnectFromPeerGpgCommand.class, message -> iceAdapterProxy.disconnectFromPeer(message.getUid()));
    fafServerAccessor.addEventListener(IceMsgGpgCommand.class, message -> iceAdapterProxy.iceMsg(message.getSender(), message.getRecord()));
  }

  @Override
  public void onIceAdapterStateChanged(String newState) {
    if ("Disconnected".equals(newState)) {
      stop();
    }
  }

  @Override
  public void onGpgGameMessage(GpgGameOutboundMessage message) {
    String command = message.getCommand();

    if (command.equals("GameFull")) {
      gameFullNotifier.onGameFull();
      return;
    }

    fafServerAccessor.sendGpgMessage(message);
  }

  @Override
  public CompletableFuture<Integer> start(int gameId) {
    return CompletableFuture.supplyAsync(() -> {
      Path workDirectory = Path.of(System.getProperty("nativeDir", "lib")).toAbsolutePath();

      int adapterPort;
      int gpgPort;
      try (ServerSocket adapterTestSocket = new ServerSocket(0);
           ServerSocket gpgTestSocket = new ServerSocket(0)) {
        adapterPort = adapterTestSocket.getLocalPort();
        gpgPort = gpgTestSocket.getLocalPort();
      } catch (IOException exception) {
        throw new CompletionException("Unable to find open port for ICE and GPG", exception);
      }

      List<String> cmd = buildCommand(workDirectory, adapterPort, gpgPort, gameId);
      try {
        startIceAdapterProcess(workDirectory, cmd);
      } catch (IOException e) {
        throw new CompletionException(e);
      }

      initializeIceAdapterConnection(adapterPort);

      return gpgPort;
    });
  }

  private void startIceAdapterProcess(Path workDirectory, List<String> cmd) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(workDirectory.toFile());
    processBuilder.command(cmd);
    processBuilder.environment()
        .put("LOG_DIR", operatingSystem.getLoggingDirectory().resolve("iceAdapterLogs").toAbsolutePath().toString());

    log.info("Starting ICE adapter with command: {}", cmd);
    boolean advancedIceLogEnabled = preferences.isAdvancedIceLogEnabled();
    if (advancedIceLogEnabled) {
      advancedLogger.info("\n\n");
    }

    Process process = processBuilder.start();
    process.onExit().thenAccept(finished -> {
      int exitCode = finished.exitValue();
      if (exitCode == 0) {
        log.info("ICE adapter terminated normally");
      } else {
        log.warn("ICE adapter terminated with exit code: {}", exitCode);
      }
    });
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
  }

  @VisibleForTesting
  void initializeIceAdapterConnection(int adapterPort) {
    for (int attempt = 0; attempt < CONNECTION_ATTEMPTS; attempt++) {
      try {
        TcpClient tcpClient = new TcpClient("localhost", adapterPort, iceAdapterCallbacksFactory.getObject());
        peer = tcpClient.getPeer();

        setLobbyInitMode();
        break;
      } catch (IOException e) {
        log.warn("Could not connect to ICE adapter (attempt {}/{})", attempt + 1, CONNECTION_ATTEMPTS);
      }

      // Wait as the socket fails too fast on unix/linux not giving the adapter enough time to start
      try {
        Thread.sleep(CONNECTION_ATTEMPT_DELAY_MILLIS);
      } catch (InterruptedException e) {
        log.warn("Error while waiting for ice adapter", e);
      }
    }
  }

  @VisibleForTesting
  List<String> buildCommand(Path workDirectory, int adapterPort, int gpgPort, int gameId) {
    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    String classpath = getBinaryName(workDirectory) + JavaUtil.CLASSPATH_SEPARATOR + getJavaFXClassPathJars();

    List<String> cmd = new ArrayList<>();
    cmd.add(operatingSystem.getJavaExecutablePath()
        .toAbsolutePath()
        .toString());

    if (!forgedAlliancePrefs.isAllowIpv6()) {
      cmd.add("-Dorg.ice4j.ipv6.DISABLED=true");
    }

    List<String> standardIceOptions = List.of(
        "-cp", classpath,
        "com.faforever.iceadapter.IceAdapter",
        "--id", String.valueOf(currentPlayer.getId()),
        "--game-id", String.valueOf(gameId),
        "--login", currentPlayer.getUsername(),
        "--rpc-port", String.valueOf(adapterPort),
        "--gpgnet-port", String.valueOf(gpgPort));

    cmd.addAll(standardIceOptions);

    if (forgedAlliancePrefs.isShowIceAdapterDebugWindow()) {
      cmd.add("--debug-window");
      cmd.add("--info-window");
    }

    return cmd;
  }

  private String getJavaFXClassPathJars() {
    return JavaUtil.CLASS_PATH_LIST.stream()
        .filter(s -> s.contains("javafx-"))
        .collect(Collectors.joining(JavaUtil.CLASSPATH_SEPARATOR));
  }

  private String getBinaryName(Path workDirectory) {
    return workDirectory.resolve("faf-ice-adapter.jar").toString();
  }

  @Override
  public void setIceServers(Collection<CoturnServer> coturnServers) {
    iceAdapterProxy.setIceServers(iceServerMapper.map(coturnServers));
  }

  @VisibleForTesting
  void setLobbyInitMode() {
    if (gameType == GameType.MATCHMAKER) {
      iceAdapterProxy.setLobbyInitMode("auto");
    } else {
      iceAdapterProxy.setLobbyInitMode("normal");
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
            log.info("Ignoring call to ICE adapter as we are not connected: {}({})", method.getName(), argList);
            return null;
          }
          log.trace("Calling {}({})", method.getName(), argList);
          if (method.getReturnType() == void.class) {
            peer.sendAsyncRequest(method.getName(), argList, null, true);
            return null;
          } else {
            return peer.sendSyncRequest(method.getName(), argList, true);
          }
        }
    );
  }

  @VisibleForTesting
  void updateGameTypeFromGameInfo(GameLaunchResponse gameLaunchMessage) {
    gameType = gameLaunchMessage.getGameType();
  }

  @Override
  public void destroy() {
    stop();
  }

  @Override
  public void stop() {
    iceAdapterProxy.quit();
    peer = null;
  }

  @Override
  public void onGameCloseRequested() {
    stop();
  }
}
