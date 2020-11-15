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
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.faforever.client.game.PlayerStatus.HOSTING;
import static com.faforever.client.game.PlayerStatus.IDLE;
import static com.faforever.client.game.PlayerStatus.LOBBYING;
import static com.faforever.client.game.PlayerStatus.PLAYING;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserFilterController implements Controller<Node> {

  private final I18n i18n;
  private final CountryFlagService flagService;
  public MenuButton gameStatusMenu;
  public GridPane filterUserRoot;
  public TextField clanFilterField;
  public TextField minRatingFilterField;
  public TextField maxRatingFilterField;
  public ToggleGroup gameStatusToggleGroup;
  public TextField countryFilterField;


  private final BooleanProperty filterApplied;
  @VisibleForTesting
  ChannelTabController channelTabController;
  @VisibleForTesting
  PlayerStatus playerStatusFilter;

  List<String> currentSelectedCountries;

  public UserFilterController(I18n i18n, CountryFlagService flagService) {
    this.i18n = i18n;
    this.flagService = flagService;
    this.filterApplied = new SimpleBooleanProperty(false);
  }

  void setChannelController(ChannelTabController channelTabController) {
    this.channelTabController = channelTabController;
  }

  public void initialize() {
    clanFilterField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
    minRatingFilterField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
    maxRatingFilterField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
    countryFilterField.textProperty().addListener(((observable, oldValue, newValue) -> filterCountry()));
    currentSelectedCountries = flagService.getCountries(null);
  }

  public void filterUsers() {
    channelTabController.setUserFilter(this::filterUser);
    filterApplied.set(
        !maxRatingFilterField.getText().isEmpty()
            || !minRatingFilterField.getText().isEmpty()
            || !clanFilterField.getText().isEmpty()
            || playerStatusFilter != null
            || !countryFilterField.getText().isEmpty()
    );
  }

  private boolean filterUser(CategoryOrChatUserListItem userListItem) {
    if (userListItem.getCategory() != null) {

      // The categories should display in the list independently of a filter
      return true;
    }

    ChatChannelUser user = userListItem.getUser();
    return channelTabController.isUsernameMatch(user)
        && isInClan(user)
        && isBoundByRating(user)
        && isGameStatusMatch(user)
        && isCountryMatch(user);
  }

  private void filterCountry() {
    currentSelectedCountries = flagService.getCountries(countryFilterField.textProperty().get());
    filterUsers();
  }

  public BooleanProperty filterAppliedProperty() {
    return filterApplied;
  }

  public boolean isFilterApplied() {
    return filterApplied.get();
  }

  @VisibleForTesting
  boolean isInClan(ChatChannelUser chatUser) {
    if (clanFilterField.getText().isEmpty()) {
      return true;
    }

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
  boolean isBoundByRating(ChatChannelUser chatUser) {
    if (minRatingFilterField.getText().isEmpty() && maxRatingFilterField.getText().isEmpty()) {
      return true;
    }

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
  boolean isGameStatusMatch(ChatChannelUser chatUser) {
    if (playerStatusFilter == null) {
      return true;
    }

    Optional<Player> playerOptional = chatUser.getPlayer();

    if (!playerOptional.isPresent()) {
      return false;
    }

    PlayerStatus playerStatus = playerOptional.get().getStatus();
    if (playerStatusFilter == LOBBYING) {
      return LOBBYING == playerStatus || HOSTING == playerStatus;
    } else {
      return playerStatusFilter == playerStatus;
    }
  }

  boolean isCountryMatch(ChatChannelUser chatUser) {
    if (countryFilterField.getText().isEmpty()) {
      return true;
    }

    Optional<Player> playerOptional = chatUser.getPlayer();
    if (playerOptional.isEmpty()) {
      return false;
    }

    String country = playerOptional.get().getCountry();
    return currentSelectedCountries.contains(country);
  }

  public void onGameStatusPlaying() {
    updateGameStatusMenuText(playerStatusFilter == PLAYING ? null : PLAYING);
    filterUsers();
  }

  public void onGameStatusLobby() {
    updateGameStatusMenuText(playerStatusFilter == LOBBYING ? null : LOBBYING);
    filterUsers();
  }

  public void onGameStatusNone() {
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
    gameStatusMenu.setText(i18n.get(status.getI18nKey()));
  }

  public Node getRoot() {
    return filterUserRoot;
  }
}
