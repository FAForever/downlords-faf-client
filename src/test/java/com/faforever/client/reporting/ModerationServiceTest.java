package com.faforever.client.reporting;

import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.FafService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModerationServiceTest {
  @Mock
  private FafService fafService;
  @Mock
  private PlayerService playerService;

  private ModerationService instance;
  private Player player;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new ModerationService(fafService, playerService);

    player = PlayerBuilder.create("junit").defaultValues().get();

    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
  }

  @Test
  public void testGetModerationReports() {
    instance.getModerationReports();
    verify(fafService).getAllModerationReports(player.getId());
  }

  @Test
  public void testPostModerationReport() {
    ModerationReport report = ModerationReportBuilder.create().defaultValues().get();
    instance.postModerationReport(report);
    verify(fafService).postModerationReport(report);
  }
}
