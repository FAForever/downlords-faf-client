package com.faforever.client.fa;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Desync reports from a game on the rebuilt engine: when the game desyncs it can send a small record and its dump
 * file, so the engine's developers can find the cause. It sends them only when the player agreed
 * ({@code /telemetry}), and each upload carries a token icebreaker issued for this game and player
 * ({@code /telemetrytoken}), which the report server checks. The game keeps the token from other players, and this
 * client keeps it out of its logs. The chosen server's endpoints ({@code desyncRecords}, {@code desyncDumps}) say
 * where the reports go; without them the game's default.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DesyncReports {

  static final String TOKEN_ARGUMENT = "/telemetrytoken";
  private static final Duration TOKEN_TIMEOUT = Duration.ofSeconds(5);

  private final FafApiAccessor fafApiAccessor;
  private final ForgedAlliancePrefs forgedAlliancePrefs;
  private final DataPrefs dataPrefs;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ClientProperties clientProperties;

  /** Icebreaker's answer to {@code POST /ice/session/token} on the API's address. */
  record TokenResponse(String jwt) {}

  /** The last game's arguments, so a game that comes back after a crash doesn't fetch its token again. */
  private record Prepared(int gameId, CompletableFuture<List<String>> arguments) {}

  private volatile Prepared prepared;
  /** Past this, the launch goes on without reports: the token request can first wait for the client's sign-in. */
  @VisibleForTesting
  long deadlineMillis = TOKEN_TIMEOUT.toMillis() + 1000;

  /**
   * Starts working out game {@code gameId}'s arguments on another thread, as fetching the token can take seconds and
   * the game is launched on the UI thread. The future never fails.
   */
  public CompletableFuture<List<String>> prepare(int gameId) {
    Prepared current = prepared;
    if (current != null && current.gameId() == gameId) {
      return current.arguments();
    }
    CompletableFuture<List<String>> arguments = CompletableFuture.supplyAsync(() -> fetchArguments(gameId))
                                                                 .exceptionally(e -> {
                                                                   log.warn("No desync report arguments for game {}", gameId, e);
                                                                   return List.of();
                                                                 })
                                                                 .completeOnTimeout(List.of(), deadlineMillis,
                                                                                    TimeUnit.MILLISECONDS);
    prepared = new Prepared(gameId, arguments);
    return arguments;
  }

  /**
   * The game's arguments for desync reports in game {@code gameId}: none unless the exe is the rebuilt engine and the
   * player agreed. A player who wasn't asked yet is asked once, and this game sends nothing. Without a token
   * (icebreaker didn't answer) the game sends nothing either. Takes what {@link #prepare} worked out for this game;
   * without that, works it out here and waits for it.
   */
  public List<String> launchArguments(int gameId) {
    List<String> arguments = prepare(gameId).join();
    // The player may have withdrawn consent since these were prepared, for example before returning to the match.
    return forgedAlliancePrefs.isSendDesyncReports() ? arguments : List.of();
  }

  private List<String> fetchArguments(int gameId) {
    Path executable = dataPrefs.getBinDirectory().resolve(PreferencesService.FORGED_ALLIANCE_EXE);
    if (!RebuiltEngineExecutable.isRebuiltEngine(executable)) {
      return List.of();
    }
    if (!forgedAlliancePrefs.isAskedSendDesyncReports()) {
      askOnce();
      return List.of();
    }
    if (!forgedAlliancePrefs.isSendDesyncReports()) {
      return List.of();
    }
    String token;
    try {
      TokenResponse response = fafApiAccessor.postJsonForObject("/ice/session/token", Map.of("gameId", gameId),
                                                                TokenResponse.class).block(TOKEN_TIMEOUT);
      token = response == null ? null : response.jwt();
    } catch (RuntimeException e) {
      log.warn("No desync report token for game {}, so the game sends none", gameId, e);
      return List.of();
    }
    if (token == null || token.isBlank()) {
      log.warn("No desync report token for game {}, so the game sends none", gameId);
      return List.of();
    }
    List<String> arguments = new ArrayList<>(List.of("/telemetry", TOKEN_ARGUMENT, token));
    ClientProperties.DesyncReports endpoints = clientProperties.getDesyncReports();
    if (endpoints.getRecordsUrl() != null && !endpoints.getRecordsUrl().isBlank()) {
      arguments.addAll(List.of("/telemetryendpoint", endpoints.getRecordsUrl()));
    }
    if (endpoints.getDumpsUrl() != null && !endpoints.getDumpsUrl().isBlank()) {
      arguments.addAll(List.of("/telemetrydumpendpoint", endpoints.getDumpsUrl()));
    }
    log.info("Game {} sends desync reports (the player agreed)", gameId);
    return arguments;
  }

  private void askOnce() {
    forgedAlliancePrefs.setAskedSendDesyncReports(true);
    notificationService.addNotification(new PersistentNotification(i18n.get("game.desyncReports.ask"), Severity.INFO,
                                                                   List.of(new Action(i18n.get("game.desyncReports.yes"),
                                                                                      () -> forgedAlliancePrefs.setSendDesyncReports(
                                                                                          true)),
                                                                           new Action(i18n.get("game.desyncReports.no"),
                                                                                      () -> forgedAlliancePrefs.setSendDesyncReports(
                                                                                          false)))));
  }

  /** The command line as it may be logged: the value after {@code /telemetrytoken} replaced. */
  public static List<String> withTokenHidden(List<String> command) {
    List<String> shown = new ArrayList<>(command);
    for (int i = 0; i + 1 < shown.size(); i++) {
      if (TOKEN_ARGUMENT.equalsIgnoreCase(shown.get(i))) {
        shown.set(i + 1, "<token>");
      }
    }
    return shown;
  }
}
