package com.faforever.client.chat;

import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.contextmenu.AddFoeMenuItem;
import com.faforever.client.fx.contextmenu.AddFriendMenuItem;
import com.faforever.client.fx.contextmenu.BroadcastMessageMenuItem;
import com.faforever.client.fx.contextmenu.ChatUserColorPickerCustomMenuItemController;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.fx.contextmenu.InvitePlayerMenuItem;
import com.faforever.client.fx.contextmenu.JoinGameMenuItem;
import com.faforever.client.fx.contextmenu.KickGameMenuItem;
import com.faforever.client.fx.contextmenu.KickLobbyMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFoeMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFriendMenuItem;
import com.faforever.client.fx.contextmenu.ReportPlayerMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageMenuItem;
import com.faforever.client.fx.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.fx.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.fx.contextmenu.WatchGameMenuItem;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
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
import javafx.stage.PopupWindow;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ChatUserItemController implements Controller<Node> {

  private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");

  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final UiService uiService;
  private final EventBus eventBus;
  private final PlayerService playerService;
  private final PlatformService platformService;
  private final ContextMenuBuilder contextMenuBuilder;

  private final InvalidationListener formatChangeListener = observable -> updateFormat();
  private final InvalidationListener chatUserPropertyInvalidationListener = observable -> updateChatUserDisplay();
  private final InvalidationListener chatUserGamePropertyInvalidationListener = observable -> updateChatUserGame();

  public ImageView playerMapImage;
  public ImageView playerStatusIndicator;
  public Pane chatUserItemRoot;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public MenuButton clanMenu;
  private Tooltip statusGameTooltip;
  private Tooltip gameInfoTooltip;
  private Tooltip countryTooltip;
  private Tooltip avatarTooltip;
  private GameTooltipController gameInfoController;
  private ChatChannelUser chatUser;

  private void updateFormat() {
    ChatFormat chatFormat = preferencesService.getPreferences().getChat().getChatFormat();
    getRoot().pseudoClassStateChanged(
        COMPACT,
        chatFormat == ChatFormat.COMPACT
    );
  }

  public void initialize() {
    chatUserItemRoot.setUserData(this);

    initializeTooltips();

    JavaFxUtil.bindManagedToVisible(countryImageView, clanMenu, playerStatusIndicator, playerMapImage);

    JavaFxUtil.bind(avatarImageView.visibleProperty(), avatarImageView.imageProperty().isNotNull());
    JavaFxUtil.bind(countryImageView.visibleProperty(), countryImageView.imageProperty().isNotNull());
    JavaFxUtil.bind(clanMenu.visibleProperty(), clanMenu.textProperty().isNotEmpty());
    JavaFxUtil.bind(playerStatusIndicator.visibleProperty(), playerStatusIndicator.imageProperty().isNotNull());
    JavaFxUtil.bind(playerMapImage.visibleProperty(), playerMapImage.imageProperty().isNotNull());
    JavaFxUtil.addAndTriggerListener(preferencesService.getPreferences().getChat().chatFormatProperty(), new WeakInvalidationListener(formatChangeListener));

    updateFormat();
    addEventHandlersToPlayerMapImage();
  }

  private void initializeTooltips() {
    avatarTooltip = new Tooltip();
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    Tooltip.install(avatarImageView, avatarTooltip);

    countryTooltip = new Tooltip();
    countryTooltip.showDelayProperty().set(Duration.millis(250));
    Tooltip.install(countryImageView, countryTooltip);

    statusGameTooltip = new Tooltip();
    statusGameTooltip.setShowDuration(Duration.seconds(30));
    statusGameTooltip.setShowDelay(Duration.ZERO);
    statusGameTooltip.setHideDelay(Duration.ZERO);
    Tooltip.install(playerStatusIndicator, statusGameTooltip);
  }

  private void addEventHandlersToPlayerMapImage() {
    playerMapImage.addEventHandler(MouseEvent.MOUSE_MOVED, eventHandlerInitializeGameInfoTooltip());
    playerMapImage.addEventHandler(MouseEvent.MOUSE_EXITED, eventHandlerRemoveGameInfoTooltip());
  }

  private EventHandler<MouseEvent> eventHandlerInitializeGameInfoTooltip() {
    return event -> {
      if (chatUser == null || chatUser.getPlayer().isEmpty() || gameInfoTooltip != null || gameInfoController != null) {
        return;
      }
      gameInfoController = prepareGameInfoController(chatUser.getPlayer().get().getGame());
      gameInfoTooltip = prepareGameInfoTooltip(gameInfoController);
      gameInfoController.displayGame();
      Tooltip.install(playerMapImage, gameInfoTooltip);
    };
  }

  private GameTooltipController prepareGameInfoController(GameBean game) {
    GameTooltipController controller = uiService.loadFxml("theme/play/game_tooltip.fxml");
    controller.setShowMods(false);
    controller.setGame(game);
    return controller;
  }

  private Tooltip prepareGameInfoTooltip(GameTooltipController controller) {
    Tooltip tooltip = JavaFxUtil.createCustomTooltip(controller.getRoot());
    tooltip.setShowDelay(Duration.ZERO);
    tooltip.setShowDuration(Duration.seconds(30));
    return tooltip;
  }

  private EventHandler<MouseEvent> eventHandlerRemoveGameInfoTooltip() {
    return event -> {
      if (gameInfoTooltip == null || gameInfoController == null) {
        return;
      }
      Tooltip.uninstall(playerMapImage, gameInfoTooltip);
      gameInfoTooltip = null;
      gameInfoController = null;
    };
  }

  public void onContextMenuRequested(ContextMenuEvent event) {
    PlayerBean player = chatUser.getPlayer().orElse(null);
    contextMenuBuilder.newBuilder()
        .addItem(ShowPlayerInfoMenuItem.class, player)
        .addItem(SendPrivateMessageMenuItem.class, chatUser.getUsername())
        .addItem(CopyUsernameMenuItem.class, chatUser.getUsername())
        .addCustomItem(uiService.loadFxml("theme/chat/color_picker_menu_item.fxml", ChatUserColorPickerCustomMenuItemController.class), chatUser)
        .addSeparator()
        .addItem(InvitePlayerMenuItem.class, player)
        .addItem(AddFriendMenuItem.class, player)
        .addItem(RemoveFriendMenuItem.class, player)
        .addItem(AddFoeMenuItem.class, player)
        .addItem(RemoveFoeMenuItem.class, player)
        .addSeparator()
        .addItem(ReportPlayerMenuItem.class, player)
        .addSeparator()
        .addItem(JoinGameMenuItem.class, player)
        .addItem(WatchGameMenuItem.class, player)
        .addItem(ViewReplaysMenuItem.class, player)
        .addSeparator()
        .addItem(KickGameMenuItem.class, player)
        .addItem(KickLobbyMenuItem.class, player)
        .addItem(BroadcastMessageMenuItem.class)
        .addCustomItem(uiService.loadFxml("theme/chat/avatar_picker_menu_item.fxml"), player)
        .build()
        .show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  public void onItemClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(chatUser.getUsername()));
    }
  }

  public Pane getRoot() {
    return chatUserItemRoot;
  }

  public ChatChannelUser getChatUser() {
    return chatUser;
  }

  public void setChatUser(ChatChannelUser chatUser) {
    Assert.checkNotNullIllegalState(this.chatUser, "Chat user is already set");

    this.chatUser = chatUser;

    if (this.chatUser != null) {
      this.chatUser.setDisplayed(true);
      addListeners();
    }
  }

  private void addListeners() {
    WeakInvalidationListener weakChatUserPropertyListener = new WeakInvalidationListener(chatUserPropertyInvalidationListener);
    JavaFxUtil.addListener(this.chatUser.usernameProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.colorProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.avatarProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.clanTagProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.countryFlagProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.countryNameProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.playerProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.socialStatusProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addListener(this.chatUser.clanProperty(), weakChatUserPropertyListener);
    JavaFxUtil.addAndTriggerListener(this.chatUser.moderatorProperty(), weakChatUserPropertyListener);

    WeakInvalidationListener weakChatUserGameListener = new WeakInvalidationListener(chatUserGamePropertyInvalidationListener);
    JavaFxUtil.addListener(this.chatUser.lastActiveProperty(), weakChatUserGameListener);
    JavaFxUtil.addListener(this.chatUser.mapImageProperty(), weakChatUserGameListener);
    JavaFxUtil.addListener(this.chatUser.gameStatusImageProperty(), weakChatUserGameListener);
    JavaFxUtil.addAndTriggerListener(this.chatUser.statusTooltipTextProperty(), weakChatUserGameListener);
  }

  private void updateChatUserDisplay() {
    String styleString = chatUser.getColor().map(color -> String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color))).orElse("");
    String avatarString = chatUser.getPlayer().map(PlayerBean::getAvatar).map(AvatarBean::getDescription).orElse("");
    String clanString = chatUser.getClanTag().orElse("");
    JavaFxUtil.runLater(() -> {
      usernameLabel.setText(chatUser.getUsername());
      usernameLabel.setStyle(styleString);
      avatarImageView.setImage(chatUser.getAvatar().orElse(null));
      countryImageView.setImage(chatUser.getCountryFlag().orElse(null));
      countryTooltip.setText(chatUser.getCountryName().orElse(""));
      avatarTooltip.setText(avatarString);
      clanMenu.setText(clanString);
    });
  }

  private void updateChatUserGame() {
    JavaFxUtil.runLater(() -> {
      playerMapImage.setImage(chatUser.getMapImage().orElse(null));
      playerStatusIndicator.setImage(chatUser.getGameStatusImage().orElse(null));
      statusGameTooltip.setText(chatUser.getStatusTooltipText().orElse(""));
    });
  }

  public void setVisible(boolean visible) {
    chatUserItemRoot.setVisible(visible);
    chatUserItemRoot.setManaged(visible);
  }

  /**
   * Memory analysis suggest this menu uses tons of memory while it stays unclear why exactly (something java fx
   * internal). We just load the menu on click. Also we destroy it over and over again.
   */
  public void onClanMenuRequested() {
    clanMenu.getItems().clear();
    clanMenu.hide();
    if (chatUser.getClan().isEmpty()) {
      return;
    }

    ClanBean clan = chatUser.getClan().get();

    PlayerBean currentPlayer = playerService.getCurrentPlayer();

    if (!currentPlayer.getId().equals(clan.getLeader().getId())
        && playerService.isOnline(clan.getLeader().getId())) {
      MenuItem messageLeaderItem = new MenuItem(i18n.get("clan.messageLeader"));
      messageLeaderItem.setOnAction(event -> eventBus.post(new InitiatePrivateChatEvent(clan.getLeader().getUsername())));
      clanMenu.getItems().add(messageLeaderItem);
    }

    MenuItem visitClanPageAction = new MenuItem(i18n.get("clan.visitPage"));
    visitClanPageAction.setOnAction(event -> platformService.showDocument(clan.getWebsiteUrl()));
    clanMenu.getItems().add(visitClanPageAction);

    clanMenu.show();
  }
}
