package com.faforever.client.headerbar;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.NodeController;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.LoginService;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class UserButtonController extends NodeController<Node> {

  private final PlayerService playerService;
  private final UiService uiService;
  private final LoginService loginService;

  public MenuButton userMenuButtonRoot;

  @Override
  protected void onInitialize() {
    userMenuButtonRoot.textProperty()
        .bind(playerService.currentPlayerProperty().flatMap(PlayerBean::usernameProperty).when(showing));
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

  public void onLogOut() {
    loginService.logOut();
  }
}
