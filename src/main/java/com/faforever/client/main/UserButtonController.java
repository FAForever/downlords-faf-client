package com.faforever.client.main;

import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.Controller;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
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
  public MenuButton userMenuButtonRoot;

  public void initialize() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    Platform.runLater(() -> userMenuButtonRoot.setText(event.getUsername()));
  }

  @Override
  public Node getRoot() {
    return userMenuButtonRoot;
  }

  public void onShowProfile(ActionEvent event) {
    UserInfoWindowController userInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    userInfoWindowController.setPlayer(playerService.getCurrentPlayer().orElseThrow(() -> new IllegalStateException("Player has not been set")));
    userInfoWindowController.setOwnerWindow(userMenuButtonRoot.getScene().getWindow());
    userInfoWindowController.show();
  }

  public void onLogOut(ActionEvent actionEvent) {
    eventBus.post(new LogOutRequestEvent());
  }
}
