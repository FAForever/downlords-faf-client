package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.SELF;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabController extends AbstractChatTabController {

  @VisibleForTesting
  static final String CSS_CLASS_MODERATOR = "moderator";
  private static final String USER_CSS_CLASS_FORMAT = "user-%s";
  private static final long IDLE_TIME_UPDATE_DELAY = Duration.ofMinutes(1).toMillis();
  /**
   * Keeps track of which ChatUserControl in which pane belongs to which user.
   */
  private final Map<String, Map<TreeItem<ChatUserTreeItem>, ChatUserItemController>> userToChatUserControls;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final TaskScheduler taskScheduler;
  public ToggleButton advancedUserFilter;
  public HBox searchFieldContainer;
  public Button closeSearchFieldButton;
  public TextField searchField;
  public VBox channelTabScrollPaneVBox;
  public Tab channelTabRoot;
  public WebView messagesWebView;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<Boolean>>> moderatorForChannelListeners;
  public TextField userSearchTextField;
  public TextField messageTextField;
  public TreeTableView<ChatUserTreeItem> chatUserTableView;
  private Channel channel;
  private Popup filterUserPopup;
  public TreeTableColumn<ChatUserTreeItem, Node> chatUserColumn;
  private ChangeListener<ChatColorMode> chatColorModeChangeListener;
  private UserFilterController userFilterController;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<Player>>> playerListeners;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<Boolean>>> hideFoeMessagesListeners;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<SocialStatus>>> socialStatusMessagesListeners;
  private MapChangeListener<String, ChatChannelUser> usersChangeListener;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<Color>>> colorPropertyListeners;
  private ScheduledFuture<?> presenceStatusUpdateFuture;
  private TreeItem<ChatUserTreeItem> moderatorsTreeItem;
  private TreeItem<ChatUserTreeItem> friendsTreeItem;
  private TreeItem<ChatUserTreeItem> othersTreeItem;
  private TreeItem<ChatUserTreeItem> chatOnlyTreeItem;
  private TreeItem<ChatUserTreeItem> foesTreeItem;

  // TODO cut dependencies
  @Inject
  public ChannelTabController(UserService userService, ChatService chatService,
                              PreferencesService preferencesService,
                              PlayerService playerService, AudioService audioService, TimeService timeService,
                              I18n i18n, ImageUploadService imageUploadService,
                              NotificationService notificationService, ReportingService reportingService,
                              UiService uiService, AutoCompletionHelper autoCompletionHelper, EventBus eventBus,
                              WebViewConfigurer webViewConfigurer,
                              @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ThreadPoolExecutor threadPoolExecutor,
                              TaskScheduler taskScheduler, CountryFlagService countryFlagService) {

    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService,
        timeService, i18n, imageUploadService, notificationService, reportingService, uiService, autoCompletionHelper,
        eventBus, countryFlagService);

    this.threadPoolExecutor = threadPoolExecutor;
    this.taskScheduler = taskScheduler;

    playerListeners = new HashMap<>();
    hideFoeMessagesListeners = new HashMap<>();
    socialStatusMessagesListeners = new HashMap<>();
    moderatorForChannelListeners = new HashMap<>();
    colorPropertyListeners = new HashMap<>();
    userToChatUserControls = FXCollections.observableHashMap();
  }

  @Override
  protected void onClosed(Event event) {
    super.onClosed(event);
    presenceStatusUpdateFuture.cancel(true);
  }

  Map<String, Map<TreeItem<ChatUserTreeItem>, ChatUserItemController>> getUserToChatUserControls() {
    return userToChatUserControls;
  }

  public void setChannel(Channel channel) {
    if (this.channel != null) {
      throw new IllegalStateException("channel has already been set");
    }

    this.channel = channel;
    String channelName = channel.getName();
    setReceiver(channelName);
    channelTabRoot.setId(channelName);
    channelTabRoot.setText(channelName);

    usersChangeListener = change -> {
      if (change.wasAdded()) {
        onUserJoinedChannel(change.getValueAdded());
      } else if (change.wasRemoved()) {
        onUserLeft(change.getValueRemoved().getUsername());
      }
      updateUserCount(change.getMap().size());
    };
    updateUserCount(channel.getUsers().size());

    chatService.addUsersListener(channelName, usersChangeListener);

    // Maybe there already were some users; fetch them
    threadPoolExecutor.execute(() -> channel.getUsers().forEach(ChannelTabController.this::onUserJoinedChannel));

    channelTabRoot.setOnCloseRequest(event -> {
      chatService.leaveChannel(channel.getName());
      chatService.removeUsersListener(channelName, usersChangeListener);
    });

    searchFieldContainer.visibleProperty().bind(searchField.visibleProperty());
    closeSearchFieldButton.visibleProperty().bind(searchField.visibleProperty());
    addSearchFieldListener();
  }

  private void updateUserCount(int count) {
    Platform.runLater(() -> userSearchTextField.setPromptText(i18n.get("chat.userCount", count)));
  }

  @Override
  public void initialize() {
    super.initialize();
    presenceStatusUpdateFuture = taskScheduler.scheduleWithFixedDelay(this::updatePresenceStatusIndicators, Date.from(Instant.now()), IDLE_TIME_UPDATE_DELAY);

    userSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> filterChatUserControlsBySearchString());

    chatColorModeChangeListener = (observable, oldValue, newValue) -> {
      if (newValue != DEFAULT) {
        setAllMessageColors();
      } else {
        removeAllMessageColors();
      }
    };

    channelTabScrollPaneVBox.setMinWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    channelTabScrollPaneVBox.setPrefWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    addChatColorListener();
    addUserFilterPopup();

    chatUserTableView.setRoot(new TreeItem<>());
    chatUserColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getGraphic()));
    chatUserColumn.setCellFactory(param -> {
      return new TreeTableCell<ChatUserTreeItem, Node>() {
        @Override
        protected void updateItem(Node item, boolean empty) {
          super.updateItem(item, empty);

          if (empty || item == null) {
            setText(null);
            setGraphic(null);
          } else {
            setText(item.toString());
            setGraphic(null);
          }
        }
      };
    });
    addCategories();
  }

  @SuppressWarnings("unchecked")
  private void addCategories() {
    chatUserTableView.getRoot().getChildren().addAll(
        moderatorsTreeItem = createHeaderItem(i18n.get("chat.category.moderators")),
        friendsTreeItem = createHeaderItem(i18n.get("chat.category.friends")),
        othersTreeItem = createHeaderItem(i18n.get("chat.category.others")),
        chatOnlyTreeItem = createHeaderItem(i18n.get("chat.category.chatOnly")),
        foesTreeItem = createHeaderItem(i18n.get("chat.category.foes"))
    );
  }

  @NotNull
  private TreeItem<ChatUserTreeItem> createHeaderItem(String title) {
    ChatUserHeaderController controller = uiService.loadFxml("theme/chat/chat_user_header.fxml");
    controller.setTitle(title);
    return new TreeItem<>(null, controller.getRoot());
  }

  private void updatePresenceStatusIndicators() {
    Platform.runLater(() -> {
      synchronized (userToChatUserControls) {
        userToChatUserControls.values().stream()
            .flatMap(paneChatUserItemControllerMap -> paneChatUserItemControllerMap.values().stream())
            .forEach(ChatUserItemController::updatePresenceStatusIndicator);
      }
    });
  }

  /**
   * Hides all chat user controls whose username does not contain the string entered in the {@link
   * #userSearchTextField}.
   */
  private void filterChatUserControlsBySearchString() {
    synchronized (userToChatUserControls) {
      for (Map<TreeItem<ChatUserTreeItem>, ChatUserItemController> chatUserControlMap : userToChatUserControls.values()) {
        for (Map.Entry<TreeItem<ChatUserTreeItem>, ChatUserItemController> chatUserControlEntry : chatUserControlMap.entrySet()) {
          ChatUserItemController chatUserItemController = chatUserControlEntry.getValue();
          chatUserItemController.setVisible(isUsernameMatch(chatUserItemController));
        }
      }
    }
  }

  private void setAllMessageColors() {
    Map<String, String> userToColor = new HashMap<>();
    channel.getUsers().stream().filter(chatUser -> chatUser.getColor() != null).forEach(chatUser
        -> userToColor.put(chatUser.getUsername(), JavaFxUtil.toRgbCode(chatUser.getColor())));
    getJsObject().call("setAllMessageColors", new Gson().toJson(userToColor));
  }

  private void removeAllMessageColors() {
    getJsObject().call("removeAllMessageColors");
  }

  @VisibleForTesting
  boolean isUsernameMatch(ChatUserItemController chatUserItemController) {
    String lowerCaseSearchString = chatUserItemController.getChatUser().getUsername().toLowerCase();
    return lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase());
  }

  /**
   * Inserts the given ChatUserControl into the given Pane such that it is correctly sorted alphabetically.
   */
  private void addChatUserItemSorted(TreeItem<ChatUserTreeItem> treeItem, ChatUserItemController chatUserItemController) {
    ObservableList<TreeItem<ChatUserTreeItem>> children = treeItem.getChildren();

    TreeItem<ChatUserTreeItem> chatUserItemRoot = new TreeItem<>(chatUserItemController, chatUserItemController.getRoot());
    ChatChannelUser chatUser = chatUserItemController.getChatUser();

    Optional<Player> playerOptional = chatUser.getPlayer();

    if (playerOptional.isPresent() && playerOptional.get().getSocialStatus() == SELF) {
      children.add(0, chatUserItemRoot);
      return;
    }

    String thisUsername = chatUser.getUsername();
    for (TreeItem<ChatUserTreeItem> userTreeItem : children) {
      String otherUsername = ((ChatUserItemController) userTreeItem.getValue()).getChatUser().getUsername();

      if (otherUsername.equalsIgnoreCase(userService.getUsername())) {
        continue;
      }

      if (thisUsername.compareToIgnoreCase(otherUsername) < 0) {
        children.add(children.indexOf(userTreeItem), chatUserItemRoot);
        return;
      }
    }

    children.add(chatUserItemRoot);
  }

  @Override
  public Tab getRoot() {
    return channelTabRoot;
  }

  @Override
  protected TextInputControl messageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  protected void onMention(ChatMessage chatMessage) {
    if (preferencesService.getPreferences().getNotification().getNotifyOnAtMentionOnlyEnabled()
        && !chatMessage.getMessage().contains("@" + userService.getUsername())) {
      return;
    }
    if (!hasFocus()) {
      audioService.playChatMentionSound();
      showNotificationIfNecessary(chatMessage);
      incrementUnreadMessagesCount(1);
      setUnread(true);
    }
  }

  @Override
  protected String getMessageCssClass(String login) {
    ChatChannelUser chatUser = chatService.getOrCreateChatUser(login, channel.getName());
    Optional<Player> currentPlayerOptional = playerService.getCurrentPlayer();

    if (currentPlayerOptional.isPresent()) {
      Player currentPlayer = currentPlayerOptional.get();

      if (!Objects.equals(login, currentPlayer.getUsername())
          && chatUser.isModerator()) {
        return CSS_CLASS_MODERATOR;
      }
    }

    return super.getMessageCssClass(login);
  }

  private void addChatColorListener() {
    JavaFxUtil.addListener(preferencesService.getPreferences().getChat().chatColorModeProperty(), new WeakChangeListener<>(chatColorModeChangeListener));
  }

  private void addUserFilterPopup() {
    filterUserPopup = new Popup();
    filterUserPopup.setAutoFix(false);
    filterUserPopup.setAutoHide(true);
    filterUserPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);

    userFilterController = uiService.loadFxml("theme/chat/user_filter.fxml");
    userFilterController.setChannelController(this);
    userFilterController.filterAppliedProperty().addListener(((observable, oldValue, newValue) -> advancedUserFilter.setSelected(newValue)));
    filterUserPopup.getContent().setAll(userFilterController.getRoot());
  }

  private void updateUserMessageColor(ChatChannelUser chatUser) {
    String color = "";
    if (chatUser.getColor() != null) {
      color = JavaFxUtil.toRgbCode(chatUser.getColor());
    }
    getJsObject().call("updateUserMessageColor", chatUser.getUsername(), color);
  }

  private void removeUserMessageClass(ChatChannelUser chatUser, String cssClass) {
    //TODO: DOM Exception 12 when cssClass string is empty string, not sure why cause .remove in the js should be able to handle it
    if (cssClass.isEmpty()) {
      return;
    }
    Platform.runLater(() -> getJsObject().call("removeUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, chatUser.getUsername()), cssClass));

  }

  private void addUserMessageClass(ChatChannelUser player, String cssClass) {
    Platform.runLater(() -> getJsObject().call("addUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, player.getUsername()), cssClass));
  }

  private void updateUserMessageDisplay(ChatChannelUser chatUser, String display) {
    Platform.runLater(() -> getJsObject().call("updateUserMessageDisplay", chatUser.getUsername(), display));
  }

  private synchronized void onUserJoinedChannel(ChatChannelUser chatUser) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    JavaFxUtil.addListener(chatUser.moderatorProperty(), createWeakModeratorForChannelListener(chatUser));
    JavaFxUtil.addListener(chatUser.playerProperty(), createWeakPlayerListener(chatUser));
    JavaFxUtil.addListener(chatPrefs.hideFoeMessagesProperty(), createWeakHideFoeMessagesListener(chatUser));
    JavaFxUtil.addListener(chatUser.colorProperty(), createWeakColorPropertyListener(chatUser));

    Collection<TreeItem<ChatUserTreeItem>> targetPanesForUser = getTargetTreeItemsForUser(chatUser);

    for (TreeItem<ChatUserTreeItem> treeItem : targetPanesForUser) {
      ChatUserItemController chatUserItemController = createChatUserControlIfNecessary(treeItem, chatUser);

      // Apply filter if exists
      if (!userSearchTextField.textProperty().get().isEmpty()) {
        chatUserItemController.setVisible(isUsernameMatch(chatUserItemController));
      }
      if (userFilterController.isFilterApplied()) {
        chatUserItemController.setVisible(userFilterController.filterUser(chatUserItemController));
      }
    }
  }

  @NotNull
  private WeakChangeListener<Color> createWeakColorPropertyListener(ChatChannelUser chatUser) {
    ChangeListener<Color> listener = (observable, oldValue, newValue) ->
        Platform.runLater(() -> updateUserMessageColor(chatUser));

    colorPropertyListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private WeakChangeListener<Boolean> createWeakModeratorForChannelListener(ChatChannelUser chatUser) {
    ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
      if (newValue) {
        removeFromPane(chatUser, othersTreeItem);
        removeFromPane(chatUser, chatOnlyTreeItem);
        removeUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);

        addToTreeItem(chatUser, moderatorsTreeItem);
        addUserMessageClass(chatUser, CSS_CLASS_MODERATOR);
      } else {
        removeFromPane(chatUser, moderatorsTreeItem);
        removeUserMessageClass(chatUser, CSS_CLASS_MODERATOR);

        Optional<Player> optionalPlayer = chatUser.getPlayer();
        if (optionalPlayer.isPresent()) {
          SocialStatus socialStatus = optionalPlayer.get().getSocialStatus();
          addToTreeItem(chatUser, getTreeItemForSocialStatus(socialStatus));
        } else {
          addToTreeItem(chatUser, chatOnlyTreeItem);
        }
      }
    };
    moderatorForChannelListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private WeakChangeListener<SocialStatus> createWeakSocialStatusListener(ChatPrefs chatPrefs, ChatChannelUser chatUser, Player player) {
    ChangeListener<SocialStatus> listener = (observable, oldValue, newValue) -> {

      removeFromPane(chatUser, getTreeItemForSocialStatus(oldValue));
      addToTreeItem(chatUser, getTreeItemForSocialStatus(newValue));

      removeUserMessageClass(chatUser, oldValue.getCssClass());
      addUserMessageClass(chatUser, newValue.getCssClass());

      addToTreeItem(chatUser, getTreeItemForSocialStatus(newValue));

      if (chatPrefs.getHideFoeMessages() && newValue == FOE) {
        updateUserMessageDisplay(chatUser, "none");
      } else {
        updateUserMessageDisplay(chatUser, "");
      }
    };
    socialStatusMessagesListeners.computeIfAbsent(player.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private ChangeListener<Boolean> createWeakHideFoeMessagesListener(ChatChannelUser chatUser) {
    ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
      if (newValue && chatUser.getPlayer().isPresent() && chatUser.getPlayer().get().getSocialStatus() == FOE) {
        updateUserMessageDisplay(chatUser, "none");
      } else {
        updateUserMessageDisplay(chatUser, "");
      }
    };
    hideFoeMessagesListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private ChangeListener<Player> createWeakPlayerListener(ChatChannelUser chatUser) {
    ChangeListener<Player> listener = (observable, oldValue, newPlayer) -> {
      if (newPlayer == null) {
        removeFromPane(chatUser, getTreeItemForSocialStatus(oldValue.getSocialStatus()));

        addToTreeItem(chatUser, chatOnlyTreeItem);
        addUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);
        return;
      }

      removeFromPane(chatUser, chatOnlyTreeItem);
      removeUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);

      addToTreeItem(chatUser, getTreeItemForSocialStatus(newPlayer.getSocialStatus()));

      ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
      JavaFxUtil.addListener(newPlayer.socialStatusProperty(), createWeakSocialStatusListener(chatPrefs, chatUser, newPlayer));
    };
    playerListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  private TreeItem<ChatUserTreeItem> getTreeItemForSocialStatus(SocialStatus socialStatus) {
    switch (socialStatus) {
      case FRIEND:
        return friendsTreeItem;
      case FOE:
        return foesTreeItem;
      default:
        return othersTreeItem;
    }
  }

  private void onUserLeft(String username) {
    JavaFxUtil.assertBackgroundThread();

    playerListeners.remove(username);
    hideFoeMessagesListeners.remove(username);
    socialStatusMessagesListeners.remove(username);
    moderatorForChannelListeners.remove(username);
    colorPropertyListeners.remove(username);

    synchronized (userToChatUserControls) {
      Map<TreeItem<ChatUserTreeItem>, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls.get(username);
      if (paneToChatUserControlMap == null) {
        return;
      }

      Platform.runLater(() -> {
        synchronized (userToChatUserControls) {
          for (Entry<TreeItem<ChatUserTreeItem>, ChatUserItemController> entry : paneToChatUserControlMap.entrySet()) {
            entry.getKey().getChildren().remove(entry.getKey());
          }
          paneToChatUserControlMap.clear();
        }
      });
      userToChatUserControls.remove(username);
    }
  }

  private void addToTreeItem(ChatChannelUser chatUser, TreeItem<ChatUserTreeItem> treeItem) {
    createChatUserControlIfNecessary(treeItem, chatUser);
  }

  private void removeFromPane(ChatChannelUser chatUser, TreeItem<?> treeItem) {
    synchronized (userToChatUserControls) {
      Map<TreeItem<ChatUserTreeItem>, ChatUserItemController> treeItemToChatUserControl = userToChatUserControls.get(chatUser.getUsername());
      if (treeItemToChatUserControl == null) {
        // User has not yet been added to this pane; no need to remove him
        return;
      }
      ChatUserItemController controller = treeItemToChatUserControl.remove(treeItem);
      if (controller == null) {
        return;
      }
      Pane root = controller.getRoot();
      if (root != null) {
        Platform.runLater(() -> treeItem.getChildren().remove(root));
      }
    }
  }

  /**
   * Creates a {@link ChatUserItemController} for the given chat user and adds it to the given pane if there isn't
   * already such a control in this pane. After the control has been added, the user search filter is applied.
   */
  private ChatUserItemController createChatUserControlIfNecessary(TreeItem<ChatUserTreeItem> treeItem, ChatChannelUser chatUser) {
    String username = chatUser.getUsername();
    synchronized (userToChatUserControls) {
      Map<TreeItem<ChatUserTreeItem>, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls
          .computeIfAbsent(username, s -> new HashMap<>(1, 1));

      ChatUserItemController existingChatUserItemController = paneToChatUserControlMap.get(treeItem);
      if (existingChatUserItemController != null) {
        return existingChatUserItemController;
      }

      ChatUserItemController chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
      chatUserItemController.setChatUser(chatUser);
      paneToChatUserControlMap.put(treeItem, chatUserItemController);

      updateRandomColorsAllowed(treeItem, chatUser, chatUserItemController);

      Platform.runLater(() -> addChatUserItemSorted(treeItem, chatUserItemController));

      return chatUserItemController;
    }
  }

  private void updateRandomColorsAllowed(TreeItem<ChatUserTreeItem> treeItem, ChatChannelUser chatUser, ChatUserItemController chatUserItemController) {
    chatUserItemController.setRandomColorAllowed(
        (treeItem == othersTreeItem || treeItem == chatOnlyTreeItem)
            && chatUser.getPlayer().isPresent()
            && chatUser.getPlayer().get().getSocialStatus() != SELF
    );
  }

  private Collection<TreeItem<ChatUserTreeItem>> getTargetTreeItemsForUser(ChatChannelUser chatUser) {
    ArrayList<TreeItem<ChatUserTreeItem>> treeItems = new ArrayList<>(3);

    if (chatUser.isModerator()) {
      treeItems.add(moderatorsTreeItem);
    }

    if (chatUser.getPlayer().isPresent()) {
      treeItems.add(getTreeItemForSocialStatus(chatUser.getPlayer().get().getSocialStatus()));
    } else {
      treeItems.add(chatOnlyTreeItem);
    }

    return treeItems;
  }

  public void onKeyReleased(KeyEvent event) {
    if (event.getCode() == KeyCode.ESCAPE) {
      onSearchFieldClose();
    } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
      searchField.clear();
      searchField.setVisible(!searchField.isVisible());
      searchField.requestFocus();
    }
  }

  public void onSearchFieldClose() {
    searchField.setVisible(false);
    searchField.clear();
  }

  private void addSearchFieldListener() {
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.trim().isEmpty()) {
        getJsObject().call("removeHighlight");
      } else {
        getJsObject().call("highlightText", newValue);
      }
    });
  }

  public void onAdvancedUserFilter(ActionEvent actionEvent) {
    advancedUserFilter.setSelected(userFilterController.isFilterApplied());
    if (filterUserPopup.isShowing()) {
      filterUserPopup.hide();
      return;
    }

    ToggleButton button = (ToggleButton) actionEvent.getSource();

    Bounds screenBounds = advancedUserFilter.localToScreen(advancedUserFilter.getBoundsInLocal());
    filterUserPopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }

  @Override
  protected String getInlineStyle(String username) {
    ChatChannelUser chatUser = chatService.getOrCreateChatUser(username, channel.getName());

    Optional<Player> playerOptional = playerService.getPlayerForUsername(username);

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    String color = "";
    String display = "";

    if (chatPrefs.getHideFoeMessages() && playerOptional.isPresent() && playerOptional.get().getSocialStatus() == FOE) {
      display = "display: none;";
    } else {
      ChatColorMode chatColorMode = chatPrefs.getChatColorMode();
      if ((chatColorMode == ChatColorMode.CUSTOM || chatColorMode == ChatColorMode.RANDOM)
          && chatUser.getColor() != null) {
        color = createInlineStyleFromColor(chatUser.getColor());
      }
    }

    return String.format("%s%s", color, display);
  }
}
