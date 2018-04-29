package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanService;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.JoinGameHelper;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.PopupWindow;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.chat.SocialStatus.SELF;
import static com.faforever.client.game.PlayerStatus.IDLE;
import static com.faforever.client.util.RatingUtil.getGlobalRating;
import static com.faforever.client.util.RatingUtil.getLeaderboardRating;
import static java.time.Instant.now;
import static java.util.Locale.US;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO null safety for "player"
public class ChatUserItemController implements Controller<Node> {

  private static final String CLAN_TAG_FORMAT = "[%s]";
  private static final PseudoClass PRESENCE_STATUS_ONLINE = PseudoClass.getPseudoClass("online");
  private static final PseudoClass PRESENCE_STATUS_IDLE = PseudoClass.getPseudoClass("idle");
  private final AvatarService avatarService;
  private final CountryFlagService countryFlagService;
  private final PreferencesService preferencesService;
  private final ChatService chatService;
  private final I18n i18n;
  private final UiService uiService;
  private final JoinGameHelper joinGameHelper;
  private final EventBus eventBus;
  private final PlayerService playerService;
  private final ClanService clanService;
  private final PlatformService platformService;
  private final TimeService timeService;
  public Pane chatUserItemRoot;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public MenuButton clanMenu;
  public Label statusLabel;
  public Text presenceStatusIndicator;
  private Player player;
  private boolean randomColorsAllowedInPane;
  private ChangeListener<ChatColorMode> colorModeChangeListener;
  private MapChangeListener<String, Color> colorPerUserMapChangeListener;
  private ChangeListener<String> avatarChangeListener;
  private ChangeListener<String> clanChangeListener;
  private ChangeListener<PlayerStatus> gameStatusChangeListener;
  private InvalidationListener userActivityListener;
  private ClanTooltipController clanTooltipController;

  @Inject
  // TODO reduce dependencies, rely on eventBus instead
  public ChatUserItemController(PreferencesService preferencesService, AvatarService avatarService,
                                CountryFlagService countryFlagService, ChatService chatService,
                                I18n i18n, UiService uiService,
                                JoinGameHelper joinGameHelper, EventBus eventBus,
                                ClanService clanService, PlayerService playerService,
                                PlatformService platformService, TimeService timeService) {
    this.platformService = platformService;
    this.preferencesService = preferencesService;
    this.avatarService = avatarService;
    this.playerService = playerService;
    this.clanService = clanService;
    this.countryFlagService = countryFlagService;
    this.chatService = chatService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.joinGameHelper = joinGameHelper;
    this.eventBus = eventBus;
    this.timeService = timeService;
  }

  public void initialize() {
    userActivityListener = (observable) -> Platform.runLater(this::onUserActivity);

    // TODO until server side support is available, the presence status is initially set to "unknown" until the user
    // does something
    presenceStatusIndicator.setText("\uF10C");
    setIdle(false);

    chatUserItemRoot.setUserData(this);
    countryImageView.managedProperty().bind(countryImageView.visibleProperty());
    countryImageView.setVisible(false);
    statusLabel.managedProperty().bind(statusLabel.visibleProperty());
    statusLabel.visibleProperty().bind(statusLabel.textProperty().isNotEmpty());
    clanMenu.managedProperty().bind(clanMenu.visibleProperty());
    clanMenu.setVisible(false);

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
    gameStatusChangeListener = (observable, oldValue, newValue) -> Platform.runLater(this::updateGameStatus);
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    ChatUserContextMenuController contextMenuController = uiService.loadFxml("theme/chat/chat_user_context_menu.fxml");
    contextMenuController.setPlayer(player);
    contextMenuController.getContextMenu().show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  public void onItemClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(player.getUsername()));
    }
  }

  private void configureColor() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    if (player.getSocialStatus() == SELF) {
      usernameLabel.getStyleClass().add(SELF.getCssClass());
      clanMenu.getStyleClass().add(SELF.getCssClass());
    }

    Color color = null;
    String lowerUsername = player.getUsername().toLowerCase(US);
    ChatUser chatUser = chatService.getOrCreateChatUser(lowerUsername);

    if (chatPrefs.getChatColorMode() == CUSTOM) {
      if (chatPrefs.getUserToColor().containsKey(lowerUsername)) {
        color = chatPrefs.getUserToColor().get(lowerUsername);
      }

      JavaFxUtil.addListener(chatPrefs.getUserToColor(), new WeakMapChangeListener<>(colorPerUserMapChangeListener));
    } else if (chatPrefs.getChatColorMode() == ChatColorMode.RANDOM && randomColorsAllowedInPane) {
      color = ColorGeneratorUtil.generateRandomColor(chatUser.getUsername().hashCode());
    }

    chatUser.setColor(color);
    assignColor(color);
  }

  private void assignColor(Color color) {
    if (color != null) {
      usernameLabel.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
      clanMenu.setStyle(String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color)));
    } else {
      usernameLabel.setStyle("");
      clanMenu.setStyle("");
    }
  }

  private void setAvatarUrl(@Nullable String avatarUrl) {
    if (StringUtils.isEmpty(avatarUrl)) {
      avatarImageView.setVisible(false);
    } else {
      CompletableFuture.supplyAsync(() -> avatarService.loadAvatar(avatarUrl)).thenAccept(image -> avatarImageView.setImage(image));
      avatarImageView.setVisible(true);
    }
  }

  private void setClanTag(String newValue) {
    if (StringUtils.isEmpty(newValue)) {
      clanMenu.setVisible(false);
      return;
    }
    Optional.ofNullable(usernameLabel.getTooltip()).ifPresent(tooltip -> usernameLabel.setTooltip(null));
    Optional.ofNullable(clanMenu.getTooltip()).ifPresent(tooltip -> clanMenu.setTooltip(null));
    clanMenu.setText(String.format(CLAN_TAG_FORMAT, newValue));
    clanMenu.setVisible(true);
  }

  private void updateGameStatus() {
    switch (player.getStatus()) {
      case IDLE:
        statusLabel.setText("");
        break;
      case HOSTING:
        statusLabel.setText(i18n.get("user.status.hosting", player.getGame().getTitle()));
        break;
      case LOBBYING:
        statusLabel.setText(i18n.get("user.status.waiting", player.getGame().getTitle()));
        break;
      case PLAYING:
        statusLabel.setText(i18n.get("user.status.playing", player.getGame().getTitle()));
        break;
    }
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
    configureClanMenu();
    configureGameStatusView();

    usernameLabel.setText(player.getUsername());
    JavaFxUtil.addListener(player.idleSinceProperty(), new WeakInvalidationListener(userActivityListener));
    JavaFxUtil.addListener(player.statusProperty(), new WeakInvalidationListener(userActivityListener));
  }

  private void addChatColorModeListener() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), new WeakChangeListener<>(colorModeChangeListener));
  }

  private void configureCountryImageView() {
    setCountry(player.getCountry());

    countryImageView.setVisible(true);

    if (!StringUtils.isEmpty(player.getCountry())) {
      final Tooltip countryTooltip = new Tooltip(i18n.getCountryNameLocalized(player.getCountry()));
      //countryTooltip.textProperty().bind(player.countryProperty());
      Tooltip.install(countryImageView, countryTooltip);
    }
  }

  private void configureAvatarImageView() {
    JavaFxUtil.addListener(player.avatarUrlProperty(), new WeakChangeListener<>(avatarChangeListener));
    setAvatarUrl(player.getAvatarUrl());

    Tooltip avatarTooltip = new Tooltip(player.getAvatarTooltip());
    avatarTooltip.textProperty().bind(player.avatarTooltipProperty());
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

    Tooltip.install(avatarImageView, avatarTooltip);
  }

  private void configureClanMenu() {
    setClanTag(player.getClan());
    JavaFxUtil.addListener(player.clanProperty(), new WeakChangeListener<>(clanChangeListener));
  }

  private void configureGameStatusView() {
    JavaFxUtil.addListener(player.statusProperty(), new WeakChangeListener<>(gameStatusChangeListener));
    updateGameStatus();
  }

  private void setCountry(String country) {
    if (StringUtils.isEmpty(country)) {
      countryImageView.setVisible(false);
    } else {
      countryFlagService.loadCountryFlag(country).ifPresent(image -> {
        countryImageView.setImage(image);
        countryImageView.setVisible(true);
      });
    }
  }

  public void onMouseEnterUsername() {
    if (player == null || player.getChatOnly() || usernameLabel.getTooltip() != null) {
      return;
    }
    Tooltip userTooltip = new Tooltip();
    usernameLabel.setTooltip(userTooltip);
    userTooltip.textProperty().bind(Bindings.createStringBinding(
        () -> String.format("%s\n%s",
            i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
            i18n.get("userInfo.idleTimeFormat", timeService.timeAgo(player.getIdleSince()))),
        player.leaderboardRatingMeanProperty(), player.leaderboardRatingDeviationProperty(),
        player.globalRatingMeanProperty(), player.globalRatingDeviationProperty(), player.idleSinceProperty()
    ));
  }

  void setRandomColorsAllowedInPane(boolean randomColorsAllowedInPane) {
    this.randomColorsAllowedInPane = randomColorsAllowedInPane;
    configureColor();
  }

  public void setVisible(boolean visible) {
    chatUserItemRoot.setVisible(visible);
    chatUserItemRoot.setManaged(visible);
  }

  /**
   * Updates the displayed idle indicator (online/idle). This is called from outside in order to only have one timer per
   * channel, instead of one timer per chat user.
   */
  void updatePresenceStatusIndicator() {
    JavaFxUtil.assertApplicationThread();

    if (player == null || player.getStatus() != IDLE) {
      setIdle(false);
      return;
    }
    int idleThreshold = preferencesService.getPreferences().getChat().getIdleThreshold();
    setIdle(player.getIdleSince().isBefore(now().minus(Duration.ofMinutes(idleThreshold))));
  }

  private void setIdle(boolean idle) {
    presenceStatusIndicator.pseudoClassStateChanged(PRESENCE_STATUS_ONLINE, !idle);
    presenceStatusIndicator.pseudoClassStateChanged(PRESENCE_STATUS_IDLE, idle);
    if (idle) {
      // TODO only until server-side support
      presenceStatusIndicator.setText("\uF111");
    }
  }

  private void onUserActivity() {
    // TODO only until server-side support
    presenceStatusIndicator.setText("\uF111");
    updatePresenceStatusIndicator();
  }

  public void onMouseEnteredClanTag() {
    clanService.getClanByTag(player.getClan()).thenAccept(optionalClan -> {
      if (!optionalClan.isPresent()) {
        return;
      }

      clanMenu.getItems().clear();

      Clan clan = optionalClan.get();
      if (playerService.isOnline(clan.getLeader().getId())) {
        MenuItem messageLeaderItem = new MenuItem(i18n.get("clan.messageLeader"));
        messageLeaderItem.setOnAction(event -> eventBus.post(new InitiatePrivateChatEvent(clan.getLeader().getUsername())));
        clanMenu.getItems().add(messageLeaderItem);
      }

      MenuItem visitClanPageAction = new MenuItem(i18n.get("clan.visitPage"));
      visitClanPageAction.setOnAction(event -> {
        platformService.showDocument(clan.getWebsiteUrl());
        // TODO: Could be viewed in clan section (if implemented)
      });
      clanMenu.getItems().add(visitClanPageAction);

      if (clanMenu.getTooltip() != null) {
        clanTooltipController.setClan(clan);
        return;
      }

      Tooltip clanTooltip = new Tooltip();
      clanTooltipController = uiService.loadFxml("theme/chat/clan_tooltip.fxml");
      clanTooltip.setMaxHeight(clanTooltipController.getRoot().getHeight());
      clanTooltip.setGraphic(clanTooltipController.getRoot());
      clanTooltipController.setClan(clan);
      clanMenu.setTooltip(clanTooltip);
      clanTooltip.show(StageHolder.getStage());

    });
  }
}
