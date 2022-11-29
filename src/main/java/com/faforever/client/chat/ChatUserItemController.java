package com.faforever.client.chat;

import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.AddFoeMenuItem;
import com.faforever.client.fx.contextmenu.AddFriendMenuItem;
import com.faforever.client.fx.contextmenu.BroadcastMessageMenuItem;
import com.faforever.client.fx.contextmenu.ChangeUsernameColorMenuItem;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.fx.contextmenu.CopyUsernameMenuItem;
import com.faforever.client.fx.contextmenu.InvitePlayerMenuItem;
import com.faforever.client.fx.contextmenu.JoinGameMenuItem;
import com.faforever.client.fx.contextmenu.KickGameMenuItem;
import com.faforever.client.fx.contextmenu.KickLobbyMenuItem;
import com.faforever.client.fx.contextmenu.OpenClanUrlMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFoeMenuItem;
import com.faforever.client.fx.contextmenu.RemoveFriendMenuItem;
import com.faforever.client.fx.contextmenu.ReportPlayerMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageClanLeaderMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageMenuItem;
import com.faforever.client.fx.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.fx.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.fx.contextmenu.WatchGameMenuItem;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Assert;
import com.google.common.eventbus.EventBus;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ChatUserItemController implements Controller<Node> {

  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final UiService uiService;
  private final ImageViewHelper imageViewHelper;
  private final MapService mapService;
  private final MapGeneratorService mapGeneratorService;
  private final EventBus eventBus;
  private final ContextMenuBuilder contextMenuBuilder;

  private final InvalidationListener chatUserPropertyInvalidationListener = observable -> updateChatUserDisplay();
  private final InvalidationListener chatUserGamePropertyInvalidationListener = observable -> updateChatUserGame();
  private final InvalidationListener showMapNameListener = observable -> updateMapNameLabelVisible();
  private final InvalidationListener showMapPreviewListener = observable -> updateMapPreviewImageViewVisible();

  public Pane root;
  public ImageView mapImageView;
  public ImageView countryImageView;
  public ImageView gameStatusImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public Label mapNameLabel;

  private Tooltip avatarTooltip;
  private Tooltip gameInfoTooltip;
  private GameTooltipController gameInfoController;
  private ChatChannelUser chatUser;
  private ChatPrefs chatPrefs;

  public void initialize() {
    chatPrefs = preferencesService.getPreferences().getChat();
    imageViewHelper.setDefaultPlaceholderImage(mapImageView, true);

    JavaFxUtil.bindManagedToVisible(mapNameLabel, mapImageView, gameStatusImageView);
    JavaFxUtil.bind(avatarImageView.visibleProperty(), avatarImageView.imageProperty().isNotNull());
    JavaFxUtil.bind(countryImageView.visibleProperty(), countryImageView.imageProperty().isNotNull());
    JavaFxUtil.bind(gameStatusImageView.visibleProperty(), gameStatusImageView.imageProperty().isNotNull());
    initializeTooltips();

    WeakInvalidationListener showMapNameWeakListener = new WeakInvalidationListener(showMapNameListener);
    JavaFxUtil.addListener(mapNameLabel.textProperty(), showMapNameWeakListener);
    JavaFxUtil.addAndTriggerListener(chatPrefs.showMapNameProperty(), showMapNameWeakListener);
    JavaFxUtil.addAndTriggerListener(chatPrefs.showMapPreviewProperty(), new WeakInvalidationListener(showMapPreviewListener));
  }

  private void updateMapNameLabelVisible() {
    boolean visible = !StringUtils.isBlank(mapNameLabel.getText()) && chatPrefs.isShowMapName();
    Platform.runLater(() -> mapNameLabel.setVisible(visible));
  }

  private void updateMapPreviewImageViewVisible() {
    Platform.runLater(() -> mapImageView.setVisible(chatPrefs.isShowMapPreview()));
  }

  private void initializeTooltips() {
    avatarTooltip = new Tooltip();
    avatarTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    Tooltip.install(avatarImageView, avatarTooltip);
  }

  public void onMapImageViewMouseExited() {
    if (gameInfoTooltip == null || gameInfoController == null) {
      return;
    }
    Tooltip.uninstall(mapImageView, gameInfoTooltip);
    gameInfoTooltip = null;
    gameInfoController = null;
  }

  public void onMapImageViewMouseMoved() {
    if (chatUser == null || chatUser.getPlayer().isEmpty() || gameInfoTooltip != null || gameInfoController != null) {
      return;
    }
    gameInfoController = prepareGameInfoController(chatUser.getPlayer().get().getGame());
    gameInfoTooltip = prepareGameInfoTooltip(gameInfoController);
    gameInfoController.displayGame();
    Tooltip.install(mapImageView, gameInfoTooltip);
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

  public void onContextMenuRequested(ContextMenuEvent event) {
    PlayerBean player = chatUser.getPlayer().orElse(null);
    contextMenuBuilder.newBuilder()
        .addItem(ShowPlayerInfoMenuItem.class, player)
        .addItem(SendPrivateMessageMenuItem.class, chatUser.getUsername())
        .addItem(CopyUsernameMenuItem.class, chatUser.getUsername())
        .addItem(ChangeUsernameColorMenuItem.class, chatUser)
        .addSeparator()
        .addItem(SendPrivateMessageClanLeaderMenuItem.class, chatUser.getClan().orElse(null))
        .addItem(OpenClanUrlMenuItem.class, chatUser.getClan().orElse(null))
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
        .show(root.getScene().getWindow(), event.getScreenX(), event.getScreenY());
  }

  public void onItemClicked(MouseEvent mouseEvent) {
    if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(chatUser.getUsername()));
    }
  }

  public Pane getRoot() {
    return root;
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
    String clanString = chatUser.getClanTag().map(s -> s + " ").orElse("");
    JavaFxUtil.runLater(() -> {
      usernameLabel.setText(clanString + chatUser.getUsername());
      usernameLabel.setStyle(styleString);
      avatarImageView.setImage(chatUser.getAvatar().orElse(null));
      countryImageView.setImage(chatUser.getCountryFlag().orElse(null));
      avatarTooltip.setText(avatarString);
    });
  }

  private void updateChatUserGame() {
    JavaFxUtil.runLater(() -> {
      gameStatusImageView.setImage(chatUser.getGameStatusImage().orElse(null));
      mapImageView.setImage(chatUser.getMapImage().orElse(null));
      chatUser.getPlayer().filter(player -> player.getStatus() != PlayerStatus.IDLE)
          .map(player -> player.getGame().getMapFolderName()).ifPresentOrElse(this::updateMapNameLabel,
              () -> updateMapNameLabel(null));
    });
  }

  private void updateMapNameLabel(String mapFolderName) {
    if (mapFolderName == null) {
      mapNameLabel.setText("");
    } else {
      String text;
      if (mapGeneratorService.isGeneratedMap(mapFolderName)) {
        text = "Neroxis Generated Map";
      } else {
        text = mapService.getMapLocallyFromName(mapFolderName).map(mapVersion -> mapVersion.getMap().getDisplayName())
            .orElseGet(() -> mapService.convertMapFolderNameToHumanNameIfPossible(mapFolderName));
      }
      mapNameLabel.setText(i18n.get("game.onMapFormat", text));
    }
  }
}
