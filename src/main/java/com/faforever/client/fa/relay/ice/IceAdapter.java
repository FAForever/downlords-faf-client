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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * Starts or stops the ICE adapter process.
 */
@Component
@Lazy
@Slf4j
@RequiredArgsConstructor
public class IceAdapter implements DisposableBean {

  private final PlayerService playerService;
  private final IceAdapterPrefs iceAdapterPrefs;
  private final ClientProperties clientProperties;
  private final IceAdapterService iceAdapterService;
  private final OperatingSystem operatingSystem;

  private Process process;

  public int start(int gameId, int clientGpgPort, String accessToken) {
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
      startIceAdapterProcess(cmd);
    } catch (IOException e) {
      throw new CompletionException(e);
    }

    return gpgPort;
  }

  private void startIceAdapterProcess(List<String> cmd) throws IOException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(operatingSystem.getLoggingDirectory().resolve("ice").toFile());
    processBuilder.command(cmd);

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
    List<String> args = new ArrayList<>(
        List.of(iceAdapterService.getExecutablePath().toString(),
                "--user-id", String.valueOf(currentPlayer.getId()),
                "--game-id", String.valueOf(gameId),
                "--gpgnet-port", String.valueOf(gpgGamePort),
                "--gpgnet-client-port", String.valueOf(gpgClientPort),
                "--api-root", clientProperties.getApi().getBaseUrl() + "/ice",
                "--access-token", accessToken,
                "--log-level", debugLogging ? "-1" : "0"
        )
    );
    // @formatter:on

    // The pioneer is very picky and breaks if we send empty args, so we need to have a clean list
    if (forceTurnRelay) {
      args.add("--force-turn-relay");
    }

    if (consentLogSharing) {
      args.add("--consent-log-sharing");
    }

    return args;
  }

  public void destroy() {
    stop();
  }

  public void stop() {
    process.destroy();
  }
}
