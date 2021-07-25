package com.faforever.client.main;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.player.PlayerInfoWindowController;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.event.ActionEvent;
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
public class UserButtonController implements Controller<Node> {

  private final EventBus eventBus;
  private final PlayerService playerService;
  private final UiService uiService;
  private final UserService userService;
  private final PreferencesService preferencesService;
  public MenuButton userMenuButtonRoot;

  public void initialize() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    JavaFxUtil.runLater(() -> userMenuButtonRoot.setText(userService.getUsername()));
  }

  @Override
  public Node getRoot() {
    return userMenuButtonRoot;
  }

  public void onShowProfile(ActionEvent event) {
    PlayerInfoWindowController playerInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    playerInfoWindowController.setPlayer(playerService.getCurrentPlayer());
    Scene scene = userMenuButtonRoot.getScene();
    if (scene != null) {
      playerInfoWindowController.setOwnerWindow(scene.getWindow());
    }
    playerInfoWindowController.show();
  }

  public void onReport(ActionEvent event) {
    ReportDialogController reportDialogController = uiService.loadFxml("theme/reporting/report_dialog.fxml");
    reportDialogController.setAutoCompleteWithOnlinePlayers();
    Scene scene = userMenuButtonRoot.getScene();
    if (scene != null) {
      reportDialogController.setOwnerWindow(scene.getWindow());
    }
    reportDialogController.show();
  }

  public void onLogOut(ActionEvent actionEvent) {
    eventBus.post(new LogOutRequestEvent());
  }
}
