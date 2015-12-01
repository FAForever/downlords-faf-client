package com.faforever.client.chat;

import com.faforever.client.ThemeService;
import com.faforever.client.fx.FxmlLoader;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GamesController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.legacy.GameStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.util.RatingUtil;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.SocialStatus.SELF;

public class ChatUserControl extends HBox {

  private static final String CLAN_TAG_FORMAT = "[%s]";
  // TODO @Aulex I thought this changed, please review and clean up if necessary
  private static final String CSS_CLASS_SELF = "self";

  @FXML
  ImageView countryImageView;
  @FXML
  ImageView avatarImageView;
  @FXML
  Label usernameLabel;
  @FXML
  Label clanLabel;
  @FXML
  ImageView statusImageView;

  @Resource
  ApplicationContext applicationContext;
  @Resource
  FxmlLoader fxmlLoader;
  @Resource
  AvatarService avatarService;
  @Resource
  CountryFlagService countryFlagService;
  @Resource
  ChatController chatController;
  @Resource
  GameService gameService;
  @Resource
  PreferencesService preferencesService;
  @Resource
  UserService userService;
  @Resource
  ChatService chatService;
  @Resource
  GamesController gamesController;
  @Resource
  ReplayService replayService;
  @Resource
  I18n i18n;
  @Resource
  ThemeService themeService;

  private PlayerInfoBean playerInfoBean;
  private boolean colorsAllowedInPane;

  @FXML
  void onContextMenuRequested(ContextMenuEvent event) {
    ChatUserContextMenuController contextMenuController = applicationContext.getBean(ChatUserContextMenuController.class);
    contextMenuController.setPlayerInfoBean(playerInfoBean);
    contextMenuController.getContextMenu().show(getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  @FXML
  void onUsernameClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      chatController.openPrivateMessageTabForUser(playerInfoBean.getUsername());
    }
  }

  @PostConstruct
  void init() {
    fxmlLoader.loadCustomControl("chat_user_control.fxml", this);
  }

  public PlayerInfoBean getPlayerInfoBean() {
    return playerInfoBean;
  }

  public void setPlayerInfoBean(PlayerInfoBean playerInfoBean) {
    this.playerInfoBean = playerInfoBean;

    configureColor();
    addChatColorModeListener();
    configureCountryImageView();
    configureAvatarImageView();
    configureClanLabel();
    configureGameStatusView();
    configureRatingTooltip();

    usernameLabel.setText(playerInfoBean.getUsername());
  }

  private void configureColor() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (playerInfoBean.getSocialStatus() == SELF) {
      getStyleClass().add(CSS_CLASS_SELF);
      return;
    }

    Color color = null;
    ChatUser chatUser = chatService.createOrGetChatUser(playerInfoBean.getUsername());

    if (chatPrefs.getChatColorMode().equals(CUSTOM)) {
      if (chatPrefs.getUserToColor().containsKey(playerInfoBean.getUsername())) {
        color = chatPrefs.getUserToColor().get(playerInfoBean.getUsername());
      }

      //FIXME: something here returned NPE when starting chat and users starting messaging aeolus on non-dev server
      chatPrefs.getUserToColor().addListener((MapChangeListener<? super String, ? super Color>) change -> {
        if (playerInfoBean.getUsername().equals(change.getKey())) {
          Color newColor = chatPrefs.getUserToColor().get(playerInfoBean.getUsername());
          assignColor(newColor);
        }
      });
    } else if (chatPrefs.getChatColorMode().equals(ChatColorMode.RANDOM) && colorsAllowedInPane) {
      color = ColorGeneratorUtil.generateRandomHexColor();
    }

    chatUser.setColor(color);
    assignColor(color);
  }

  private void addChatColorModeListener() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    chatPrefs.chatColorModeProperty().addListener((observable, oldValue, newValue) -> {
      configureColor();
    });
  }

  private void configureCountryImageView() {
    setCountry(playerInfoBean.getCountry());

    Tooltip countryTooltip = new Tooltip(playerInfoBean.getCountry());
    countryTooltip.textProperty().bind(playerInfoBean.countryProperty());

    Tooltip.install(countryImageView, countryTooltip);
  }

  private void configureAvatarImageView() {
    playerInfoBean.avatarUrlProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> setAvatarUrl(newValue));
    });
    setAvatarUrl(playerInfoBean.getAvatarUrl());

    Tooltip avatarTooltip = new Tooltip(playerInfoBean.getAvatarTooltip());
    avatarTooltip.textProperty().bind(playerInfoBean.avatarTooltipProperty());
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

    Tooltip.install(avatarImageView, avatarTooltip);
  }

  private void configureClanLabel() {
    setClanTag(playerInfoBean.getClan());
    playerInfoBean.clanProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> setClanTag(newValue));
    });
  }

  private void configureGameStatusView() {
    setGameStatus(playerInfoBean.getGameStatus());
    playerInfoBean.gameStatusProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> setGameStatus(newValue));
    });

  }

  private void configureRatingTooltip() {
    if (!playerInfoBean.getChatOnly()) {
      Tooltip userRatingTooltip = new Tooltip();

      String rating = i18n.get("userInfo.ratingFormat", RatingUtil.getRoundedGlobalRating(playerInfoBean), RatingUtil.getLeaderboardRating(playerInfoBean));
      userRatingTooltip.setText(rating);

      addRatingListenerToTooltip(playerInfoBean.leaderboardRatingMeanProperty(), userRatingTooltip);
      addRatingListenerToTooltip(playerInfoBean.globalRatingMeanProperty(), userRatingTooltip);

      Tooltip.install(clanLabel, userRatingTooltip);
      Tooltip.install(usernameLabel, userRatingTooltip);
    }
  }

  private void assignColor(Color color) {
    if (color != null) {
      usernameLabel.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
      clanLabel.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
    } else {
      usernameLabel.setStyle("");
      clanLabel.setStyle("");
    }
  }

  private void setCountry(String country) {
    if (StringUtils.isEmpty(country)) {
      countryImageView.setVisible(false);
    } else {
      countryImageView.setImage(countryFlagService.loadCountryFlag(country));
      countryImageView.setVisible(true);
    }
  }

  private void setAvatarUrl(String avatarUrl) {
    if (StringUtils.isEmpty(avatarUrl)) {
      avatarImageView.setVisible(false);
    } else {
      avatarImageView.setImage(avatarService.loadAvatar(avatarUrl));
      avatarImageView.setVisible(true);
    }
  }

  private void setClanTag(String newValue) {
    if (StringUtils.isEmpty(newValue)) {
      clanLabel.setVisible(false);
    } else {
      clanLabel.setText(String.format(CLAN_TAG_FORMAT, newValue));
      clanLabel.setVisible(true);
    }
  }

  public void setGameStatus(GameStatus gameStatus) {
    switch (gameStatus) {
      case PLAYING:
        statusImageView.setImage(new Image(themeService.getThemeFile(ThemeService.PLAYING_STATUS_IMAGE)));
        break;
      case HOST:
        statusImageView.setImage(new Image(themeService.getThemeFile(ThemeService.HOSTING_STATUS_IMAGE)));
        break;
      case LOBBY:
        statusImageView.setImage(new Image(themeService.getThemeFile(ThemeService.LOBBY_STATUS_IMAGE)));
        break;
      default:
        statusImageView.setImage(null);
    }
    statusImageView.setVisible(true);
  }

  private void addRatingListenerToTooltip(FloatProperty ratingProperty, Tooltip tooltip) {
    ratingProperty.addListener((observable, oldValue, newValue) -> {
      String updatedRating = i18n.get("userInfo.ratingFormat", RatingUtil.getGlobalRating(playerInfoBean), RatingUtil.getLeaderboardRating(playerInfoBean));
      tooltip.setText(updatedRating);
    });
  }

  @FXML
  void onMouseEnterGameStatus() {
    if (playerInfoBean.getGameStatus() == GameStatus.NONE) {
      return;
    }

    GameStatusTooltipController gameStatusTooltipController = applicationContext.getBean(GameStatusTooltipController.class);
    gameStatusTooltipController.setGameInfoBean(gameService.getByUid(playerInfoBean.getGameUid()));

    Tooltip statusTooltip = new Tooltip();
    statusTooltip.setGraphic(gameStatusTooltipController.getRoot());
    Tooltip.install(statusImageView, statusTooltip);
  }

  @FXML
  void onMouseClickGameStatus(MouseEvent mouseEvent) {
    GameStatus gameStatus = playerInfoBean.getGameStatus();
    if (gameStatus == GameStatus.NONE) {
      return;
    }
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      int uid = playerInfoBean.getGameUid();
      if (gameStatus == GameStatus.LOBBY || gameStatus == GameStatus.HOST) {
        GameInfoBean gameInfoBean = gameService.getByUid(uid);
        gamesController.onJoinGame(gameInfoBean, null, mouseEvent.getScreenX(), mouseEvent.getScreenY());
      } else if (gameStatus == GameStatus.PLAYING) {
        try {
          replayService.runLiveReplay(uid, playerInfoBean.getUsername());
        } catch (IOException e) {
          //FIXME log
        }
      }
    }
  }

  public void setColorsAllowedInPane(boolean colorsAllowedInPane) {
    this.colorsAllowedInPane = colorsAllowedInPane;
    configureColor();
  }
}
