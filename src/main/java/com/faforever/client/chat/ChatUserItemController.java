package com.faforever.client.chat;

import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.contextmenu.AddEditPlayerNoteMenuItem;
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
import com.faforever.client.fx.contextmenu.RemovePlayerNoteMenuItem;
import com.faforever.client.fx.contextmenu.ReportPlayerMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageClanLeaderMenuItem;
import com.faforever.client.fx.contextmenu.SendPrivateMessageMenuItem;
import com.faforever.client.fx.contextmenu.ShowPlayerInfoMenuItem;
import com.faforever.client.fx.contextmenu.ViewReplaysMenuItem;
import com.faforever.client.fx.contextmenu.WatchGameMenuItem;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ChatUserItemController implements Controller<Node> {

  private final PreferencesService preferencesService;
  private final I18n i18n;
  private final UiService uiService;
  private final MapService mapService;
  private final MapGeneratorService mapGeneratorService;
  private final EventBus eventBus;
  private final ContextMenuBuilder contextMenuBuilder;

  private final ObjectProperty<ChatChannelUser> chatUser = new SimpleObjectProperty<>();

  public Pane root;
  public ImageView mapImageView;
  public ImageView countryImageView;
  public ImageView gameStatusImageView;
  public ImageView avatarImageView;
  public Label usernameLabel;
  public Label mapNameLabel;
  public Label noteIcon;
  public VBox userContainer;

  private Tooltip avatarTooltip;
  private Tooltip gameInfoTooltip;
  private Tooltip noteTooltip;
  private GameTooltipController gameInfoController;

  public void initialize() {
    initializeTooltips();
    bindProperties();
  }

  private void initializeTooltips() {
    initializeAvatarTooltip();
    initializePlayerNoteTooltip();
  }

  private void initializePlayerNoteTooltip() {
    noteTooltip = new Tooltip();
    noteTooltip.setShowDelay(Duration.ZERO);
    noteTooltip.setShowDuration(Duration.seconds(30));
    noteTooltip.textProperty().isEmpty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        Tooltip.uninstall(userContainer, noteTooltip);
      } else {
        Tooltip.install(userContainer, noteTooltip);
      }
    });
  }

  private void initializeAvatarTooltip() {
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
    PlayerBean player = Optional.ofNullable(chatUser.get()).flatMap(ChatChannelUser::getPlayer).orElse(null);
    if (player == null || gameInfoTooltip != null || gameInfoController != null) {
      return;
    }
    gameInfoController = prepareGameInfoController(player.getGame());
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
    ChatChannelUser chatChannelUser = chatUser.get();
    PlayerBean player = chatChannelUser == null ? null : chatChannelUser.getPlayer().orElse(null);
    String username = chatChannelUser == null ? null : chatChannelUser.getUsername();
    contextMenuBuilder.newBuilder()
        .addItem(ShowPlayerInfoMenuItem.class, player)
        .addItem(SendPrivateMessageMenuItem.class, username)
        .addItem(CopyUsernameMenuItem.class, username)
        .addItem(ChangeUsernameColorMenuItem.class, chatChannelUser)
        .addSeparator()
        .addItem(SendPrivateMessageClanLeaderMenuItem.class, player)
        .addItem(OpenClanUrlMenuItem.class, player)
        .addSeparator()
        .addItem(InvitePlayerMenuItem.class, player)
        .addItem(AddFriendMenuItem.class, player)
        .addItem(RemoveFriendMenuItem.class, player)
        .addItem(AddFoeMenuItem.class, player)
        .addItem(RemoveFoeMenuItem.class, player)
        .addSeparator()
        .addItem(AddEditPlayerNoteMenuItem.class, player)
        .addItem(RemovePlayerNoteMenuItem.class, player)
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
    ChatChannelUser chatChannelUser = chatUser.get();
    if (chatChannelUser != null && mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
      eventBus.post(new InitiatePrivateChatEvent(chatChannelUser.getUsername()));
    }
  }

  public Pane getRoot() {
    return root;
  }

  public ChatChannelUser getChatUser() {
    return chatUser.get();
  }

  public void setChatUser(ChatChannelUser chatUser) {
    this.chatUser.set(chatUser);
  }

  private void bindProperties() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    JavaFxUtil.bindManagedToVisible(mapNameLabel, mapImageView, gameStatusImageView, noteIcon);
    avatarImageView.visibleProperty().bind(avatarImageView.imageProperty().isNotNull());
    countryImageView.visibleProperty().bind(countryImageView.imageProperty().isNotNull());
    gameStatusImageView.visibleProperty().bind(gameStatusImageView.imageProperty().isNotNull());
    noteIcon.visibleProperty().bind(noteTooltip.textProperty().isNotEmpty());
    mapNameLabel.visibleProperty().bind(mapNameLabel.textProperty().isNotEmpty().and(chatPrefs.showMapNameProperty()));
    mapImageView.visibleProperty().bind(chatPrefs.showMapPreviewProperty());
    mapNameLabel.textProperty()
        .bind(chatUser.flatMap(ChatChannelUser::playerProperty)
            .flatMap(PlayerBean::gameProperty)
            .flatMap(GameBean::mapFolderNameProperty)
            .map(this::getMapName)
            .map(mapName -> i18n.get("game.onMapFormat", mapName)));
    mapImageView.imageProperty()
        .bind(chatUser.flatMap(ChatChannelUser::mapImageProperty)
            .flatMap(this::getImageWithErrorProperty));
    gameStatusImageView.imageProperty().bind(chatUser.flatMap(ChatChannelUser::gameStatusImageProperty));
    noteTooltip.textProperty()
        .bind(chatUser.flatMap(ChatChannelUser::playerProperty).flatMap(PlayerBean::noteProperty));
    avatarTooltip.textProperty()
        .bind(chatUser.flatMap(ChatChannelUser::playerProperty)
            .flatMap(PlayerBean::avatarProperty)
            .flatMap(AvatarBean::descriptionProperty));
    avatarImageView.imageProperty().bind(chatUser.flatMap(ChatChannelUser::avatarProperty));
    countryImageView.imageProperty().bind(chatUser.flatMap(ChatChannelUser::countryFlagProperty));
    usernameLabel.styleProperty()
        .bind(chatUser.flatMap(ChatChannelUser::colorProperty)
            .map(JavaFxUtil::toRgbCode)
            .map(rgb -> String.format("-fx-text-fill: %s", rgb)));
    ObservableValue<String> clanTagProperty = chatUser.flatMap(ChatChannelUser::clanTagProperty);
    ObservableValue<String> usernameProperty = chatUser.flatMap(ChatChannelUser::usernameProperty);
    usernameLabel.textProperty()
        .bind(Bindings.createStringBinding(() -> {
          String clanTag = clanTagProperty.getValue();
          String username = usernameProperty.getValue();
          return StringUtils.isEmpty(clanTag) ? username : clanTag + " " + username;
        }, clanTagProperty, usernameProperty));
  }

  private ObservableValue<Image> getImageWithErrorProperty(Image image) {
    return image.errorProperty()
        .map(error -> error ? uiService.getThemeImage(UiService.NO_IMAGE_AVAILABLE) : image);
  }

  private String getMapName(String mapFolderName) {
    if (mapGeneratorService.isGeneratedMap(mapFolderName)) {
      return "Neroxis Generated Map";
    } else {
      return mapService.getMapLocallyFromName(mapFolderName)
          .map(mapVersion -> mapVersion.getMap().getDisplayName())
          .orElseGet(() -> mapService.convertMapFolderNameToHumanNameIfPossible(mapFolderName));
    }
  }
}
