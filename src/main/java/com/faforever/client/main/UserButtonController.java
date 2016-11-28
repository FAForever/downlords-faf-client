package com.faforever.client.main;

import com.faforever.client.chat.UserInfoWindowController;
import com.faforever.client.fx.Controller;
import com.faforever.client.player.PlayerService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.event.LogOutRequestEvent;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.client.util.IdenticonUtil;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserButtonController implements Controller<Node> {

  public MenuButton userButtonRoot;
  public ImageView userImageView;

  @Inject
  EventBus eventBus;
  @Inject
  PlayerService playerService;
  @Inject
  UiService uiService;

  public void initialize() {
    eventBus.register(this);
  }

  @Subscribe
  public void onLoginSuccessEvent(LoginSuccessEvent event) {
    Platform.runLater(() -> {
      userButtonRoot.setText(event.getUsername());
      userImageView.setImage(IdenticonUtil.createIdenticon(event.getUserId()));
    });
  }

  @Override
  public Node getRoot() {
    return userButtonRoot;
  }

  public void onShowProfile(ActionEvent event) {
    UserInfoWindowController userInfoWindowController = uiService.loadFxml("theme/user_info_window.fxml");
    userInfoWindowController.setPlayer(playerService.getCurrentPlayer());
    userInfoWindowController.setOwnerWindow(userButtonRoot.getScene().getWindow());
    userInfoWindowController.show();
  }

  public void onLogOut(ActionEvent actionEvent) {
    eventBus.post(new LogOutRequestEvent());
  }
}
