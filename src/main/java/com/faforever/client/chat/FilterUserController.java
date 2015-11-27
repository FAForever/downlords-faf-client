package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.GameStatus;
import com.faforever.client.util.RatingUtil;
import com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

import static com.faforever.client.legacy.GameStatus.HOST;
import static com.faforever.client.legacy.GameStatus.LOBBY;
import static com.faforever.client.legacy.GameStatus.NONE;
import static com.faforever.client.legacy.GameStatus.PLAYING;

public class FilterUserController {

  @FXML
  MenuButton gameStatusMenu;
  @FXML
  GridPane filterUserRoot;
  @FXML
  TextField clanFilterField;
  @FXML
  TextField minRatingFilterField;
  @FXML
  TextField maxRatingFilterField;

  @Resource
  I18n i18n;

  @VisibleForTesting
  ChannelTabController channelTabController;
  @VisibleForTesting
  GameStatus gameStatusFilter;

  public void setChannelController(ChannelTabController channelTabController) {
    this.channelTabController = channelTabController;
  }

  @PostConstruct
  void init() {
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
    Map<String, Map<Pane, ChatUserControl>> userToChatUserControls = channelTabController.getUserToChatUserControls();
    for (Map<Pane, ChatUserControl> chatUserControlMap : userToChatUserControls.values()) {
      for (Map.Entry<Pane, ChatUserControl> chatUserControlEntry : chatUserControlMap.entrySet()) {
        ChatUserControl chatUserControl = chatUserControlEntry.getValue();
        boolean display;
        display = filterUser(chatUserControl);
        chatUserControl.setVisible(display);
        chatUserControl.setManaged(display);
      }
    }
  }

  private boolean filterUser(ChatUserControl chatUserControl) {
    return channelTabController.isUsernameMatch(chatUserControl)
        && isInClan(chatUserControl)
        && isBoundedByRating(chatUserControl)
        && isGameStatusMatch(chatUserControl);
  }

  @VisibleForTesting
  boolean isInClan(ChatUserControl chatUserControl) {
    if (clanFilterField.getText().isEmpty()) {
      return true;
    }

    String clan = chatUserControl.getPlayerInfoBean().getClan();
    if (clan == null) {
      return false;
    } else {
      String lowerCaseSearchString = clan.toLowerCase();
      return lowerCaseSearchString.contains(clanFilterField.getText().toLowerCase());
    }
  }

  @VisibleForTesting
  boolean isBoundedByRating(ChatUserControl chatUserControl) {
    int globalRating = RatingUtil.getGlobalRating(chatUserControl.getPlayerInfoBean());
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
  boolean isGameStatusMatch(ChatUserControl chatUserControl) {
    if (gameStatusFilter == null) {
      return true;
    }

    GameStatus gameStatus = chatUserControl.getPlayerInfoBean().getGameStatus();
    if (gameStatusFilter == LOBBY) {
      return LOBBY == gameStatus || HOST == gameStatus;
    } else {
      return gameStatusFilter == gameStatus;
    }
  }

  @FXML
  void onGameStatusPlaying(ActionEvent actionEvent) {
    gameStatusFilter = PLAYING;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus.playing"));
    filterUsers();
  }

  @FXML
  void onGameStatusLobby(ActionEvent actionEvent) {
    gameStatusFilter = LOBBY;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus.lobby"));
    filterUsers();
  }

  @FXML
  void onGameStatusNone(ActionEvent actionEvent) {
    gameStatusFilter = NONE;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus.none"));
    filterUsers();
  }

  @FXML
  void onClearGameStatus(ActionEvent actionEvent) {
    gameStatusFilter = null;
    gameStatusMenu.setText(i18n.get("chat.filter.gameStatus"));
    filterUsers();
  }

  public Node getRoot() {
    return filterUserRoot;
  }
}
