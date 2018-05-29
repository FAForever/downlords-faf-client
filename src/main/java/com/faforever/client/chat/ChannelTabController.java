package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.collections.WeakSetChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;

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
  private final Map<String, Map<Pane, ChatUserItemController>> userToChatUserControls;
  private final ThreadPoolExecutor threadPoolExecutor;
  private final TaskScheduler taskScheduler;
  public ToggleButton advancedUserFilter;
  public HBox searchFieldContainer;
  public Button closeSearchFieldButton;
  public TextField searchField;
  public VBox channelTabScrollPaneVBox;
  public TitledPane moderatorsTitlePane;
  public TitledPane friendsTitlePane;
  public TitledPane othersTitlePane;
  public TitledPane chatOnlyTitlePane;
  public TitledPane foesTitlePane;
  public Tab channelTabRoot;
  public WebView messagesWebView;
  public Pane moderatorsPane;
  public Pane friendsPane;
  public Pane foesPane;
  public Pane othersPane;
  public Pane chatOnlyPane;
  public TextField userSearchTextField;
  public TextField messageTextField;
  private Channel channel;
  private Popup filterUserPopup;
  private MapChangeListener<String, ChatUser> usersChangeListener;
  private ChangeListener<ChatColorMode> chatColorModeChangeListener;
  private UserFilterController userFilterController;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<Boolean>>> chatOnlyListeners;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<Boolean>>> hideFoeMessagesListeners;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<SocialStatus>>> socialStatusMessagesListeners;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<SetChangeListener<String>>> moderatorForChannelListeners;
  /** Prevents garbage collection of weak listeners. Key is the username. */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, Collection<ChangeListener<Color>>> colorPropertyListeners;
  private ScheduledFuture<?> presenceStatusUpdateFuture;

  // TODO cut dependencies
  @Inject
  public ChannelTabController(UserService userService, ChatService chatService,
                              PreferencesService preferencesService,
                              PlayerService playerService, AudioService audioService, TimeService timeService,
                              I18n i18n, ImageUploadService imageUploadService,
                              NotificationService notificationService, ReportingService reportingService,
                              UiService uiService, AutoCompletionHelper autoCompletionHelper, EventBus eventBus,
                              WebViewConfigurer webViewConfigurer, ThreadPoolExecutor threadPoolExecutor,
                              TaskScheduler taskScheduler, CountryFlagService countryFlagService) {

    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService,
        timeService, i18n, imageUploadService, notificationService, reportingService, uiService, autoCompletionHelper,
        eventBus, countryFlagService);

    this.threadPoolExecutor = threadPoolExecutor;
    this.taskScheduler = taskScheduler;

    chatOnlyListeners = new HashMap<>();
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

  // TODO clean this up
  Map<String, Map<Pane, ChatUserItemController>> getUserToChatUserControls() {
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
      for (Map<Pane, ChatUserItemController> chatUserControlMap : userToChatUserControls.values()) {
        for (Map.Entry<Pane, ChatUserItemController> chatUserControlEntry : chatUserControlMap.entrySet()) {
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
    String lowerCaseSearchString = chatUserItemController.getPlayer().getUsername().toLowerCase();
    return lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase());
  }

  /**
   * Inserts the given ChatUserControl into the given Pane such that it is correctly sorted alphabetically.
   */
  private void addChatUserItemSorted(Pane pane, ChatUserItemController chatUserItemController) {
    ObservableList<Node> children = pane.getChildren();

    Pane chatUserItemRoot = chatUserItemController.getRoot();
    if (chatUserItemController.getPlayer().getSocialStatus() == SELF) {
      children.add(0, chatUserItemRoot);
      return;
    }

    String thisUsername = chatUserItemController.getPlayer().getUsername();
    for (Node child : children) {
      String otherUsername = ((ChatUserItemController) child.getUserData()).getPlayer().getUsername();

      if (otherUsername.equalsIgnoreCase(userService.getUsername())) {
        continue;
      }

      if (thisUsername.compareToIgnoreCase(otherUsername) < 0) {
        children.add(children.indexOf(child), chatUserItemRoot);
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
    Player player = playerService.getPlayerForUsername(login);
    if (player != null
        && !player.equals(playerService.getCurrentPlayer())
        && player.getModeratorForChannels().contains(channel.getName())) {
      return CSS_CLASS_MODERATOR;
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

  public void updateUserMessageColor(ChatUser chatUser) {
    String color = "";
    if (chatUser.getColor() != null) {
      color = JavaFxUtil.toRgbCode(chatUser.getColor());
    }
    getJsObject().call("updateUserMessageColor", chatUser.getUsername(), color);
  }

  private void removeUserMessageClass(Player player, String cssClass) {
    //TODO: DOM Exception 12 when cssClass string is empty string, not sure why cause .remove in the js should be able to handle it
    if (cssClass.isEmpty()) {
      return;
    }
    Platform.runLater(() -> getJsObject().call("removeUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, player.getUsername()), cssClass));

  }

  private void setUserMessageClass(Player player, String cssClass) {
    Platform.runLater(() -> getJsObject().call("setUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, player.getUsername()), cssClass));
  }

  private void updateUserMessageDisplay(Player player, String display) {
    Platform.runLater(() -> getJsObject().call("updateUserMessageDisplay", player.getUsername(), display));
  }

  private synchronized void onUserJoinedChannel(ChatUser chatUser) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String username = chatUser.getUsername();

    // TODO for "chat only" users, no player should be created. Then again, FAF will only allow players to join IRC in
    // future, so "ChatUser" won't be a thing.
    Player player = playerService.createAndGetPlayerForUsername(username);

    JavaFxUtil.bind(player.moderatorForChannelsProperty(), chatUser.moderatorInChannelsProperty());
    JavaFxUtil.bind(player.usernameProperty(), chatUser.usernameProperty());

    JavaFxUtil.addListener(player.socialStatusProperty(), createWeakSocialStatusListener(chatPrefs, player));
    JavaFxUtil.addListener(player.chatOnlyProperty(), createWeakChatOnlyListener(chatUser, username, player));
    JavaFxUtil.addListener(player.getModeratorForChannels(), createWeakModeratorForChannelListener(player));
    JavaFxUtil.addListener(chatPrefs.hideFoeMessagesProperty(), createWeakHideFoeMessagesListener(player));
    JavaFxUtil.addListener(chatUser.colorProperty(), createWeakColorPropertyListener(chatUser));

    Collection<Pane> targetPanesForUser = getTargetPanesForUser(player);

    for (Pane pane : targetPanesForUser) {
      ChatUserItemController chatUserItemController = createChatUserControlForPlayerIfNecessary(pane, player);

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
  private ChangeListener<Color> createWeakColorPropertyListener(ChatUser chatUser) {
    ChangeListener<Color> listener = (observable, oldValue, newValue) ->
        Platform.runLater(() -> updateUserMessageColor(chatUser));
    colorPropertyListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private SetChangeListener<String> createWeakModeratorForChannelListener(Player player) {
    SetChangeListener<String> listener = change -> {
      if (change.wasAdded()) {
        addToPane(player, moderatorsPane);
        removeFromPane(player, othersPane);
        removeFromPane(player, chatOnlyPane);
        setUserMessageClass(player, CSS_CLASS_MODERATOR);

      } else {
        removeFromPane(player, moderatorsPane);
        SocialStatus socialStatus = player.getSocialStatus();
        if (socialStatus == OTHER || socialStatus == SELF) {
          addToPane(player, othersPane);
        }
        removeUserMessageClass(player, CSS_CLASS_MODERATOR);
      }
    };
    moderatorForChannelListeners.computeIfAbsent(player.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakSetChangeListener<>(listener);
  }

  @NotNull
  private ChangeListener<SocialStatus> createWeakSocialStatusListener(ChatPrefs chatPrefs, Player player) {
    ChangeListener<SocialStatus> listener = (observable, oldValue, newValue) -> {
      if (oldValue == OTHER && player.isChatOnly()) {
        removeFromPane(player, chatOnlyPane);
        removeUserMessageClass(player, CSS_CLASS_CHAT_ONLY);
      } else {
        removeFromPane(player, getPaneForSocialStatus(oldValue));
        removeUserMessageClass(player, oldValue.getCssClass());
      }
      if (newValue == OTHER && player.isChatOnly()) {
        addToPane(player, chatOnlyPane);
        setUserMessageClass(player, CSS_CLASS_CHAT_ONLY);
      } else {
        addToPane(player, getPaneForSocialStatus(newValue));
        setUserMessageClass(player, newValue.getCssClass());
      }

      if (chatPrefs.getHideFoeMessages() && newValue == FOE) {
        updateUserMessageDisplay(player, "none");
      }

      if (chatPrefs.getHideFoeMessages() && oldValue == FOE) {
        updateUserMessageDisplay(player, "");
      }
    };
    socialStatusMessagesListeners.computeIfAbsent(player.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private ChangeListener<Boolean> createWeakHideFoeMessagesListener(Player player) {
    ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
      if (newValue && player.getSocialStatus() == FOE) {
        updateUserMessageDisplay(player, "none");
      } else {
        updateUserMessageDisplay(player, "");
      }
    };
    hideFoeMessagesListeners.computeIfAbsent(player.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private ChangeListener<Boolean> createWeakChatOnlyListener(ChatUser chatUser, String username, Player player) {
    ChangeListener<Boolean> listener = (observable, oldValue, newValue) -> {
      if (player.getSocialStatus() == OTHER && !chatUser.getModeratorInChannels().contains(username)) {
        if (newValue) {
          removeFromPane(player, othersPane);
          addToPane(player, chatOnlyPane);
          setUserMessageClass(player, CSS_CLASS_CHAT_ONLY);
        } else {
          removeFromPane(player, chatOnlyPane);
          addToPane(player, getPaneForSocialStatus(player.getSocialStatus()));
          removeUserMessageClass(player, CSS_CLASS_CHAT_ONLY);
        }
      }
    };
    chatOnlyListeners.computeIfAbsent(player.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  private Pane getPaneForSocialStatus(SocialStatus socialStatus) {
    switch (socialStatus) {
      case FRIEND:
        return friendsPane;
      case FOE:
        return foesPane;
      default:
        return othersPane;
    }
  }

  private void onUserLeft(String username) {
    JavaFxUtil.assertBackgroundThread();

    chatOnlyListeners.remove(username);
    hideFoeMessagesListeners.remove(username);
    socialStatusMessagesListeners.remove(username);
    moderatorForChannelListeners.remove(username);
    colorPropertyListeners.remove(username);

    synchronized (userToChatUserControls) {
      Map<Pane, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls.get(username);
      if (paneToChatUserControlMap == null) {
        return;
      }

      Platform.runLater(() -> {
        synchronized (userToChatUserControls) {
          for (Map.Entry<Pane, ChatUserItemController> entry : paneToChatUserControlMap.entrySet()) {
            entry.getKey().getChildren().remove(entry.getValue().getRoot());
          }
          paneToChatUserControlMap.clear();
        }
      });
      userToChatUserControls.remove(username);
    }
  }

  private ChatUserItemController addToPane(Player player, Pane pane) {
    return createChatUserControlForPlayerIfNecessary(pane, player);
  }

  private void removeFromPane(Player player, Pane pane) {
    synchronized (userToChatUserControls) {
      Map<Pane, ChatUserItemController> paneChatUserControlMap = userToChatUserControls.get(player.getUsername());
      if (paneChatUserControlMap == null) {
        // User has not yet been added to this pane; no need to remove him
        return;
      }
      ChatUserItemController controller = paneChatUserControlMap.remove(pane);
      if (controller == null) {
        return;
      }
      Pane root = controller.getRoot();
      if (root != null) {
        Platform.runLater(() -> pane.getChildren().remove(root));
      }
    }
  }

  /**
   * Creates a {@link ChatUserItemController} for the given playerInfoBean and adds it to the given pane if there isn't
   * already such a control in this pane. After the control has been added, the user search filter is applied.
   */
  private ChatUserItemController createChatUserControlForPlayerIfNecessary(Pane pane, Player player) {
    String username = player.getUsername();
    synchronized (userToChatUserControls) {
      Map<Pane, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls
          .computeIfAbsent(username, s -> new HashMap<>(1, 1));

      ChatUserItemController existingChatUserItemController = paneToChatUserControlMap.get(pane);
      if (existingChatUserItemController != null) {
        return existingChatUserItemController;
      }

      ChatUserItemController chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
      chatUserItemController.setPlayer(player);
      paneToChatUserControlMap.put(pane, chatUserItemController);

      chatUserItemController.setRandomColorsAllowedInPane((pane == othersPane || pane == chatOnlyPane) && player.getSocialStatus() != SELF);

      Platform.runLater(() -> addChatUserItemSorted(pane, chatUserItemController));

      return chatUserItemController;
    }
  }

  private Collection<Pane> getTargetPanesForUser(Player player) {
    ArrayList<Pane> panes = new ArrayList<>(3);

    if (player.getModeratorForChannels().contains(channel.getName())) {
      panes.add(moderatorsPane);
    }

    Pane pane = getPaneForSocialStatus(player.getSocialStatus());
    if (pane == othersPane && player.isChatOnly()) {
      panes.add(chatOnlyPane);
    } else {
      panes.add(pane);
    }

    return panes;
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
}
