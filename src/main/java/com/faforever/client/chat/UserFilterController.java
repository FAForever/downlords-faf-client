package com.faforever.client.chat;

import com.faforever.client.fx.Controller;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.util.ProgrammingError;
import com.faforever.client.util.RatingUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

import static com.faforever.client.game.PlayerStatus.HOSTING;
import static com.faforever.client.game.PlayerStatus.IDLE;
import static com.faforever.client.game.PlayerStatus.LOBBYING;
import static com.faforever.client.game.PlayerStatus.PLAYING;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserFilterController implements Controller<Node> {

  private final I18n i18n;
  public MenuButton gameStatusMenu;
  public GridPane filterUserRoot;
  public TextField clanFilterField;
  public TextField minRatingFilterField;
  public TextField maxRatingFilterField;
  public ToggleGroup gameStatusToggleGroup;
  private final BooleanProperty filterApplied;
  @VisibleForTesting
  ChannelTabController channelTabController;
  @VisibleForTesting
  PlayerStatus playerStatusFilter;

  @Inject
  public UserFilterController(I18n i18n) {
    this.i18n = i18n;
    this.filterApplied = new SimpleBooleanProperty(false);
  }

  void setChannelController(ChannelTabController channelTabController) {
    this.channelTabController = channelTabController;
  }

  public void initialize() {
    clanFilterField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
    minRatingFilterField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
    maxRatingFilterField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
  }

  private void filterUsers() {
    Map<String, Map<Pane, ChatUserItemController>> userToChatUserControls = channelTabController.getUserToChatUserControls();
    synchronized (userToChatUserControls) {
      for (Map<Pane, ChatUserItemController> chatUserControlMap : userToChatUserControls.values()) {
        for (Map.Entry<Pane, ChatUserItemController> chatUserControlEntry : chatUserControlMap.entrySet()) {
          ChatUserItemController chatUserItemController = chatUserControlEntry.getValue();
          chatUserItemController.setVisible(filterUser(chatUserItemController));
        }
      }
    }
    filterApplied.set(!maxRatingFilterField.getText().isEmpty() || !minRatingFilterField.getText().isEmpty() || !clanFilterField.getText().isEmpty() || playerStatusFilter != null);
  }

  boolean filterUser(ChatUserItemController chatUserItemController) {
    return channelTabController.isUsernameMatch(chatUserItemController)
        && isInClan(chatUserItemController)
        && isBoundByRating(chatUserItemController)
        && isGameStatusMatch(chatUserItemController);
  }

  public BooleanProperty filterAppliedProperty() {
    return filterApplied;
  }

  public boolean isFilterApplied() {
    return filterApplied.get();
  }

  @VisibleForTesting
  boolean isInClan(ChatUserItemController chatUserItemController) {
    if (clanFilterField.getText().isEmpty()) {
      return true;
    }

    ChatUser chatUser = chatUserItemController.getChatUser();
    Optional<Player> playerOptional = chatUser.getPlayer();

    if (!playerOptional.isPresent()) {
      return false;
    }

    Player player = playerOptional.get();
    String clan = player.getClan();
    if (clan == null) {
      return false;
    }

    String lowerCaseSearchString = clan.toLowerCase();
    return lowerCaseSearchString.contains(clanFilterField.getText().toLowerCase());
  }

  @VisibleForTesting
  boolean isBoundByRating(ChatUserItemController chatUserItemController) {
    if (minRatingFilterField.getText().isEmpty() && maxRatingFilterField.getText().isEmpty()) {
      return true;
    }

    ChatUser chatUser = chatUserItemController.getChatUser();
    Optional<Player> optionalPlayer = chatUser.getPlayer();

    if (!optionalPlayer.isPresent()) {
      return false;
    }

    Player player = optionalPlayer.get();

    int globalRating = RatingUtil.getGlobalRating(player);
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

    ChatUser chatUser = chatUserItemController.getChatUser();
    Optional<Player> playerOptional = chatUser.getPlayer();

    if (!playerOptional.isPresent()) {
      return false;
    }

    Player player = playerOptional.get();
    PlayerStatus playerStatus = player.getStatus();
    if (playerStatusFilter == LOBBYING) {
      return LOBBYING == playerStatus || HOSTING == playerStatus;
    } else {
      return playerStatusFilter == playerStatus;
    }
  }

  public void onGameStatusPlaying(ActionEvent actionEvent) {
    updateGameStatusMenuText(playerStatusFilter == PLAYING ? null : PLAYING);
    filterUsers();
  }

  public void onGameStatusLobby(ActionEvent actionEvent) {
    updateGameStatusMenuText(playerStatusFilter == LOBBYING ? null : LOBBYING);
    filterUsers();
  }

  public void onGameStatusNone(ActionEvent actionEvent) {
    updateGameStatusMenuText(playerStatusFilter == IDLE ? null : IDLE);
    filterUsers();
  }

  private void updateGameStatusMenuText(PlayerStatus status) {
    playerStatusFilter = status;
    if (status == null) {
      gameStatusMenu.setText(i18n.get("game.gameStatus"));
      gameStatusToggleGroup.selectToggle(null);
      return;
    }

    switch (status) {
      case PLAYING:
        gameStatusMenu.setText(i18n.get("game.gameStatus.playing"));
        break;
      case LOBBYING:
        gameStatusMenu.setText(i18n.get("game.gameStatus.lobby"));
        break;
      case IDLE:
        gameStatusMenu.setText(i18n.get("game.gameStatus.none"));
        break;
      default:
        throw new ProgrammingError("Uncovered player status: " + status);
    }
  }

  public Node getRoot() {
    return filterUserRoot;
  }
}
