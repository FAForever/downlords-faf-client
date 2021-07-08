package com.faforever.client.reporting;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Lazy
@Service
@RequiredArgsConstructor
public class ModerationService {
  private final FafService fafService;
  private final PlayerService playerService;

  public CompletableFuture<List<ModerationReport>> getModerationReports() {
    Player currentPlayer = playerService.getCurrentPlayer();
    return fafService.getAllModerationReports(currentPlayer.getId());
  }

  public CompletableFuture<Void> postModerationReport(ModerationReport report) {
    return fafService.postModerationReport(report);
  }
}
