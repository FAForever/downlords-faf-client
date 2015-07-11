package com.faforever.client.chat;

import com.faforever.client.fx.SceneFactory;
import com.faforever.client.player.PlayerService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static com.faforever.client.fx.WindowDecorator.WindowButtonType.CLOSE;

public class ChatUserContextMenuController {

  @FXML
  MenuItem addFriendItem;

  @FXML
  MenuItem removeFriendItem;

  @FXML
  MenuItem addFoeItem;

  @FXML
  MenuItem removeFoeItem;

  @FXML
  MenuItem watchGameItem;

  @FXML
  MenuItem viewReplaysItem;

  @FXML
  MenuItem inviteItem;

  @FXML
  SeparatorMenuItem moderatorActionSeparator;

  @FXML
  MenuItem kickItem;

  @FXML
  MenuItem banItem;

  @FXML
  ContextMenu contextMenu;

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  SceneFactory sceneFactory;

  @Autowired
  PlayerService playerService;

  private PlayerInfoBean playerInfoBean;

  public ContextMenu getContextMenu() {
    return contextMenu;
  }

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;

    addFriendItem.visibleProperty().bind(playerInfoBean.friendProperty().not());
    removeFriendItem.visibleProperty().bind(playerInfoBean.friendProperty());
    addFoeItem.visibleProperty().bind(playerInfoBean.foeProperty().not());
    removeFoeItem.visibleProperty().bind(playerInfoBean.foeProperty());
  }

  @FXML
  void onUserInfo(ActionEvent actionEvent) {
    UserInfoWindowController userInfoWindowController = applicationContext.getBean(UserInfoWindowController.class);
    userInfoWindowController.setPlayerInfoBean(playerInfoBean);

    Stage userInfoWindow = new Stage(StageStyle.TRANSPARENT);
    userInfoWindow.initModality(Modality.NONE);
    userInfoWindow.initOwner(contextMenu.getOwnerWindow());

    sceneFactory.createScene(userInfoWindow, userInfoWindowController.getRoot(), true, CLOSE);

    userInfoWindow.show();
  }

  public void onSendPrivateMessage(ActionEvent event) {
  }

  public void onAddFriend(ActionEvent event) {
    playerService.addFriend(playerInfoBean.getUsername());
  }

  public void onRemoveFriend(ActionEvent event) {
    playerService.removeFriend(playerInfoBean.getUsername());
  }

  public void onAddFoe(ActionEvent event) {
    playerService.addFoe(playerInfoBean.getUsername());
  }

  public void onRemoveFoe(ActionEvent event) {
    playerService.removeFoe(playerInfoBean.getUsername());
  }

  public void onWatchGame(ActionEvent event) {
  }

  public void onViewReplays(ActionEvent event) {
  }

  public void onInviteToGame(ActionEvent event) {
  }

  public void onKick(ActionEvent event) {
  }

  public void onBan(ActionEvent event) {

  }
}
