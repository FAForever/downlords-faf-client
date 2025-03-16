package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

@Component
@Lazy
@Slf4j
@RequiredArgsConstructor
public class IceAdapterImpl implements IceAdapter, DisposableBean {

  private final OperatingSystem operatingSystem;
  private final PlayerService playerService;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final ClientProperties clientProperties;

  private Process process;

  @Override
  public int start(int gameId, int clientGpgPort, String accessToken) {
    Path workDirectory = Path.of(System.getProperty("nativeDir", "lib")).toAbsolutePath();

    int gpgPort;
    try (
        ServerSocket gpgTestSocket = new ServerSocket(0)
    ) {
      gpgPort = gpgTestSocket.getLocalPort();
    } catch (IOException exception) {
      throw new CompletionException("Unable to find open port for GPG", exception);
    }

    List<String> cmd = buildCommand(gpgPort, clientGpgPort, gameId, accessToken);
    try {
      startIceAdapterProcess(workDirectory, cmd);
    } catch (IOException e) {
      throw new CompletionException(e);
    }

    return gpgPort;
  }

  private void startIceAdapterProcess(Path workDirectory, List<String> cmd) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(workDirectory.toFile());
    processBuilder.command(cmd);
    processBuilder.environment()
                  .put("LOG_DIR",
                       operatingSystem.getLoggingDirectory().resolve("iceAdapterLogs").toAbsolutePath().toString());

    log.info("Starting ICE adapter with command: {}", cmd);

    process = processBuilder.start();
    process.onExit().thenAccept(finished -> {
      int exitCode = finished.exitValue();
      if (exitCode == 0) {
        log.info("ICE adapter terminated normally");
      } else {
        log.warn("ICE adapter terminated with exit code: {}", exitCode);
      }
    });
  }

  @VisibleForTesting
  List<String> buildCommand(int gpgGamePort, int gpgClientPort, int gameId, String accessToken) {
    PlayerInfo currentPlayer = playerService.getCurrentPlayer();

    List<String> cmd = new ArrayList<>();
    //cmd.add(operatingSystem.getJavaExecutablePath().toAbsolutePath().toString());

    if (!forgedAlliancePrefs.isAllowIpv6()) {
      cmd.add("-Dorg.ice4j.ipv6.DISABLED=true");
    }

    List<String> standardIceOptions = List.of(System.getProperty("PIONEER_BIN_NAME", "pioneer.exe"), "--user-id",
                                              String.valueOf(currentPlayer.getId()), "--game-id",
                                              String.valueOf(gameId), "--gpgnet-port", String.valueOf(gpgGamePort),
                                              "--gpgnet-client-port", String.valueOf(gpgClientPort), "--access-token",
                                              accessToken,
                                              "--api-root", clientProperties.getApi().getBaseUrl() + "/ice");

    cmd.addAll(standardIceOptions);

    return cmd;
  }

  @Override
  public void destroy() {
    stop();
  }

  @Override
  public void stop() {
    process.destroy();
  }
}
