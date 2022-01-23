package com.faforever.client.chat;

import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.ClanBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.contextmenu.AddFoeMenuItem;
import com.faforever.client.test.contextmenu.AddFriendMenuItem;
import com.faforever.client.test.contextmenu.BroadcastMessageMenuItem;
import com.faforever.client.test.contextmenu.ContextMenuBuilder;
import com.faforever.client.test.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.test.contextmenu.InvitePlayerMenuItem;
import com.faforever.client.test.contextmenu.JoinGameMenuItem;
import com.faforever.client.test.contextmenu.KickGameMenuItem;
import com.faforever.client.test.contextmenu.KickLobbyMenuItem;
import com.faforever.client.test.contextmenu.RemoveFoeMenuItem;
import com.faforever.client.test.contextmenu.RemoveFriendMenuItem;
import com.faforever.client.test.contextmenu.ReportPlayerMenuItem;
import com.faforever.client.test.contextmenu.SendPrivateMessageMenuItem;
import com.faforever.client.test.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.test.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.test.contextmenu.WatchGameMenuItem;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
public class ChatUserItemController implements Controller<Node> {

  private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");

  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final UiService uiService;
  private final EventBus eventBus;
  private final PlayerService playerService;
  private final PlatformService platformService;
  private final ApplicationContext context;

  private final InvalidationListener formatChangeListener;
  private final InvalidationListener chatUserPropertyInvalidationListener;
  private final InvalidationListener chatUserGamePropertyInvalidationListener;

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
  private WeakReference<ContextMenu> contextMenuWeakReference = null;

  public ChatUserItemController(PreferencesService preferencesService,
                                I18n i18n, UiService uiService, EventBus eventBus, PlayerService playerService,
                                PlatformService platformService, ApplicationContext context) {
    this.platformService = platformService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.eventBus = eventBus;
    this.context = context;

    formatChangeListener = observable -> updateFormat();
    chatUserPropertyInvalidationListener = observable -> updateChatUserDisplay();
    chatUserGamePropertyInvalidationListener = observable -> updateChatUserGame();
  }

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
    if (contextMenuWeakReference != null) {
      ContextMenu contextMenu = contextMenuWeakReference.get();
      if (contextMenu != null) {
        contextMenu.show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
        return;
      }
    }

    PlayerBean player = chatUser.getPlayer().orElseThrow();
    ContextMenu contextMenu = ContextMenuBuilder.newBuilder(context)
        .addItem(new ShowPlayerInfoMenuItem(), player)
        .addItem(new SendPrivateMessageMenuItem(), player)
        .addItem(new CopyUsernameMenuItem(), player.getUsername())
        .addCustomItem(uiService.loadFxml("theme/color_picker_menu_item.fxml"), chatUser)
        .addSeparator()
        .addItem(new InvitePlayerMenuItem(), player)
        .addItem(new AddFriendMenuItem(), player)
        .addItem(new RemoveFriendMenuItem(), player)
        .addItem(new AddFoeMenuItem(), player)
        .addItem(new RemoveFoeMenuItem(), player)
        .addItem(new ReportPlayerMenuItem(), player)
        .addSeparator()
        .addItem(new JoinGameMenuItem(), player)
        .addItem(new WatchGameMenuItem(), player)
        .addItem(new ViewReplaysMenuItem(), player)
        .addSeparator()
        .addItem(new KickGameMenuItem(), player)
        .addItem(new KickLobbyMenuItem(), player)
        .addItem(new BroadcastMessageMenuItem())
        .addCustomItem(uiService.loadFxml("theme/avatar_picker_menu_item.fxml"), player)
        .build();
    contextMenu.show(chatUserItemRoot.getScene().getWindow(), event.getScreenX(), event.getScreenY());
    contextMenuWeakReference = new WeakReference<>(contextMenu);
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
