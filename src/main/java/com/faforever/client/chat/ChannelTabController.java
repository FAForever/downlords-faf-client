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
  private final Map<String, Collection<ChangeListener<Player>>> playerListeners;
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
    String lowerCaseSearchString = chatUserItemController.getChatUser().getUsername().toLowerCase();
    return lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase());
  }

  /**
   * Inserts the given ChatUserControl into the given Pane such that it is correctly sorted alphabetically.
   */
  private void addChatUserItemSorted(Pane pane, ChatUserItemController chatUserItemController) {
    ObservableList<Node> children = pane.getChildren();

    Pane chatUserItemRoot = chatUserItemController.getRoot();
    ChatUser chatUser = chatUserItemController.getChatUser();

    Optional<Player> playerOptional = chatUser.getPlayer();

    if (playerOptional.isPresent() && playerOptional.get().getSocialStatus() == SELF) {
      children.add(0, chatUserItemRoot);
      return;
    }

    String thisUsername = chatUser.getUsername();
    for (Node child : children) {
      String otherUsername = ((ChatUserItemController) child.getUserData()).getChatUser().getUsername();

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
    ChatUser chatUser = chatService.getOrCreateChatUser(login);
    Optional<Player> currentPlayerOptional = playerService.getCurrentPlayer();

    if (currentPlayerOptional.isPresent()) {
      Player currentPlayer = currentPlayerOptional.get();

      if (!Objects.equals(login, currentPlayer.getUsername())
          && chatUser.getModeratorInChannels().contains(channel.getName())) {
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

  public void updateUserMessageColor(ChatUser chatUser) {
    String color = "";
    if (chatUser.getColor() != null) {
      color = JavaFxUtil.toRgbCode(chatUser.getColor());
    }
    getJsObject().call("updateUserMessageColor", chatUser.getUsername(), color);
  }

  private void removeUserMessageClass(ChatUser chatUser, String cssClass) {
    //TODO: DOM Exception 12 when cssClass string is empty string, not sure why cause .remove in the js should be able to handle it
    if (cssClass.isEmpty()) {
      return;
    }
    Platform.runLater(() -> getJsObject().call("removeUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, chatUser.getUsername()), cssClass));

  }

  private void addUserMessageClass(ChatUser player, String cssClass) {
    Platform.runLater(() -> getJsObject().call("addUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, player.getUsername()), cssClass));
  }

  private void updateUserMessageDisplay(ChatUser chatUser, String display) {
    Platform.runLater(() -> getJsObject().call("updateUserMessageDisplay", chatUser.getUsername(), display));
  }

  private synchronized void onUserJoinedChannel(ChatUser chatUser) {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    String username = chatUser.getUsername();

    JavaFxUtil.addListener(chatUser.getModeratorInChannels(), createWeakModeratorForChannelListener(chatUser));
    JavaFxUtil.addListener(chatUser.playerProperty(), createWeakPlayerListener(chatUser));
    JavaFxUtil.addListener(chatPrefs.hideFoeMessagesProperty(), createWeakHideFoeMessagesListener(chatUser));
    JavaFxUtil.addListener(chatUser.colorProperty(), createWeakColorPropertyListener(chatUser));

    Collection<Pane> targetPanesForUser = getTargetPanesForUser(chatUser);

    for (Pane pane : targetPanesForUser) {
      ChatUserItemController chatUserItemController = createChatUserControlIfNecessary(pane, chatUser);

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
  private WeakChangeListener<Color> createWeakColorPropertyListener(ChatUser chatUser) {
    ChangeListener<Color> listener = (observable, oldValue, newValue) ->
        Platform.runLater(() -> updateUserMessageColor(chatUser));

    colorPropertyListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakChangeListener<>(listener);
  }

  @NotNull
  private WeakSetChangeListener<String> createWeakModeratorForChannelListener(ChatUser chatUser) {
    SetChangeListener<String> listener = change -> {
      if (change.wasAdded()) {
        removeFromPane(chatUser, othersPane);
        removeFromPane(chatUser, chatOnlyPane);
        removeUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);

        addToPane(chatUser, moderatorsPane);
        addUserMessageClass(chatUser, CSS_CLASS_MODERATOR);
      } else {
        removeFromPane(chatUser, moderatorsPane);
        removeUserMessageClass(chatUser, CSS_CLASS_MODERATOR);

        Optional<Player> optionalPlayer = chatUser.getPlayer();
        if (optionalPlayer.isPresent()) {
          SocialStatus socialStatus = optionalPlayer.get().getSocialStatus();
          addToPane(chatUser, getPaneForSocialStatus(socialStatus));
        } else {
          addToPane(chatUser, chatOnlyPane);
        }
      }
    };
    moderatorForChannelListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
    return new WeakSetChangeListener<>(listener);
  }

  @NotNull
  private WeakChangeListener<SocialStatus> createWeakSocialStatusListener(ChatPrefs chatPrefs, ChatUser chatUser, Player player) {
    ChangeListener<SocialStatus> listener = (observable, oldValue, newValue) -> {

      removeFromPane(chatUser, getPaneForSocialStatus(oldValue));
      addToPane(chatUser, getPaneForSocialStatus(newValue));

      removeUserMessageClass(chatUser, oldValue.getCssClass());
      addUserMessageClass(chatUser, newValue.getCssClass());

      addToPane(chatUser, getPaneForSocialStatus(newValue));

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
  private ChangeListener<Boolean> createWeakHideFoeMessagesListener(ChatUser chatUser) {
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
  private ChangeListener<Player> createWeakPlayerListener(ChatUser chatUser) {
    ChangeListener<Player> listener = (observable, oldValue, newPlayer) -> {
      if (newPlayer == null) {
        removeFromPane(chatUser, getPaneForSocialStatus(oldValue.getSocialStatus()));

        addToPane(chatUser, chatOnlyPane);
        addUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);
        return;
      }

      removeFromPane(chatUser, chatOnlyPane);
      removeUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);

      addToPane(chatUser, getPaneForSocialStatus(newPlayer.getSocialStatus()));

      ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
      JavaFxUtil.addListener(newPlayer.socialStatusProperty(), createWeakSocialStatusListener(chatPrefs, chatUser, newPlayer));
    };
    playerListeners.computeIfAbsent(chatUser.getUsername(), i -> new ArrayList<>()).add(listener);
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

    playerListeners.remove(username);
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

  private ChatUserItemController addToPane(ChatUser chatUser, Pane pane) {
    return createChatUserControlIfNecessary(pane, chatUser);
  }

  private void removeFromPane(ChatUser chatUser, Pane pane) {
    synchronized (userToChatUserControls) {
      Map<Pane, ChatUserItemController> paneChatUserControlMap = userToChatUserControls.get(chatUser.getUsername());
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
   * Creates a {@link ChatUserItemController} for the given chat user and adds it to the given pane if there isn't
   * already such a control in this pane. After the control has been added, the user search filter is applied.
   */
  private ChatUserItemController createChatUserControlIfNecessary(Pane pane, ChatUser chatUser) {
    String username = chatUser.getUsername();
    synchronized (userToChatUserControls) {
      Map<Pane, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls
          .computeIfAbsent(username, s -> new HashMap<>(1, 1));

      ChatUserItemController existingChatUserItemController = paneToChatUserControlMap.get(pane);
      if (existingChatUserItemController != null) {
        return existingChatUserItemController;
      }

      ChatUserItemController chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
      chatUserItemController.setChatUser(chatUser);
      paneToChatUserControlMap.put(pane, chatUserItemController);

      updateRandomColorsAllowed(pane, chatUser, chatUserItemController);

      Platform.runLater(() -> addChatUserItemSorted(pane, chatUserItemController));

      return chatUserItemController;
    }
  }

  private void updateRandomColorsAllowed(Pane pane, ChatUser chatUser, ChatUserItemController chatUserItemController) {
    chatUserItemController.setRandomColorsAllowed(
        (pane == othersPane || pane == chatOnlyPane)
            && chatUser.getPlayer().isPresent()
            && chatUser.getPlayer().get().getSocialStatus() != SELF
    );
  }

  private Collection<Pane> getTargetPanesForUser(ChatUser chatUser) {
    ArrayList<Pane> panes = new ArrayList<>(3);

    if (chatUser.getModeratorInChannels().contains(channel.getName())) {
      panes.add(moderatorsPane);
    }

    if (chatUser.getPlayer().isPresent()) {
      panes.add(getPaneForSocialStatus(chatUser.getPlayer().get().getSocialStatus()));
    } else {
      panes.add(chatOnlyPane);
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
