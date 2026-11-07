package com.faforever.client.fa;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.DataPrefs;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.PreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Desync reports from a faf-re game: when the game desyncs it can send a small record and its dump file, so its
 * developers can find the cause. It sends them only when the player agreed ({@code /telemetry}), and each upload
 * carries a token icebreaker issued for this game and player ({@code /telemetrytoken}), which the report server
 * checks. The game keeps the token from other players, and this client keeps it out of its logs.
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

  /** Icebreaker's answer to {@code POST /session/token}. */
  record TokenResponse(String jwt) {}

  /**
   * The game's arguments for desync reports in game {@code gameId}: none unless the exe is faf-re and the player
   * agreed. A player who wasn't asked yet is asked once, and this game sends nothing. Without a token (icebreaker
   * didn't answer) the game sends nothing either.
   */
  public List<String> launchArguments(int gameId) {
    Path executable = dataPrefs.getBinDirectory().resolve(PreferencesService.FORGED_ALLIANCE_EXE);
    if (!FafReExecutable.isFafRe(executable)) {
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
    log.info("Game {} sends desync reports (the player agreed)", gameId);
    return List.of("/telemetry", TOKEN_ARGUMENT, token);
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
