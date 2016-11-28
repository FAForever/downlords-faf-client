package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.util.RatingUtil;
import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

import static com.faforever.client.game.PlayerStatus.HOSTING;
import static com.faforever.client.game.PlayerStatus.IDLE;
import static com.faforever.client.game.PlayerStatus.LOBBYING;
import static com.faforever.client.game.PlayerStatus.PLAYING;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserFilterController implements Controller<Node> {

  public MenuButton gameStatusMenu;
  public GridPane filterUserRoot;
  public TextField clanFilterField;
  public TextField minRatingFilterField;
  public TextField maxRatingFilterField;

  @Inject
  I18n i18n;

  @VisibleForTesting
  ChannelTabController channelTabController;
  @VisibleForTesting
  PlayerStatus playerStatusFilter;

  public void setChannelController(ChannelTabController channelTabController) {
    this.channelTabController = channelTabController;
  }

  void initialize() {
    clanFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
      filterUsers();
    });
    minRatingFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
      filterUsers();
    });
    maxRatingFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
      filterUsers();
    });
  }

  private void filterUsers() {
    Map<String, Map<Pane, ChatUserItemController>> userToChatUserControls = channelTabController.getUserToChatUserControls();
    for (Map<Pane, ChatUserItemController> chatUserControlMap : userToChatUserControls.values()) {
      for (Map.Entry<Pane, ChatUserItemController> chatUserControlEntry : chatUserControlMap.entrySet()) {
        ChatUserItemController chatUserItemController = chatUserControlEntry.getValue();
        chatUserItemController.setVisible(filterUser(chatUserItemController));
      }
    }
  }

  boolean filterUser(ChatUserItemController chatUserItemController) {
    return channelTabController.isUsernameMatch(chatUserItemController)
        && isInClan(chatUserItemController)
        && isBoundedByRating(chatUserItemController)
        && isGameStatusMatch(chatUserItemController);
  }

  @VisibleForTesting
  boolean isInClan(ChatUserItemController chatUserItemController) {
    if (clanFilterField.getText().isEmpty()) {
      return true;
    }

    String clan = chatUserItemController.getPlayer().getClan();
    if (clan == null) {
      return false;
    } else {
      String lowerCaseSearchString = clan.toLowerCase();
      return lowerCaseSearchString.contains(clanFilterField.getText().toLowerCase());
    }
  }

  @VisibleForTesting
  boolean isBoundedByRating(ChatUserItemController chatUserItemController) {
    if (minRatingFilterField.getText().isEmpty() && maxRatingFilterField.getText().isEmpty()) {
      return true;
    }

    int globalRating = RatingUtil.getGlobalRating(chatUserItemController.getPlayer());
    int minRating;
    int maxRating;

    try {
      minRating = Integer.parseInt(minRatingFilterField.getText());
    } catch (NumberFormatException e) {
      minRating = Integer.MIN_VALUE;
    }
    try {
      maxRating = Integer.parseInt(maxRatingFilterField.getText());
    } catch (NumberFormatException e) {
      maxRating = Integer.MAX_VALUE;
    }

    return globalRating >= minRating && globalRating <= maxRating;
  }

  @VisibleForTesting
  boolean isGameStatusMatch(ChatUserItemController chatUserItemController) {
    if (playerStatusFilter == null) {
      return true;
    }

    PlayerStatus playerStatus = chatUserItemController.getPlayer().getStatus();
    if (playerStatusFilter == LOBBYING) {
      return LOBBYING == playerStatus || HOSTING == playerStatus;
    } else {
      return playerStatusFilter == playerStatus;
    }
  }

  public void onGameStatusPlaying(ActionEvent actionEvent) {
    playerStatusFilter = PLAYING;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus.playing"));
    filterUsers();
  }

  public void onGameStatusLobby(ActionEvent actionEvent) {
    playerStatusFilter = LOBBYING;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus.lobby"));
    filterUsers();
  }

  public void onGameStatusNone(ActionEvent actionEvent) {
    playerStatusFilter = IDLE;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus.none"));
    filterUsers();
  }

  public void onClearGameStatus(ActionEvent actionEvent) {
    playerStatusFilter = null;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus"));
    filterUsers();
  }

  public Node getRoot() {
    return filterUserRoot;
  }
}
