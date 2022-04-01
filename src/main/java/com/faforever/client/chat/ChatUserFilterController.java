package com.faforever.client.chat;

import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.util.RatingUtil;
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
  public TextField clanTextField;
  public TextField minRatingTextField;
  public TextField maxRatingTextField;
  public ToggleGroup gameStatusToggleGroup;
  public TextField countryTextField;

  private FilteredList<ListItem> userList;
  private TextField searchUsernameTextField;
  private PlayerStatus playerStatus;
  private List<String> currentSelectedCountries;
  
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
    JavaFxUtil.addListener(clanTextField.textProperty(), textPropertyWeakListener);
    JavaFxUtil.addListener(minRatingTextField.textProperty(), textPropertyWeakListener);
    JavaFxUtil.addListener(maxRatingTextField.textProperty(), textPropertyWeakListener);

    countryTextPropertyListener = observable -> {
      currentSelectedCountries = flagService.getCountries(countryTextField.textProperty().get());
      filterUsers();
    };
    JavaFxUtil.addListener(countryTextField.textProperty(), new WeakInvalidationListener(countryTextPropertyListener));
  }

  private void filterUsers() {
    userList.setPredicate(this::filterUser);
    filterApplied.set(
        !maxRatingTextField.getText().isEmpty()
            || !minRatingTextField.getText().isEmpty()
            || !clanTextField.getText().isEmpty()
            || playerStatus != null
            || !countryTextField.getText().isEmpty()
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

  private boolean isInClan(ChatChannelUser chatUser) {
    return clanTextField.getText().isEmpty() ||
        chatUser.getPlayer().map(PlayerBean::getClan)
            .map(String::toLowerCase)
            .stream().anyMatch(clan -> clan.contains(clanTextField.getText().toLowerCase()));
  }

  private boolean isBoundByRating(ChatChannelUser chatUser) {
    if (minRatingTextField.getText().isEmpty() && maxRatingTextField.getText().isEmpty()) {
      return true;
    }

    // TODO filter by specific leaderboard rating remove hardcoded value
    return chatUser.getPlayer().map(player -> RatingUtil.getLeaderboardRating(player, "global"))
        .stream().anyMatch(rating -> {
          int minRating, maxRating;

          try {
            minRating = Integer.parseInt(minRatingTextField.getText());
          } catch (NumberFormatException e) {
            minRating = Integer.MIN_VALUE;
          }
          try {
            maxRating = Integer.parseInt(maxRatingTextField.getText());
          } catch (NumberFormatException e) {
            maxRating = Integer.MAX_VALUE;
          }
          return rating >= minRating && rating <= maxRating;
        });
  }

  private boolean isGameStatusMatch(ChatChannelUser chatUser) {
    return playerStatus == null || chatUser.getPlayer().map(PlayerBean::getStatus)
        .stream().anyMatch(status -> {
          if (this.playerStatus == LOBBYING) {
            return LOBBYING == status || HOSTING == status;
          } else {
            return this.playerStatus == status;
          }
        });
  }

  boolean isCountryMatch(ChatChannelUser chatUser) {
    // Users of  'chat only' group have no country so that need to check it for empty string
    return countryTextField.getText().isEmpty() || chatUser.getPlayer().map(PlayerBean::getCountry)
        .stream().anyMatch(country -> currentSelectedCountries.contains(country));
  }

  public void onGameStatusPlaying() {
    updateGameStatusMenuText(playerStatus == PLAYING ? null : PLAYING);
    filterUsers();
  }

  public void onGameStatusLobby() {
    updateGameStatusMenuText(playerStatus == LOBBYING ? null : LOBBYING);
    filterUsers();
  }

  public void onGameStatusNone() {
    updateGameStatusMenuText(playerStatus == IDLE ? null : IDLE);
    filterUsers();
  }

  private void updateGameStatusMenuText(PlayerStatus status) {
    this.playerStatus = status;
    gameStatusMenu.setText(i18n.get(status == null ? "game.gameStatus" : status.getI18nKey()));
    if (status == null) {
      gameStatusToggleGroup.selectToggle(null);
    }
  }

  public Node getRoot() {
    return root;
  }
}
