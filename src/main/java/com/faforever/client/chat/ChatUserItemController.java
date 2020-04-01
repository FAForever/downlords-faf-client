package com.faforever.client.chat;

import com.faforever.client.chat.avatar.AvatarService;
import com.faforever.client.clan.Clan;
import com.faforever.client.clan.ClanService;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.faforever.client.chat.ChatColorMode.CUSTOM;
import static com.faforever.client.player.SocialStatus.SELF;
import static com.faforever.client.util.RatingUtil.getGlobalRating;
import static com.faforever.client.util.RatingUtil.getLeaderboardRating;
import static java.util.Locale.US;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO null safety for "player"
public class ChatUserItemController implements Controller<Node> {

  private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");

  private static volatile Map<PlayerStatus, Image> playerStatusIcons;

  private final AvatarService avatarService;
  private final CountryFlagService countryFlagService;
  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final UiService uiService;
  private final EventBus eventBus;
  private final PlayerService playerService;
  private final ClanService clanService;
  private final PlatformService platformService;
  private final TimeService timeService;

  private final InvalidationListener colorChangeListener;
  private final InvalidationListener formatChangeListener;
  private final InvalidationListener colorPerUserInvalidationListener;
  private final ChangeListener<String> avatarChangeListener;
  private final ChangeListener<String> clanChangeListener;
  private final ChangeListener<String> countryChangeListener;
  private final ChangeListener<PlayerStatus> gameStatusChangeListener;
  private final ChangeListener<Player> playerChangeListener;
  private final InvalidationListener userActivityListener;
  private final InvalidationListener usernameInvalidationListener;
  private final WeakChangeListener<Player> weakPlayerChangeListener;
  private final WeakInvalidationListener weakUsernameInvalidationListener;
  private final WeakInvalidationListener weakColorInvalidationListener;
  private final WeakInvalidationListener weakFormatInvalidationListener;
  private final WeakInvalidationListener weakUserActivityListener;
  private final WeakChangeListener<PlayerStatus> weakGameStatusListener;
  private final WeakChangeListener<String> weakAvatarChangeListener;
  private final WeakChangeListener<String> weakClanChangeListener;
  private final WeakChangeListener<String> weakCountryChangeListener;
  private final WeakInvalidationListener weakColorPerUserInvalidationListener;

  public ImageView playerStatusIndicator;

  public Pane chatUserItemRoot;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public MenuButton clanMenu;
  private ChatChannelUser chatUser;
  @VisibleForTesting
  protected Tooltip countryTooltip;
  @VisibleForTesting
  protected Tooltip avatarTooltip;
  @VisibleForTesting
  protected Tooltip userTooltip;
  private Clan clan;
  private ClanTooltipController clanTooltipController;
  private Tooltip clanTooltip;

  // TODO reduce dependencies, rely on eventBus instead
  public ChatUserItemController(PreferencesService preferencesService, AvatarService avatarService,
                                CountryFlagService countryFlagService,
                                I18n i18n, UiService uiService, EventBus eventBus,
                                ClanService clanService, PlayerService playerService,
                                PlatformService platformService, TimeService timeService) {
    this.platformService = platformService;
    this.preferencesService = preferencesService;
    this.avatarService = avatarService;
    this.playerService = playerService;
    this.clanService = clanService;
    this.countryFlagService = countryFlagService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.eventBus = eventBus;
    this.timeService = timeService;
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    colorPerUserInvalidationListener = change -> {
      if (chatUser != null) {
        updateColor();
      }
    };

    colorChangeListener = observable -> updateColor();
    formatChangeListener = observable -> updateFormat();
    weakColorInvalidationListener = new WeakInvalidationListener(colorChangeListener);
    weakFormatInvalidationListener = new WeakInvalidationListener(formatChangeListener);
    weakColorPerUserInvalidationListener = new WeakInvalidationListener(colorPerUserInvalidationListener);

    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), weakColorInvalidationListener);
    JavaFxUtil.addListener(chatPrefs.chatFormatProperty(), weakFormatInvalidationListener);

    userActivityListener = (observable) -> JavaFxUtil.runLater(this::onUserActivity);
    gameStatusChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(this::updateGameStatus);
    avatarChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(() -> setAvatarUrl(newValue));
    clanChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(() -> setClanTag(newValue));
    countryChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(() -> setCountry(newValue));

    weakUserActivityListener = new WeakInvalidationListener(userActivityListener);
    weakGameStatusListener = new WeakChangeListener<>(gameStatusChangeListener);
    weakAvatarChangeListener = new WeakChangeListener<>(avatarChangeListener);
    weakClanChangeListener = new WeakChangeListener<>(clanChangeListener);
    weakCountryChangeListener = new WeakChangeListener<>(countryChangeListener);

    playerChangeListener = (observable, oldValue, newValue) -> JavaFxUtil.runLater(() -> {
      if (oldValue != null) {
        JavaFxUtil.removeListener(oldValue.idleSinceProperty(), weakUserActivityListener);
        JavaFxUtil.removeListener(oldValue.statusProperty(), weakUserActivityListener);
        JavaFxUtil.removeListener(oldValue.statusProperty(), weakGameStatusListener);
        JavaFxUtil.removeListener(oldValue.avatarUrlProperty(), weakAvatarChangeListener);
        JavaFxUtil.removeListener(oldValue.clanProperty(), weakClanChangeListener);
        JavaFxUtil.removeListener(oldValue.countryProperty(), weakCountryChangeListener);

        weakGameStatusListener.changed(oldValue.statusProperty(), oldValue.getStatus(), null);
        weakAvatarChangeListener.changed(oldValue.avatarUrlProperty(), oldValue.getAvatarUrl(), null);
        weakClanChangeListener.changed(oldValue.clanProperty(), oldValue.getClan(), null);
        weakCountryChangeListener.changed(oldValue.countryProperty(), oldValue.getCountry(), null);
      }

      if (newValue != null) {
        JavaFxUtil.addListener(newValue.idleSinceProperty(), weakUserActivityListener);
        JavaFxUtil.addListener(newValue.statusProperty(), weakUserActivityListener);
        JavaFxUtil.addListener(newValue.statusProperty(), weakGameStatusListener);
        JavaFxUtil.addListener(newValue.avatarUrlProperty(), weakAvatarChangeListener);
        JavaFxUtil.addListener(newValue.clanProperty(), weakClanChangeListener);
        JavaFxUtil.addListener(newValue.countryProperty(), weakCountryChangeListener);

        weakUserActivityListener.invalidated(newValue.idleSinceProperty());
        weakGameStatusListener.changed(newValue.statusProperty(), null, newValue.getStatus());
        weakAvatarChangeListener.changed(newValue.avatarUrlProperty(), null, newValue.getAvatarUrl());
        weakClanChangeListener.changed(newValue.clanProperty(), null, newValue.getClan());
        weakCountryChangeListener.changed(newValue.countryProperty(), null, newValue.getCountry());
      }

      if (this.chatUser != null) {
        userActivityListener.invalidated(null);
      }
    });
    weakPlayerChangeListener = new WeakChangeListener<>(playerChangeListener);

    usernameInvalidationListener = observable -> {
      updateNameLabelTooltip();
      if (this.chatUser == null) {
        usernameLabel.setText("");
      } else {
        usernameLabel.setText(this.chatUser.getUsername());
      }
    };
    weakUsernameInvalidationListener = new WeakInvalidationListener(usernameInvalidationListener);
  }

  private void initClanTooltip() {
    clanTooltipController = uiService.loadFxml("theme/chat/clan_tooltip.fxml");

    clanTooltip = new Tooltip();
    clanTooltip.setGraphic(clanTooltipController.getRoot());
    Tooltip.install(clanMenu, clanTooltip);
  }

  private void updateFormat() {
    ChatFormat chatFormat = preferencesService.getPreferences().getChat().getChatFormat();
    if (chatFormat == ChatFormat.COMPACT) {
      JavaFxUtil.removeListener(preferencesService.getPreferences().getChat().getUserToColor(), weakColorPerUserInvalidationListener);
      JavaFxUtil.addListener(preferencesService.getPreferences().getChat().getUserToColor(), weakColorPerUserInvalidationListener);
    }
    getRoot().pseudoClassStateChanged(
        COMPACT,
        chatFormat == ChatFormat.COMPACT
    );
  }

  public void initialize() {
    chatUserItemRoot.setUserData(this);
    countryImageView.managedProperty().bind(countryImageView.visibleProperty());
    countryImageView.setVisible(false);
    clanMenu.managedProperty().bind(clanMenu.visibleProperty());
    clanMenu.setVisible(false);

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    weakColorInvalidationListener.invalidated(chatPrefs.chatColorModeProperty());
    weakFormatInvalidationListener.invalidated(chatPrefs.chatFormatProperty());

    updateFormat();
    initClanTooltip();
  }

  private WeakReference<ChatUserContextMenuController> contextMenuController = null;

  public void onContextMenuRequested(ContextMenuEvent event) {
    if (contextMenuController != null) {
      ChatUserContextMenuController controller = contextMenuController.get();
      if (controller != null) {
        controller.getContextMenu().show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        return;
      }
    }

    ChatUserContextMenuController controller = uiService.loadFxml("theme/chat/chat_user_context_menu.fxml");
    controller.setChatUser(chatUser);
    controller.getContextMenu().show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());

    contextMenuController = new WeakReference<>(controller);
  }

  public void onItemClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(chatUser.getUsername()));
    }
  }

  private void updateColor() {
    if (chatUser == null) {
      return;
    }
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    chatUser.getPlayer().ifPresent(player -> {
      if (player.getSocialStatus() == SELF) {
        usernameLabel.getStyleClass().add(SELF.getCssClass());
        clanMenu.getStyleClass().add(SELF.getCssClass());
      }
    });

    Color color = null;
    String lowerUsername = chatUser.getUsername().toLowerCase(US);

    if (chatPrefs.getChatColorMode() == CUSTOM) {
      if (chatPrefs.getUserToColor().containsKey(lowerUsername)) {
        color = chatPrefs.getUserToColor().get(lowerUsername);
      }
    } else if (chatPrefs.getChatColorMode() == ChatColorMode.RANDOM) {
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
    updateAvatarTooltip();
    if (Strings.isNullOrEmpty(avatarUrl)) {
      avatarImageView.setVisible(false);
    } else {
      // Loading the avatar image asynchronously would be better but asynchronous list cell updates don't work well
      avatarImageView.setImage(avatarService.loadAvatar(avatarUrl));
      avatarImageView.setVisible(true);
    }
  }

  private void setClanTag(String clanTag) {
    if (!chatUser.getPlayer().isPresent() || Strings.isNullOrEmpty(clanTag)) {
      clanMenu.setVisible(false);
      return;
    }
    clanMenu.setText(String.format("[%s]", clanTag));
    clanMenu.setVisible(true);
    setClan();
  }

  private static void loadPlayerStatusIcons(UiService uiService) throws IOException {
    playerStatusIcons = new HashMap<>();
    playerStatusIcons.put(PlayerStatus.HOSTING, uiService.getThemeImage(UiService.CHAT_LIST_STATUS_HOSTING));
    playerStatusIcons.put(PlayerStatus.LOBBYING, uiService.getThemeImage(UiService.CHAT_LIST_STATUS_LOBBYING));
    playerStatusIcons.put(PlayerStatus.PLAYING, uiService.getThemeImage(UiService.CHAT_LIST_STATUS_PLAYING));
  }

  public Pane getRoot() {
    return chatUserItemRoot;
  }

  public ChatChannelUser getChatUser() {
    return chatUser;
  }

  public void setChatUser(@Nullable ChatChannelUser chatUser) {
    if (this.chatUser == chatUser) {
      return;
    }

    if (this.chatUser != null) {
      removeListeners(this.chatUser);
    }

    this.chatUser = chatUser;
    if (this.chatUser != null) {
      addListeners(this.chatUser);
    }
  }

  private void updateCountryTooltip() {
    Optional.ofNullable(countryTooltip).ifPresent(imageView -> Tooltip.uninstall(countryImageView, countryTooltip));

    chatUser.getPlayer().ifPresent(player -> {
      countryTooltip = new Tooltip(player.getCountry());
      countryTooltip.setText(player.getCountry());
      Tooltip.install(countryImageView, countryTooltip);
    });
  }

  private void setClan() {
    chatUser.getPlayer().ifPresent(this::setClan);
  }

  private void updateNameLabelTooltip() {
    Optional.ofNullable(usernameLabel.getTooltip()).ifPresent(tooltip -> usernameLabel.setTooltip(null));

    if (chatUser == null || !chatUser.getPlayer().isPresent()) {
      return;
    }

    chatUser.getPlayer().ifPresent(player -> {
      userTooltip = new Tooltip();
      usernameLabel.setTooltip(userTooltip);
      updateNameLabelText(player);
    });
  }

  private void updateNameLabelText(Player player) {
    userTooltip.setText(String.format("%s\n%s",
        i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
        i18n.get("userInfo.idleTimeFormat", timeService.timeAgo(player.getIdleSince()))));
  }

  private void addListeners(@NotNull ChatChannelUser chatUser) {
    JavaFxUtil.addListener(chatUser.usernameProperty(), weakUsernameInvalidationListener);
    JavaFxUtil.addListener(chatUser.colorProperty(), weakColorInvalidationListener);
    JavaFxUtil.addListener(chatUser.playerProperty(), weakPlayerChangeListener);

    weakUsernameInvalidationListener.invalidated(chatUser.usernameProperty());
    weakColorInvalidationListener.invalidated(chatUser.colorProperty());
    weakPlayerChangeListener.changed(chatUser.playerProperty(), null, chatUser.getPlayer().orElse(null));
  }

  private void removeListeners(@NotNull ChatChannelUser chatUser) {
    JavaFxUtil.removeListener(chatUser.usernameProperty(), weakUsernameInvalidationListener);
    JavaFxUtil.removeListener(chatUser.colorProperty(), weakColorInvalidationListener);
    JavaFxUtil.removeListener(chatUser.playerProperty(), weakPlayerChangeListener);

    weakPlayerChangeListener.changed(chatUser.playerProperty(), chatUser.getPlayer().orElse(null), null);
  }

  private void setCountry(String country) {
    if (StringUtils.isEmpty(country)) {
      countryImageView.setVisible(false);
    } else {
      countryFlagService.loadCountryFlag(country).ifPresent(image -> {
        countryImageView.setImage(image);
        countryImageView.setVisible(true);
        updateCountryTooltip();
      });
    }
  }

  public void setVisible(boolean visible) {
    chatUserItemRoot.setVisible(visible);
    chatUserItemRoot.setManaged(visible);
  }

  private void onUserActivity() {
    // TODO only until server-side support
    updateGameStatus();
    if (chatUser.getPlayer().isPresent() && userTooltip != null) {
      updateNameLabelText(chatUser.getPlayer().get());
    }
  }

  private void setClan(Player player) {
    clanService.getClanByTag(player.getClan())
        .thenAccept(optionalClan -> JavaFxUtil.runLater(() -> setClan(optionalClan)))
        .exceptionally(throwable -> {
          log.warn("Clan was not updated", throwable);
          return null;
        });
  }

  private void setClan(Optional<Clan> optionalClan) {
    clan = optionalClan.orElse(null);
  }

  public void updateAvatarTooltip() {
    Optional.ofNullable(avatarTooltip).ifPresent(tooltip -> Tooltip.uninstall(avatarImageView, tooltip));

    chatUser.getPlayer().ifPresent(player -> {
      avatarTooltip = new Tooltip(player.getAvatarTooltip());
      avatarTooltip.textProperty().bind(player.avatarTooltipProperty());
      avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

      Tooltip.install(avatarImageView, avatarTooltip);
    });
  }

  private void updateGameStatus() {
    if (chatUser == null) {
      return;
    }
    Optional<Player> playerOptional = chatUser.getPlayer();
    if (!playerOptional.isPresent()) {
      playerStatusIndicator.setVisible(false);
      return;
    }

    Player player = playerOptional.get();

    if (player.getStatus() == PlayerStatus.IDLE) {
      playerStatusIndicator.setVisible(false);
    } else {
      playerStatusIndicator.setVisible(true);
      playerStatusIndicator.setImage(getPlayerStatusIcon(player.getStatus()));
    }
  }

  @SneakyThrows(IOException.class)
  private Image getPlayerStatusIcon(PlayerStatus status) {
    if (playerStatusIcons == null) {
      loadPlayerStatusIcons(uiService);
    }

    return playerStatusIcons.get(status);
  }

  public void onMouseEnteredUserNameLabel() {
    chatUser.getPlayer().ifPresent(this::updateNameLabelText);
  }

  /**
   * Memory analysis suggest this menu uses tons of memory while it stays unclear why exactly (something java fx
   * internal). We just load the menu on click. Also we destroy it over and over again.
   */
  public void onClanMenuRequested() {
    clanMenu.getItems().clear();
    if (clan == null) {
      return;
    }

    Player currentPlayer = playerService.getCurrentPlayer()
        .orElseThrow(() -> new IllegalStateException("Player has to be set"));

    if (currentPlayer.getId() != clan.getLeader().getId()
        && playerService.isOnline(clan.getLeader().getId())) {
      MenuItem messageLeaderItem = new MenuItem(i18n.get("clan.messageLeader"));
      messageLeaderItem.setOnAction(event -> eventBus.post(new InitiatePrivateChatEvent(clan.getLeader().getUsername())));
      clanMenu.getItems().add(messageLeaderItem);
    }

    MenuItem visitClanPageAction = new MenuItem(i18n.get("clan.visitPage"));
    visitClanPageAction.setOnAction(event -> platformService.showDocument(clan.getWebsiteUrl()));
    clanMenu.getItems().add(visitClanPageAction);

    //reload to load newly set UI
    clanMenu.hide();
    clanMenu.show();
  }

  /**
   * Because of a bug in java fx tooltip when controls are updated without the tooltip showing the ui is leaked, so we
   * only update when the user hover over the tooltip. This will reduce that to a minimum.
   */
  public void updateClanData() {
    if (clan == null) {
      return;
    }
    clanTooltipController.setClan(clan);
    clanTooltip.setMaxHeight(clanTooltipController.getRoot().getHeight());
  }
}
