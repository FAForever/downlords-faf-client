package com.faforever.client.chat;

import com.faforever.client.avatar.AvatarService;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
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
import com.faforever.client.game.PlayerStatus;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
@RequiredArgsConstructor
public class ChatUserItemController extends NodeController<Node> {

  private final I18n i18n;
  private final UiService uiService;
  private final ThemeService themeService;
  private final MapService mapService;
  private final ChatService chatService;
  private final MapGeneratorService mapGeneratorService;
  private final CountryFlagService countryFlagService;
  private final AvatarService avatarService;
  private final ContextMenuBuilder contextMenuBuilder;
  private final ChatPrefs chatPrefs;
  private final ImageViewHelper imageViewHelper;

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
  private Tooltip statusTooltip;
  private Tooltip countryTooltip;
  private Tooltip noteTooltip;

  @Override
  protected void onInitialize() {
    initializeTooltips();
    bindProperties();
  }

  private void initializeTooltips() {
    initializeAvatarTooltip();
    initializePlayerNoteTooltip();
    initializeCountryTooltip();
    initializeStatusTooltip();
  }

  private void initializePlayerNoteTooltip() {
    noteTooltip = new Tooltip();
    noteTooltip.setShowDelay(Duration.ZERO);
    noteTooltip.setShowDuration(Duration.seconds(30));
    noteTooltip.textProperty().isEmpty().when(showing).subscribe((oldValue, newValue) -> {
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

  private void initializeCountryTooltip() {
    countryTooltip = new Tooltip();
    countryTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    Tooltip.install(countryImageView, countryTooltip);
  }

  private void initializeStatusTooltip() {
    statusTooltip = new Tooltip();
    statusTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);
    Tooltip.install(gameStatusImageView, statusTooltip);
  }

  public void installGameTooltip(GameTooltipController gameInfoController, Tooltip tooltip) {
    mapImageView.setOnMouseEntered(event -> gameInfoController.gameProperty()
        .bind(chatUser.flatMap(ChatChannelUser::playerProperty).flatMap(PlayerBean::gameProperty).when(showing)));
    Tooltip.install(mapImageView, tooltip);
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
      chatService.onInitiatePrivateChat(chatChannelUser.getUsername());
    }
  }

  @Override
  public Pane getRoot() {
    return root;
  }

  public ChatChannelUser getChatUser() {
    return chatUser.get();
  }

  public void setChatUser(ChatChannelUser chatUser) {
    this.chatUser.set(chatUser);
  }

  public ObjectProperty<ChatChannelUser> chatUserProperty() {
    return chatUser;
  }

  private void bindProperties() {
    ObservableValue<PlayerBean> playerProperty = chatUser.flatMap(ChatChannelUser::playerProperty);
    ObservableValue<GameBean> gameProperty = playerProperty.flatMap(PlayerBean::gameProperty);
    BooleanExpression gameNotClosedObservable = BooleanExpression.booleanExpression(gameProperty.flatMap(GameBean::statusProperty)
        .map(status -> status != GameStatus.CLOSED));

    JavaFxUtil.bindManagedToVisible(mapNameLabel, mapImageView, noteIcon);

    noteIcon.visibleProperty().bind(noteTooltip.textProperty().isNotEmpty());
    mapNameLabel.visibleProperty()
        .bind(chatPrefs.showMapNameProperty()
            .and(mapNameLabel.textProperty().isNotEmpty())
            .and(gameNotClosedObservable)
            .when(showing));

    mapImageView.visibleProperty().bind(chatPrefs.showMapPreviewProperty().and(gameNotClosedObservable).when(showing));

    mapNameLabel.textProperty().bind(gameProperty.flatMap(GameBean::mapFolderNameProperty).map(mapFolderName -> {
      if (mapGeneratorService.isGeneratedMap(mapFolderName)) {
        return "Neroxis Generated Map";
      } else {
        return mapService.getMapLocallyFromName(mapFolderName)
            .map(mapVersion -> mapVersion.getMap().getDisplayName())
            .orElseGet(() -> mapService.convertMapFolderNameToHumanNameIfPossible(mapFolderName));
      }
    }).map(mapName -> i18n.get("game.onMapFormat", mapName)).when(showing));

    mapImageView.imageProperty()
        .bind(gameProperty.flatMap(GameBean::mapFolderNameProperty)
            .flatMap(mapFolderName -> Bindings.createObjectBinding(() -> mapService.loadPreview(mapFolderName, PreviewSize.SMALL), mapService.isInstalledBinding(mapFolderName)))
            .flatMap(imageViewHelper::createPlaceholderImageOnErrorObservable)
            .when(showing));

    ObservableValue<PlayerStatus> statusProperty = playerProperty.flatMap(PlayerBean::statusProperty);
    gameStatusImageView.imageProperty().bind(statusProperty.map(status -> switch (status) {
      case HOSTING -> themeService.getThemeImage(ThemeService.CHAT_LIST_STATUS_HOSTING);
      case LOBBYING -> themeService.getThemeImage(ThemeService.CHAT_LIST_STATUS_LOBBYING);
      case PLAYING -> themeService.getThemeImage(ThemeService.CHAT_LIST_STATUS_PLAYING);
      default -> null;
    }).when(showing));

    statusTooltip.textProperty().bind(statusProperty.map(PlayerStatus::getI18nKey).map(i18n::get).when(showing));

    noteTooltip.textProperty().bind(playerProperty.flatMap(PlayerBean::noteProperty).when(showing));

    ObservableValue<AvatarBean> avatarProperty = playerProperty.flatMap(PlayerBean::avatarProperty);
    avatarTooltip.textProperty().bind(avatarProperty.flatMap(AvatarBean::descriptionProperty).when(showing));
    avatarImageView.imageProperty().bind(avatarProperty.map(avatarService::loadAvatar).when(showing));

    ObservableValue<String> countryProperty = playerProperty.flatMap(PlayerBean::countryProperty);
    countryTooltip.textProperty().bind(countryProperty.map(i18n::getCountryNameLocalized).when(showing));

    countryImageView.imageProperty()
        .bind(countryProperty.map(countryFlagService::loadCountryFlag)
            .map(possibleFlag -> possibleFlag.orElse(null))
            .when(showing));

    usernameLabel.styleProperty()
        .bind(chatUser.flatMap(ChatChannelUser::colorProperty)
            .map(JavaFxUtil::toRgbCode)
            .map(rgb -> String.format("-fx-text-fill: %s", rgb))
            .when(showing));

    ObservableValue<String> clanTagProperty = chatUser.flatMap(ChatChannelUser::playerProperty)
        .flatMap(PlayerBean::clanProperty)
        .map(clanTag -> clanTag.isBlank() ? null : String.format("[%s]", clanTag));
    ObservableValue<String> usernameProperty = chatUser.map(ChatChannelUser::getUsername);
    usernameLabel.textProperty().bind(Bindings.createStringBinding(() -> {
      String clanTag = clanTagProperty.getValue();
      String username = usernameProperty.getValue();
      return StringUtils.isEmpty(clanTag) ? username : clanTag + " " + username;
    }, clanTagProperty, usernameProperty).when(showing));
  }

}
