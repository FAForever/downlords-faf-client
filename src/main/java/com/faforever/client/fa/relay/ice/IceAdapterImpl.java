package com.faforever.client.fa.relay.ice;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.os.OperatingSystem;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.IceAdapterPrefs;
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
  private final IceAdapterPrefs iceAdapterPrefs;
  private final ClientProperties clientProperties;
  private final IceAdapterService iceAdapterService;

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

    List<String> cmd = buildCommand(gpgPort, clientGpgPort, gameId, accessToken, iceAdapterPrefs.isForceTurnRelay(),
                                    iceAdapterPrefs.isConsentLogSharing(), iceAdapterPrefs.isEnableDebugLogging());
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

    log.info("Starting ICE adapter with command: {}", maskAccessToken(cmd));

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

  private List<String> maskAccessToken(List<String> cmd) {
    List<String> result = new ArrayList<>(cmd);
    int index = result.indexOf("--access-token") + 1;
    result.set(index, "[redacted]");
    return result;
  }

  @VisibleForTesting
  List<String> buildCommand(int gpgGamePort, int gpgClientPort, int gameId, String accessToken, boolean forceTurnRelay,
                            boolean consentLogSharing, boolean debugLogging) {
    PlayerInfo currentPlayer = playerService.getCurrentPlayer();

    // @formatter:off
    return List.of(iceAdapterService.getExecutablePath().toString(),
                   "--user-id", String.valueOf(currentPlayer.getId()),
                   "--game-id", String.valueOf(gameId),
                   "--gpgnet-port", String.valueOf(gpgGamePort),
                   "--gpgnet-client-port", String.valueOf(gpgClientPort),
                   "--access-token", accessToken,
                   "--api-root", clientProperties.getApi().getBaseUrl() + "/ice",
                   "--force-turn-relay", String.valueOf(forceTurnRelay),
                   "--consent-log-sharing", String.valueOf(consentLogSharing),
                   "--log-level", debugLogging ? "-1" : "0"
    );
    // @formatter:on

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
