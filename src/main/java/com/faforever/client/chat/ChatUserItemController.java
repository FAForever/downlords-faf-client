package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameService;
import com.faforever.client.game.GameStatus;
import com.faforever.client.game.GamesController;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.player.Player;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.ThemeService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.SocialStatus.SELF;
import static com.faforever.client.util.RatingUtil.getGlobalRating;
import static com.faforever.client.util.RatingUtil.getLeaderboardRating;
import static java.util.Collections.singletonList;
import static java.util.Locale.US;

public class ChatUserItemController {

  private static final String CLAN_TAG_FORMAT = "[%s]";

  @FXML
  Pane chatUserItemRoot;
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
  AvatarService avatarService;
  @Resource
  CountryFlagService countryFlagService;
  @Resource
  GameService gameService;
  @Resource
  PreferencesService preferencesService;
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
  @Resource
  NotificationService notificationService;
  @Resource
  ReportingService reportingService;
  @Resource
  JoinGameHelper joinGameHelper;
  @Resource
  EventBus eventBus;

  private Player player;
  private boolean colorsAllowedInPane;
  private ChangeListener<ChatColorMode> colorModeChangeListener;
  private MapChangeListener<? super String, ? super Color> colorPerUserMapChangeListener;
  private ChangeListener<String> avatarChangeListener;
  private ChangeListener<String> clanChangeListener;
  private ChangeListener<GameStatus> gameStatusChangeListener;

  @FXML
  void initialize() {
    chatUserItemRoot.setUserData(this);
  }

  @FXML
  void onContextMenuRequested(ContextMenuEvent event) {
    ChatUserContextMenuController contextMenuController = applicationContext.getBean(ChatUserContextMenuController.class);
    contextMenuController.setPlayer(player);
    contextMenuController.getContextMenu().show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  @FXML
  void onUsernameClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
    }
  }

  @PostConstruct
  void postConstruct() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    colorModeChangeListener = (observable, oldValue, newValue) -> configureColor();
    colorPerUserMapChangeListener = change -> {
      String lowerUsername = player.getUsername().toLowerCase(US);
      if (lowerUsername.equalsIgnoreCase(change.getKey())) {
        Color newColor = chatPrefs.getUserToColor().get(lowerUsername);
        assignColor(newColor);
      }
    };
    avatarChangeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> setAvatarUrl(newValue));
    clanChangeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> setClanTag(newValue));
    gameStatusChangeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> setGameStatus(newValue));
    joinGameHelper.setParentNode(getRoot());
  }

  private void configureColor() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (player.getSocialStatus() == SELF) {
      usernameLabel.getStyleClass().add(SELF.getCssClass());
      clanLabel.getStyleClass().add(SELF.getCssClass());
      return;
    }

    Color color = null;
    String lowerUsername = player.getUsername().toLowerCase(US);
    ChatUser chatUser = chatService.getOrCreateChatUser(lowerUsername);

    if (chatPrefs.getChatColorMode() == CUSTOM) {
      synchronized (chatPrefs.getUserToColor()) {
        if (chatPrefs.getUserToColor().containsKey(lowerUsername)) {
          color = chatPrefs.getUserToColor().get(lowerUsername);
        }

        chatPrefs.getUserToColor().addListener(new WeakMapChangeListener<>(colorPerUserMapChangeListener));
      }
    } else if (chatPrefs.getChatColorMode() == ChatColorMode.RANDOM && colorsAllowedInPane) {
      color = ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode());
    }

    chatUser.setColor(color);
    assignColor(color);
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
    Image statusImage;
    switch (gameStatus) {
      case PLAYING:
        statusImage = themeService.getThemeImage(ThemeService.PLAYING_STATUS_IMAGE);
        break;
      case HOST:
        statusImage = themeService.getThemeImage(ThemeService.HOSTING_STATUS_IMAGE);
        break;
      case LOBBY:
        statusImage = themeService.getThemeImage(ThemeService.LOBBY_STATUS_IMAGE);
        break;
      default:
        statusImage = null;
    }
    statusImageView.setImage(statusImage);
    statusImageView.setVisible(statusImageView.getImage() != null);
  }

  public Pane getRoot() {
    return chatUserItemRoot;
  }

  public Player getPlayer() {
    return player;
  }

  public void setPlayer(Player player) {
    this.player = player;

    configureColor();
    addChatColorModeListener();
    configureCountryImageView();
    configureAvatarImageView();
    configureClanLabel();
    configureGameStatusView();

    usernameLabel.setText(player.getUsername());
  }

  private void addChatColorModeListener() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    chatPrefs.chatColorModeProperty().addListener(new WeakChangeListener<>(colorModeChangeListener));
  }

  private void configureCountryImageView() {
    setCountry(player.getCountry());

    Tooltip countryTooltip = new Tooltip(player.getCountry());
    countryTooltip.textProperty().bind(player.countryProperty());

    Tooltip.install(countryImageView, countryTooltip);
  }

  private void configureAvatarImageView() {
    player.avatarUrlProperty().addListener(new WeakChangeListener<>(avatarChangeListener));
    setAvatarUrl(player.getAvatarUrl());

    Tooltip avatarTooltip = new Tooltip(player.getAvatarTooltip());
    avatarTooltip.textProperty().bind(player.avatarTooltipProperty());
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

    Tooltip.install(avatarImageView, avatarTooltip);
  }

  private void configureClanLabel() {
    setClanTag(player.getClan());
    player.clanProperty().addListener(new WeakChangeListener<>(clanChangeListener));
  }

  private void configureGameStatusView() {
    setGameStatus(player.getGameStatus());
    player.gameStatusProperty().addListener(new WeakChangeListener<>(gameStatusChangeListener));
  }

  private void setCountry(String country) {
    if (StringUtils.isEmpty(country)) {
      countryImageView.setVisible(false);
    } else {
      countryImageView.setImage(countryFlagService.loadCountryFlag(country));
      countryImageView.setVisible(true);
    }
  }

  @FXML
  void onMouseEnterGameStatus() {
    if (player.getGameStatus() == GameStatus.NONE) {
      return;
    }

    GameStatusTooltipController gameStatusTooltipController = applicationContext.getBean(GameStatusTooltipController.class);
    gameStatusTooltipController.setGameInfoBean(player.getGame());

    Tooltip statusTooltip = new Tooltip();
    statusTooltip.setGraphic(gameStatusTooltipController.getRoot());
    Tooltip.install(statusImageView, statusTooltip);
  }

  @FXML
  void onMouseEnterUsername() {
    if (player.getChatOnly() || usernameLabel.getTooltip() != null) {
      return;
    }

    Tooltip tooltip = new Tooltip();
    Label label = new Label();
    tooltip.setGraphic(label);
    Tooltip.install(usernameLabel, tooltip);
    Tooltip.install(clanLabel, tooltip);

    label.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
        player.leaderboardRatingMeanProperty(), player.leaderboardRatingDeviationProperty(),
        player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()
    ));
  }

  @FXML
  void onMouseClickGameStatus(MouseEvent mouseEvent) {
    GameStatus gameStatus = player.getGameStatus();
    if (gameStatus == GameStatus.NONE) {
      return;
    }
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      int uid = player.getGame().getId();
      if (gameStatus == GameStatus.LOBBY || gameStatus == GameStatus.HOST) {
        Game game = gameService.getByUid(uid);
        joinGameHelper.join(game);
      } else if (gameStatus == GameStatus.PLAYING) {
        try {
          replayService.runLiveReplay(uid, player.getId());
        } catch (IOException e) {
          notificationService.addNotification(new ImmediateNotification(
              i18n.get("errorTitle"), i18n.get("replayCouldNotBeStarted"),
              Severity.ERROR, e, singletonList(new ReportAction(i18n, reportingService, e))
          ));
        }
      }
    }
  }

  public void setColorsAllowedInPane(boolean colorsAllowedInPane) {
    this.colorsAllowedInPane = colorsAllowedInPane;
    configureColor();
  }

  public void setVisible(boolean visible) {
    chatUserItemRoot.setVisible(visible);
    chatUserItemRoot.setManaged(visible);
  }
}
