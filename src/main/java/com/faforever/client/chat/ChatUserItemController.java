package com.faforever.client.chat;

import com.faforever.client.clan.Clan;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.Game;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.ref.WeakReference;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO null safety for "player"
public class ChatUserItemController implements Controller<Node> {

  private static final PseudoClass COMPACT = PseudoClass.getPseudoClass("compact");

  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final UiService uiService;
  private final EventBus eventBus;
  private final PlayerService playerService;
  private final PlatformService platformService;
  private final ChatPrefs chatPrefs;

  private final InvalidationListener formatChangeListener;
  private final WeakInvalidationListener weakFormatInvalidationListener;

  public ImageView playerMapImage;
  public ImageView playerStatusIndicator;
  public Pane chatUserItemRoot;
  public ImageView countryImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public MenuButton clanMenu;
  protected Tooltip statusGameTooltip;
  protected Tooltip gameInfoTooltip;
  @VisibleForTesting
  protected Tooltip countryTooltip;
  @VisibleForTesting
  protected Tooltip avatarTooltip;
  private GameTooltipController gameInfoController;
  private ChatChannelUser chatUser;
  private WeakReference<ChatUserContextMenuController> contextMenuController = null;

  public ChatUserItemController(PreferencesService preferencesService,
                                I18n i18n, UiService uiService, EventBus eventBus, PlayerService playerService,
                                PlatformService platformService) {
    this.platformService = platformService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.i18n = i18n;
    this.uiService = uiService;
    this.eventBus = eventBus;
    this.chatPrefs = preferencesService.getPreferences().getChat();

    formatChangeListener = observable -> updateFormat();
    weakFormatInvalidationListener = new WeakInvalidationListener(formatChangeListener);

    JavaFxUtil.addListener(chatPrefs.chatFormatProperty(), weakFormatInvalidationListener);
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

    weakFormatInvalidationListener.invalidated(chatPrefs.chatFormatProperty());

    JavaFxUtil.bindManagedToVisible(countryImageView, clanMenu, playerStatusIndicator, playerMapImage);

    JavaFxUtil.bind(avatarImageView.visibleProperty(), Bindings.isNotNull(avatarImageView.imageProperty()));
    JavaFxUtil.bind(countryImageView.visibleProperty(), Bindings.isNotNull(countryImageView.imageProperty()));
    JavaFxUtil.bind(clanMenu.visibleProperty(), Bindings.isNotEmpty(clanMenu.textProperty()));
    JavaFxUtil.bind(playerStatusIndicator.visibleProperty(), Bindings.isNotNull(playerStatusIndicator.imageProperty()));
    JavaFxUtil.bind(playerMapImage.visibleProperty(), Bindings.isNotNull(playerMapImage.imageProperty()));

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

  private GameTooltipController prepareGameInfoController(Game game) {
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

    JavaFxUtil.unbind(usernameLabel.textProperty());
    JavaFxUtil.unbind(usernameLabel.styleProperty());
    JavaFxUtil.unbind(avatarImageView.imageProperty());
    JavaFxUtil.unbind(clanMenu.textProperty());
    JavaFxUtil.unbind(clanMenu.styleProperty());
    JavaFxUtil.unbind(countryImageView.imageProperty());
    JavaFxUtil.unbind(playerMapImage.imageProperty());
    JavaFxUtil.unbind(playerStatusIndicator.imageProperty());
    JavaFxUtil.unbind(avatarTooltip.textProperty());
    JavaFxUtil.unbind(countryTooltip.textProperty());
    JavaFxUtil.unbind(statusGameTooltip.textProperty());

    if (this.chatUser != null) {
      this.chatUser.setDisplayed(false);
    }

    this.chatUser = chatUser;

    if (this.chatUser != null) {
      this.chatUser.setDisplayed(true);
      JavaFxUtil.bind(usernameLabel.textProperty(), this.chatUser.usernameProperty());
      JavaFxUtil.bind(usernameLabel.styleProperty(), Bindings.createStringBinding(() ->
              chatUser.getColor().map(color -> String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color))).orElse(""),
          chatUser.colorProperty()));
      JavaFxUtil.bind(avatarImageView.imageProperty(), this.chatUser.avatarProperty());
      JavaFxUtil.bind(clanMenu.textProperty(), this.chatUser.clanTagProperty());
      JavaFxUtil.bind(countryImageView.imageProperty(), this.chatUser.countryFlagProperty());
      JavaFxUtil.bind(countryTooltip.textProperty(), this.chatUser.countryNameProperty());
      JavaFxUtil.bind(playerMapImage.imageProperty(), this.chatUser.mapImageProperty());
      JavaFxUtil.bind(playerStatusIndicator.imageProperty(), this.chatUser.gameStatusImageProperty());
      JavaFxUtil.bind(statusGameTooltip.textProperty(), this.chatUser.statusTooltipTextProperty());
      if (this.chatUser.getPlayer().isPresent()) {
        JavaFxUtil.bind(avatarTooltip.textProperty(), this.chatUser.getPlayer().get().avatarTooltipProperty());
      }
    }
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

    Clan clan = chatUser.getClan().get();

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

    clanMenu.show();
  }
}
