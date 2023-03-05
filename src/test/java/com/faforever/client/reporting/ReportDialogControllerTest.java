package com.faforever.client.reporting;

import com.faforever.client.builders.ModerationReportBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.domain.ModerationReportBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReportDialogControllerTest extends UITest {
  @InjectMocks
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

  private PlayerBean player;
  private ReplayBean replay;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();
    replay = ReplayBeanBuilder.create().defaultValues()
        .teams(FXCollections.observableMap(new HashMap<>(Map.of("1", List.of(player.getUsername())))))
        .get();

    when(i18n.get("report.noReports")).thenReturn("noReports");

    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(playerService.getPlayerByName(player.getUsername())).thenReturn(CompletableFuture.completedFuture(Optional.of(player)));
    when(replayService.findById(replay.getId())).thenReturn(CompletableFuture.completedFuture(Optional.of(replay)));
    when(moderationService.getModerationReports()).thenReturn(CompletableFuture.completedFuture(List.of()));
    when(moderationService.postModerationReport(any())).thenReturn(CompletableFuture.completedFuture(ModerationReportBeanBuilder.create().defaultValues().get()));

    loadFxml("theme/reporting/report_dialog.fxml", clazz -> instance);

    instance.offender.setText(player.getUsername());
    instance.gameId.setText(String.valueOf(replay.getId()));
    instance.gameTime.setText("now");
    instance.reportDescription.setText("Testing");
  }

  @Test
  public void testOnReport() {
    when(i18n.get("report.submit")).thenReturn("submit");

    runOnFxThreadAndWait(() -> instance.onReportButtonClicked());

    verify(moderationService).postModerationReport(any(ModerationReportBean.class));
    verify(notificationService).addImmediateInfoNotification("report.success");
    assertEquals("submit", instance.reportButton.getText());
    assertFalse(instance.reportDialogRoot.isDisabled());
    assertTrue(instance.offender.getText().isBlank());
    assertTrue(instance.reportDescription.getText().isBlank());
    assertTrue(instance.gameTime.getText().isBlank());
    assertTrue(instance.gameId.getText().isBlank());
  }

  @Test void testSendingReportProcess() {
    when(i18n.get("report.sending")).thenReturn("sending...");
    final Boolean[] disabledRootInteraction = new Boolean[1];
    final Boolean[] updatedButtonText = new Boolean[1];
    Mockito.doAnswer(invocation -> {
      WaitForAsyncUtils.waitForFxEvents();
      disabledRootInteraction[0] = instance.reportDialogRoot.isDisabled();
      updatedButtonText[0] = instance.reportButton.getText().equals("sending...");
      return invocation;
    }).when(moderationService).postModerationReport(any());

    runOnFxThreadAndWait(() -> instance.onReportButtonClicked());

    assertTrue(disabledRootInteraction[0]);
    assertTrue(updatedButtonText[0]);
  }

  @Test
  public void testOnReportGameIdHasHashTag() {
    instance.gameId.setText("#" + replay.getId());

    instance.onReportButtonClicked();

    verify(moderationService).postModerationReport(any(ModerationReportBean.class));
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
    replay.setTeams(Map.of());

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
