package com.faforever.client.headerbar;

import com.faforever.client.api.TokenRetriever;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.NodeController;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.ClipboardUtil;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class UserButtonController extends NodeController<Node> {

  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  private final PlayerService playerService;
  private final UiService uiService;
  private final LoginService loginService;
  private final TokenRetriever tokenRetriever;

  public MenuButton userMenuButtonRoot;

  @Override
  protected void onInitialize() {
    userMenuButtonRoot.textProperty()
                      .bind(playerService.currentPlayerProperty().flatMap(PlayerInfo::usernameProperty).when(showing));
  }

  @Override
  public Node getRoot() {
    return userMenuButtonRoot;
  }

  public void onShowProfile() {
    PlayerInfoWindowController playerInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    playerInfoWindowController.setPlayer(playerService.getCurrentPlayer());
    Scene scene = userMenuButtonRoot.getScene();
    if (scene != null) {
      playerInfoWindowController.setOwnerWindow(scene.getWindow());
    }
    playerInfoWindowController.show();
  }

  public void onReport() {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setAutoCompleteWithOnlinePlayers();
    Scene scene = userMenuButtonRoot.getScene();
    if (scene != null) {
      reportDialogController.setOwnerWindow(scene.getWindow());
    }
    reportDialogController.show();
  }

  public void onCopyAccessToken() {
    tokenRetriever.getAccessToken().publishOn(fxApplicationThreadExecutor.asScheduler()).subscribe(accessToken -> {
      log.info("Copied access token to clipboard");
      ClipboardUtil.copyToClipboard(accessToken);
    });
  }

  public void onLogOut() {
    loginService.logOut();
  }
}
