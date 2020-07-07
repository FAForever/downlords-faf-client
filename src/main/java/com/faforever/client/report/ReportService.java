package com.faforever.client.report;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.api.dto.Game;
import com.faforever.client.api.dto.ModerationReport;
import com.faforever.client.api.dto.Player;
import com.faforever.client.player.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReportService {
  private final FafApiAccessor fafApiAccessor;
  private final PlayerService playerService;

  public void report(com.faforever.client.player.Player offender, String replayId, String description, String timeCode) {
    ModerationReport moderationReport = new ModerationReport();
    com.faforever.client.api.dto.Player reporter = playerService.getCurrentPlayer().map(p -> {
      Player player = new Player();
      player.setId(String.valueOf(p.getId()));
      return player;
    }).orElseThrow(() -> new IllegalStateException("Player has not been set"));
    Player offenderDto = new Player();
    offenderDto.setId(String.valueOf(offender.getId()));
    moderationReport.setReportDescription(description);
    moderationReport.setReporter(reporter);
    moderationReport.setReportedUsers(Collections.singleton(offenderDto));
    if (StringUtils.isNotEmpty(replayId)) {
      Game game = new Game();
      game.setId(Integer.valueOf(replayId).toString());
      moderationReport.setGame(game);
    }
    if (StringUtils.isNotEmpty(timeCode)) {
      moderationReport.setGameIncidentTimecode(timeCode);
    }
    ModerationReport report = fafApiAccessor.createReport(moderationReport);
    log.info("Reported {}", report);
  }

}
