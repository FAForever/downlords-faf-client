package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.clan.ClanService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ReplayService;
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
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
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
import java.util.concurrent.ConcurrentHashMap;
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
  public Button advancedUserFilter;
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

  // TODO cut dependencies
  @Inject
  public ChannelTabController(ClanService clanService, UserService userService, ChatService chatService,
                              PlatformService platformService, PreferencesService preferencesService,
                              PlayerService playerService, AudioService audioService, TimeService timeService,
                              I18n i18n, ImageUploadService imageUploadService, UrlPreviewResolver urlPreviewResolver,
                              NotificationService notificationService, ReportingService reportingService,
                              UiService uiService, AutoCompletionHelper autoCompletionHelper, EventBus eventBus,
                              WebViewConfigurer webViewConfigurer, ThreadPoolExecutor threadPoolExecutor,
                              TaskScheduler taskScheduler, CountryFlagService countryFlagService,
                              ReplayService replayService, ClientProperties clientProperties) {

    super(clanService, webViewConfigurer, userService, chatService, platformService, preferencesService, playerService,
        audioService, timeService, i18n, imageUploadService, urlPreviewResolver, notificationService, reportingService,
        uiService, autoCompletionHelper, eventBus, countryFlagService, replayService, clientProperties);

    userToChatUserControls = FXCollections.observableMap(new ConcurrentHashMap<>());
    this.threadPoolExecutor = threadPoolExecutor;
    this.taskScheduler = taskScheduler;
  }


  // TODO clean this up
  public Map<String, Map<Pane, ChatUserItemController>> getUserToChatUserControls() {
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
    taskScheduler.scheduleWithFixedDelay(this::updatePresenceStatusIndicators, Date.from(Instant.now()), IDLE_TIME_UPDATE_DELAY);
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
    Platform.runLater(() -> userToChatUserControls.values().stream()
        .flatMap(paneChatUserItemControllerMap -> paneChatUserItemControllerMap.values().stream())
        .forEach(ChatUserItemController::updatePresenceStatusIndicator));
  }

  /**
   * Hides all chat user controls whose username does not contain the string entered in the {@link
   * #userSearchTextField}.
   */
  private void filterChatUserControlsBySearchString() {
    synchronized (userToChatUserControls) {
      for (Map<Pane, ChatUserItemController> chatUserControlMap : userToChatUserControls.values()) {
        synchronized (chatUserControlMap) {
          for (Map.Entry<Pane, ChatUserItemController> chatUserControlEntry : chatUserControlMap.entrySet()) {
            ChatUserItemController chatUserItemController = chatUserControlEntry.getValue();
            chatUserItemController.setVisible(isUsernameMatch(chatUserItemController));
          }
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
    preferencesService.getPreferences().getChat().chatColorModeProperty().addListener(new WeakChangeListener<>(chatColorModeChangeListener));
  }

  private void addUserFilterPopup() {
    filterUserPopup = new Popup();
    filterUserPopup.setAutoFix(false);
    filterUserPopup.setAutoHide(true);
    filterUserPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);

    userFilterController = uiService.loadFxml("theme/chat/user_filter.fxml");
    userFilterController.setChannelController(this);
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
    Platform.runLater(() -> getJsObject().call("updateUserMessageDisplay", String.format(USER_CSS_CLASS_FORMAT, player.getUsername()), display));
  }

  private synchronized void onUserJoinedChannel(ChatUser chatUser) {
    JavaFxUtil.assertBackgroundThread();

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String username = chatUser.getUsername();
    Player player = playerService.createAndGetPlayerForUsername(username);

    player.moderatorForChannelsProperty().bind(chatUser.moderatorInChannelsProperty());
    player.usernameProperty().addListener((observable, oldValue, newValue) -> {
      Map<Pane, ChatUserItemController> userItemControllers = userToChatUserControls.get(oldValue);
      if (userItemControllers == null) {
        return;
      }
      for (Map.Entry<Pane, ChatUserItemController> entry : userItemControllers.entrySet()) {
        Pane pane = entry.getKey();
        ChatUserItemController chatUserItemController = entry.getValue();

        pane.getChildren().remove(chatUserItemController.getRoot());
        addChatUserItemSorted(pane, chatUserItemController);
      }
    });
    player.usernameProperty().bind(chatUser.usernameProperty());

    player.socialStatusProperty().addListener((observable, oldValue, newValue) -> {
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

      if (oldValue == OTHER && player.isChatOnly()) {
        removeFromPane(player, chatOnlyPane);
        removeUserMessageClass(player, CSS_CLASS_CHAT_ONLY);
      } else {
        removeFromPane(player, getPaneForSocialStatus(oldValue));
        removeUserMessageClass(player, oldValue.getCssClass());
      }

      if (chatPrefs.getHideFoeMessages() && oldValue == FOE) {
        updateUserMessageDisplay(player, "");
      }
    });

    player.chatOnlyProperty().addListener((observable, oldValue, newValue) -> {
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
    });

    player.getModeratorForChannels().addListener((SetChangeListener<String>) change -> {
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
    });

    chatPrefs.hideFoeMessagesProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue && player.getSocialStatus() == FOE) {
        updateUserMessageDisplay(player, "none");
      } else {
        updateUserMessageDisplay(player, "");
      }
    });

    chatUser.colorProperty().addListener((observable, oldValue, newValue) ->
        Platform.runLater(() -> updateUserMessageColor(chatUser))
    );

    Collection<Pane> targetPanesForUser = getTargetPanesForUser(player);

    for (Pane pane : targetPanesForUser) {
      ChatUserItemController chatUserItemController = createChatUserControlForPlayerIfNecessary(pane, player);

      // Apply filter if exists
      if (!userSearchTextField.textProperty().get().isEmpty()) {
        chatUserItemController.setVisible(isUsernameMatch(chatUserItemController));
      }
      if (filterUserPopup.isShowing()) {
        userFilterController.filterUser(chatUserItemController);
      }
    }
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

    Map<Pane, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls.get(username);
    if (paneToChatUserControlMap == null) {
      return;
    }

    Platform.runLater(() -> {
      synchronized (paneToChatUserControlMap) {
        for (Map.Entry<Pane, ChatUserItemController> entry : paneToChatUserControlMap.entrySet()) {
          entry.getKey().getChildren().remove(entry.getValue().getRoot());
        }
        paneToChatUserControlMap.clear();
      }
    });
    userToChatUserControls.remove(username);
  }

  private ChatUserItemController addToPane(Player player, Pane pane) {
    return createChatUserControlForPlayerIfNecessary(pane, player);
  }

  private void removeFromPane(Player player, Pane pane) {
    Map<Pane, ChatUserItemController> paneChatUserControlMap = userToChatUserControls.get(player.getUsername());
    if (paneChatUserControlMap == null) {
      // User has not yet been added to this pane; no need to remove him
      return;
    }
    synchronized (paneChatUserControlMap) {
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
      if (!userToChatUserControls.containsKey(username)) {
        userToChatUserControls.put(username, new HashMap<>(1, 1));
      }
    }

    Map<Pane, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls.get(username);

    ChatUserItemController existingChatUserItemController = paneToChatUserControlMap.get(pane);
    if (existingChatUserItemController != null) {
      return existingChatUserItemController;
    }

    ChatUserItemController chatUserItemController = uiService.loadFxml("theme/chat/chat_user_item.fxml");
    chatUserItemController.setPlayer(player);
    paneToChatUserControlMap.put(pane, chatUserItemController);

    chatUserItemController.setColorsAllowedInPane((pane == othersPane || pane == chatOnlyPane) && player.getSocialStatus() != SELF);

    Platform.runLater(() -> {
      addChatUserItemSorted(pane, chatUserItemController);
      isUsernameMatch(chatUserItemController);
    });

    return chatUserItemController;
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

  public void addSearchFieldListener() {
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.trim().isEmpty()) {
        getJsObject().call("removeHighlight");
      } else {
        getJsObject().call("highlightText", newValue);
      }
    });
  }

  public void onAdvancedUserFilter(ActionEvent actionEvent) {
    if (filterUserPopup.isShowing()) {
      filterUserPopup.hide();
      return;
    }

    Button button = (Button) actionEvent.getSource();

    Bounds screenBounds = advancedUserFilter.localToScreen(advancedUserFilter.getBoundsInLocal());
    filterUserPopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }
}
