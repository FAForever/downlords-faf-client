package com.faforever.client.chat;

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
import com.faforever.client.util.RatingUtil;
import com.faforever.client.util.ThemeUtil;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class ChatUserControl extends HBox {

  private static final String CLAN_TAG_FORMAT = "[%s]";
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  @Autowired
  ApplicationContext applicationContext;
  @Autowired
  FxmlLoader fxmlLoader;
  @Autowired
  AvatarService avatarService;
  @Autowired
  CountryFlagService countryFlagService;
  @Autowired
  ChatController chatController;
  @Autowired
  GameService gameService;
  @Autowired
  PreferencesService preferencesService;
  @Autowired
  UserService userService;
  @Autowired
  ChatService chatService;
  @Autowired
  GamesController gamesController;
  @Autowired
  ReplayService replayService;
  @Autowired
  Environment environment;
  @Autowired
  I18n i18n;
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
  private PlayerInfoBean playerInfoBean;

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
    addPrettyColorListener();
    configureCountryImageView();
    configureAvatarImageView();
    configureClanLabel();
    configureGameStatusView();
    configureRatingTooltip();

    usernameLabel.setText(playerInfoBean.getUsername());
  }

  private void configureColor() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    ObjectProperty<Color> colorProperty = null;
    Color color = null;

    // Always regardless of pretty Colors enabled
    if (!playerInfoBean.getModeratorForChannels().isEmpty()) {
      colorProperty = chatPrefs.modsChatColorProperty();
    } else if (playerInfoBean.isFriend()) {
      colorProperty = chatPrefs.friendsChatColorProperty();
    } else if (playerInfoBean.isFoe()) {
      colorProperty = chatPrefs.foesChatColorProperty();
    } else if (playerInfoBean.getUsername().equals(userService.getUsername())) {
      colorProperty = chatPrefs.selfChatColorProperty();
    }

    // Only if pretty colors is disabled
    if (!chatPrefs.getPrettyColors() && colorProperty == null) {
      if (playerInfoBean.isChatOnly()) {
        colorProperty = chatPrefs.ircChatColorProperty();
      } else {
        colorProperty = chatPrefs.othersChatColorProperty();
      }

      // Self color
    } else if (chatPrefs.getPrettyColors() && colorProperty == null) {
      color = ColorGeneratorUtil.generatePrettyHexColor();
      ChatUser chatUser = chatService.getChatUser(playerInfoBean.getUsername());

      //FIXME result of chatUser color not being generated on chatUser being initialized
      try {
        if (chatUser.getColor() == null) {
          chatUser.setColor(color);
        } else {
          color = chatUser.getColor();
        }
      } catch (NullPointerException e) {
        logger.warn("Could not generate color for {}", playerInfoBean.getUsername());
      }
    }

    if (colorProperty != null) {
      //FIXME if there is a listener already will this break?
      color = colorProperty.get();
      addColorListenerToLabels(colorProperty);
    }

    usernameLabel.setTextFill(color);
    clanLabel.setTextFill(color);
  }

  private void addPrettyColorListener() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    chatPrefs.prettyColorsProperty().addListener((observable, oldValue, newValue) -> {
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

      String leaderboardRating = i18n.get("playerRatingFormat", RatingUtil.getGlobalRating(playerInfoBean), RatingUtil.getLeaderboardRating(playerInfoBean));
      userRatingTooltip.setText(leaderboardRating);

      addRatingListenerToTooltip(playerInfoBean.leaderboardRatingMeanProperty(), userRatingTooltip);
      addRatingListenerToTooltip(playerInfoBean.globalRatingMeanProperty(), userRatingTooltip);

      Tooltip.install(clanLabel, userRatingTooltip);
      Tooltip.install(usernameLabel, userRatingTooltip);
    }
  }

  private void addColorListenerToLabels(ObjectProperty<Color> colorProperty) {
    colorProperty.addListener((observable, oldValue, newValue) -> {
      usernameLabel.setTextFill(newValue);
      clanLabel.setTextFill(newValue);
    });
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
    String theme = preferencesService.getPreferences().getTheme();
    switch (gameStatus) {
      case PLAYING:
        statusImageView.setImage(new Image(ThemeUtil.themeFile(theme, "images/chat/playing.png")));
        break;
      case HOST:
        statusImageView.setImage(new Image(ThemeUtil.themeFile(theme, "images/chat/host.png")));
        break;
      case LOBBY:
        statusImageView.setImage(new Image(ThemeUtil.themeFile(theme, "images/chat/lobby.png")));
        break;
      // None case
      default:
        statusImageView.setImage(new Image(ThemeUtil.themeFile(theme, "images/chat/none.png")));
    }
    statusImageView.setVisible(true);
  }

  private void addRatingListenerToTooltip(FloatProperty ratingProperty, Tooltip tooltip) {
    ratingProperty.addListener((observable, oldValue, newValue) -> {
      String updatedRating = i18n.get("playerRatingFormat", RatingUtil.getGlobalRating(playerInfoBean), RatingUtil.getLeaderboardRating(playerInfoBean));
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
}
