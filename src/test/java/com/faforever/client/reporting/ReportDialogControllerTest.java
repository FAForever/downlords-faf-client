package com.faforever.client.reporting;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.Replay;
import com.faforever.client.replay.ReplayBuilder;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.collections.FXCollections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportDialogControllerTest extends AbstractPlainJavaFxTest {
  private ReportDialogController instance;

  @Mock
  private ModerationService moderationService;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private TimeService timeService;
  @Mock
  private PlayerService playerService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReplayService replayService;

  private Player player;
  private Replay replay;

  @Before
  public void setUp() throws Exception {
    player = PlayerBuilder.create("junit").defaultValues().get();
    replay = ReplayBuilder.create().defaultValues()
        .teams(FXCollections.observableMap(new HashMap<>(Map.of("1", List.of(player.getUsername())))))
        .get();

    instance = new ReportDialogController(moderationService, notificationService, playerService, i18n,
        uiService, timeService, replayService);

    when(i18n.get("report.noReports")).thenReturn("noReports");

    when(playerService.getCurrentPlayer()).thenReturn(Optional.of(player));
    when(playerService.getPlayerByName(player.getUsername())).thenReturn(CompletableFuture.completedFuture(Optional.of(player)));
    when(replayService.findById(replay.getId())).thenReturn(CompletableFuture.completedFuture(Optional.of(replay)));
    when(moderationService.getModerationReports()).thenReturn(CompletableFuture.completedFuture(List.of()));

    loadFxml("theme/reporting/report_dialog.fxml", clazz -> instance);

    instance.offender.setText(player.getUsername());
    instance.gameId.setText(String.valueOf(replay.getId()));
    instance.gameTime.setText("now");
    instance.reportDescription.setText("Testing");
  }

  @Test
  public void testOnReport() {
    instance.onReportButtonClicked();

    verify(moderationService).postModerationReport(any(ModerationReport.class));
    assertTrue(instance.offender.getText().isBlank());
    assertTrue(instance.reportDescription.getText().isBlank());
    assertTrue(instance.gameTime.getText().isBlank());
    assertTrue(instance.gameId.getText().isBlank());
  }

  @Test
  public void testOnReportGameIdHasHashTag() {
    instance.gameId.setText("#" + replay.getId());

    instance.onReportButtonClicked();

    verify(moderationService).postModerationReport(any(ModerationReport.class));
    assertTrue(instance.offender.getText().isBlank());
    assertTrue(instance.reportDescription.getText().isBlank());
    assertTrue(instance.gameTime.getText().isBlank());
    assertTrue(instance.gameId.getText().isBlank());
  }

  @Test
  public void testOnReportNoPlayer() {
    when(playerService.getPlayerByName(player.getUsername())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    instance.onReportButtonClicked();

    verify(notificationService).addImmediateWarnNotification("report.warning.noPlayer");
  }

  @Test
  public void testOnReportNoGame() {
    when(replayService.findById(replay.getId())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    instance.onReportButtonClicked();

    verify(notificationService).addImmediateWarnNotification("report.warning.title");
  }

  @Test
  public void testOnReportNoOffender() {
    instance.offender.setText("");

    instance.onReportButtonClicked();

    verify(notificationService).addImmediateWarnNotification("report.warning.noOffender");
  }

  @Test
  public void testOnReportOffenderNotInGame() {
    replay.getTeams().clear();

    instance.onReportButtonClicked();

    verify(notificationService).addImmediateWarnNotification("report.warning.noOffenderInGame");
  }

  @Test
  public void testOnReportNoDescription() {
    instance.reportDescription.setText("");

    instance.onReportButtonClicked();

    verify(notificationService).addImmediateWarnNotification("report.warning.noDescription");
  }

  @Test
  public void testOnReportNoGameTime() {
    instance.gameTime.setText("");

    instance.onReportButtonClicked();

    verify(notificationService).addImmediateWarnNotification("report.warning.noGameTime");
  }

  @Test
  public void testOnReportGameIdNotNumeric() {
    instance.gameId.setText("boo");

    instance.onReportButtonClicked();

    verify(notificationService).addImmediateWarnNotification("report.warning.gameIdNotNumeric");
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.reportDialogRoot, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testSetOffenderPlayer() {
    instance.setOffender(player);
    assertEquals(player.getUsername(), instance.offender.getText());
  }

  @Test
  public void testSetOffenderString() {
    instance.setOffender(player.getUsername());
    assertEquals(player.getUsername(), instance.offender.getText());
  }

  @Test
  public void testSetGame() {
    instance.setReplay(replay);
    assertEquals(String.valueOf(replay.getId()), instance.gameId.getText());
  }
}
