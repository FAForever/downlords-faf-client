package com.faforever.client.chat;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.util.RatingUtil;
import com.google.common.annotations.VisibleForTesting;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.faforever.client.game.PlayerStatus.HOSTING;
import static com.faforever.client.game.PlayerStatus.IDLE;
import static com.faforever.client.game.PlayerStatus.LOBBYING;
import static com.faforever.client.game.PlayerStatus.PLAYING;
import static java.util.Locale.US;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ChatUserFilterController implements Controller<Node> {

  private final I18n i18n;
  private final CountryFlagService flagService;

  public GridPane root;
  public MenuButton gameStatusMenu;
  public TextField clanFilterField;
  public TextField minRatingFilterField;
  public TextField maxRatingFilterField;
  public ToggleGroup gameStatusToggleGroup;
  public TextField countryFilterField;

  private FilteredList<ListItem> userList;
  private TextField searchUsernameTextField;

  @VisibleForTesting
  PlayerStatus playerStatusFilter;
  List<String> currentSelectedCountries;
  
  /* Listeners */
  private final BooleanProperty filterApplied = new SimpleBooleanProperty(false);
  private final InvalidationListener textPropertyListener = observable -> filterUsers();
  @SuppressWarnings("FieldCanBeLocal")
  private InvalidationListener countryTextPropertyListener;

  public void finalizeFiltersSettings(FilteredList<ListItem> userList, TextField searchUsernameTextField) {
    this.userList = userList;
    this.searchUsernameTextField = searchUsernameTextField;
    initializeListeners();
  }

  private void initializeListeners() {
    WeakInvalidationListener  textPropertyWeakListener = new WeakInvalidationListener(textPropertyListener);
    JavaFxUtil.addListener(searchUsernameTextField.textProperty(), textPropertyWeakListener);
    JavaFxUtil.addListener(clanFilterField.textProperty(), textPropertyWeakListener);
    JavaFxUtil.addListener(minRatingFilterField.textProperty(), textPropertyWeakListener);
    JavaFxUtil.addListener(maxRatingFilterField.textProperty(), textPropertyWeakListener);

    countryTextPropertyListener = observable -> {
      currentSelectedCountries = flagService.getCountries(countryFilterField.textProperty().get());
      filterUsers();
    };
    JavaFxUtil.addListener(countryFilterField.textProperty(), new WeakInvalidationListener(countryTextPropertyListener));
  }

  private void filterUsers() {
    userList.setPredicate(this::filterUser);
    filterApplied.set(
        !maxRatingFilterField.getText().isEmpty()
            || !minRatingFilterField.getText().isEmpty()
            || !clanFilterField.getText().isEmpty()
            || playerStatusFilter != null
            || !countryFilterField.getText().isEmpty()
    );
  }

  private boolean filterUser(ListItem item) {
    if (item.getUser().isEmpty()) {
      // It is a category
      return true;
    }
    ChatChannelUser user = item.getUser().get();
    return isUsernameMatch(user)
        && isInClan(user)
        && isBoundByRating(user)
        && isGameStatusMatch(user)
        && isCountryMatch(user);
  }

  boolean isUsernameMatch(ChatChannelUser user) {
    String username = user.getUsername().toLowerCase(US);
    return username.contains(searchUsernameTextField.getText().toLowerCase(US));
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

    Optional<PlayerBean> playerOptional = chatUser.getPlayer();

    if (playerOptional.isEmpty()) {
      return false;
    }

    PlayerBean player = playerOptional.get();
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

    Optional<PlayerBean> optionalPlayer = chatUser.getPlayer();

    if (optionalPlayer.isEmpty()) {
      return false;
    }

    //TODO filter by specifc leaderboard rating remove hardcoded value
    PlayerBean player = optionalPlayer.get();
    int rating = RatingUtil.getLeaderboardRating(player, "global");
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

    return rating >= minRating && rating <= maxRating;
  }

  @VisibleForTesting
  boolean isGameStatusMatch(ChatChannelUser chatUser) {
    if (playerStatusFilter == null) {
      return true;
    }

    Optional<PlayerBean> playerOptional = chatUser.getPlayer();

    if (playerOptional.isEmpty()) {
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
    // Users of  'chat only' group have no country so that need to check it for empty string
    if (countryFilterField.getText().isEmpty()) {
      return true;
    }

    Optional<PlayerBean> playerOptional = chatUser.getPlayer();
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
    return root;
  }
}
