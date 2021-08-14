package com.faforever.client.player;

import com.faforever.client.chat.InitiatePrivateChatEvent;
import com.faforever.client.fx.Controller;
import com.faforever.client.main.event.ShowUserReplaysEvent;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.ClipboardUtil;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class PlayerContextMenuController implements Controller<ContextMenu> {

  private final UiService uiService;
  private final EventBus eventBus;
  private Player player;

  public ContextMenu contextMenu;
  public MenuItem sendPrivateMessageItem;
  public MenuItem showPlayerInfoItem;
  public MenuItem copyPlayerNameItem;
  public MenuItem reportPlayerItem;
  public MenuItem viewReplaysItem;

  @Override
  public ContextMenu getRoot() {
    return contextMenu;
  }

  public void setPlayer(Player player) {
    this.player = Optional.of(player).orElseThrow(() -> new IllegalStateException("player must not be null"));
  }

  public void onShowPlayerInfoClicked() {
    PlayerInfoWindowController playerInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    playerInfoWindowController.setPlayer(player);
    playerInfoWindowController.setOwnerWindow(getRoot().getOwnerWindow());
    playerInfoWindowController.show();
  }

  public void onSendPrivateMessageClicked() {
    eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
  }

  public void onCopyPlayerNameClicked() {
    ClipboardUtil.copyToClipboard(player.getUsername());
  }

  public void onReportPlayerClicked() {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setOffender(player);
    reportDialogController.setOwnerWindow(getRoot().getOwnerWindow());
    reportDialogController.show();
  }

  public void onViewReplaysClicked() {
    eventBus.post(new ShowUserReplaysEvent(player.getId()));
  }
}
