package com.faforever.client.player;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.scene.input.Clipboard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PlayerContextMenuControllerTest extends UITest {

  @Mock
  private UiService uiService;
  @Mock
  private EventBus eventBus;
  @Mock
  private PlayerInfoWindowController playerInfoWindowController;
  @Mock
  private ReportDialogController reportDialogController;

  private Player player;
  private PlayerContextMenuController instance;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBuilder.create("junit").get();

    instance = new PlayerContextMenuController(uiService, eventBus);
    instance.setPlayer(player);
    loadFxml("theme/player/player_context_menu.fxml", clazz -> instance);
  }

  @Test
  public void testOnShowPlayerInfoClicked() {
    when(uiService.loadFxml("theme/user_info_window.fxml")).thenReturn(playerInfoWindowController);
    runOnFxThreadAndWait(() -> instance.onShowPlayerInfoClicked());

    verify(playerInfoWindowController).setPlayer(player);
    verify(playerInfoWindowController).show();
  }

  @Test
  public void testOnSendPrivateMessageClicked() {
    runOnFxThreadAndWait(() -> instance.onSendPrivateMessageClicked());
    verify(eventBus).post(any(InitiatePrivateChatEvent.class));
  }

  @Test
  public void testOnCopyPlayerNameClicked() {
    String[] actual = new String[1];
    runOnFxThreadAndWait(() -> {
      instance.onCopyPlayerNameClicked();
      actual[0] = Clipboard.getSystemClipboard().getString();
    });
    assertEquals("junit", actual[0]);
  }

  @Test
  public void testOnReportPlayerClicked() {
    when(uiService.loadFxml("theme/reporting/report_dialog.fxml")).thenReturn(reportDialogController);
    runOnFxThreadAndWait(() -> instance.onReportPlayerClicked());

    verify(reportDialogController).setOffender(player);
    verify(reportDialogController).show();
  }

  @Test
  public void testOnViewReplaysClicked() {
    runOnFxThreadAndWait(() -> instance.onViewReplaysClicked());
    verify(eventBus).post(any(ShowUserReplaysEvent.class));
  }
}
