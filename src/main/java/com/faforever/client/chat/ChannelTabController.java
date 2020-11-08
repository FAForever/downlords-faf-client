package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
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
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.faforever.client.fx.PlatformService.URL_REGEX_PATTERN;
import static com.faforever.client.player.SocialStatus.FOE;
import static java.util.Locale.US;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabController extends AbstractChatTabController {
  @VisibleForTesting
  static final String CSS_CLASS_MODERATOR = "moderator";
  private static final String USER_CSS_CLASS_FORMAT = "user-%s";
  private static final Comparator<CategoryOrChatUserListItem> CHAT_USER_ITEM_COMPARATOR = (o1, o2) -> {
    ChatChannelUser left = o1.getUser();
    ChatChannelUser right = o2.getUser();

    Assert.state(left != null, "Only users must be compared");
    Assert.state(right != null, "Only users must be compared");

    if (isSelf(left)) {
      return 1;
    }
    if (isSelf(right)) {
      return -1;
    }
    return right.getUsername().compareToIgnoreCase(left.getUsername());
  };
  @VisibleForTesting
  /** Maps a chat user category to a list of all user items that belong to it. */
  final Map<ChatUserCategory, List<CategoryOrChatUserListItem>> categoriesToUserListItems;
  /** Maps a chat user category to the list items that represent the respective category within the chat user list. */
  private final Map<ChatUserCategory, CategoryOrChatUserListItem> categoriesToCategoryListItems;
  /** Maps usernames to all chat user list items that belong to that user. */
  private final Map<String, List<CategoryOrChatUserListItem>> userNamesToListItems;

  private final FilteredList<CategoryOrChatUserListItem> filteredChatUserList;

  /** The list of chat user (or category) items that backs the chat user list view. */
  private final ObservableList<CategoryOrChatUserListItem> chatUserListItems;

  private final AutoCompletionHelper autoCompletionHelper;
  private final PlatformService platformService;
  public SplitPane splitPane;
  public ToggleButton advancedUserFilter;
  public HBox searchFieldContainer;
  public Button closeSearchFieldButton;
  public TextField searchField;
  public VBox channelTabScrollPaneVBox;
  public Tab channelTabRoot;
  public WebView messagesWebView;
  public TextField userSearchTextField;
  public TextField messageTextField;
  public VBox chatUserListViewBox;
  public VBox topicPane;
  public TextFlow topicText;
  public ToggleButton toggleSidePaneButton;
  private ChatChannel chatChannel;
  private final InvalidationListener channelTopicListener = observable -> JavaFxUtil.runLater(this::updateChannelTopic);
  private Popup filterUserPopup;
  private UserFilterController userFilterController;
  private MapChangeListener<String, ChatChannelUser> usersChangeListener;

  // TODO cut dependencies
  public ChannelTabController(UserService userService, ChatService chatService,
                              PreferencesService preferencesService,
                              PlayerService playerService, AudioService audioService, TimeService timeService,
                              I18n i18n, ImageUploadService imageUploadService,
                              NotificationService notificationService, ReportingService reportingService,
                              UiService uiService, EventBus eventBus,
                              WebViewConfigurer webViewConfigurer,
                              CountryFlagService countryFlagService, PlatformService platformService,
                              ChatUserService chatUserService) {

    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService,
        timeService, i18n, imageUploadService, notificationService, reportingService, uiService,
        eventBus, countryFlagService, chatUserService);
    this.platformService = platformService;

    categoriesToUserListItems = new HashMap<>();
    categoriesToCategoryListItems = new HashMap<>();
    userNamesToListItems = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    chatUserListItems = FXCollections.observableArrayList();
    filteredChatUserList = new FilteredList<>(chatUserListItems);

    autoCompletionHelper = new AutoCompletionHelper(
        currentWord -> userNamesToListItems.keySet().stream()
            .filter(playerName -> playerName.toLowerCase(US).startsWith(currentWord.toLowerCase()))
            .sorted()
            .collect(Collectors.toList())
    );

    List<CategoryOrChatUserListItem> categoryObjects = createCategoryTreeObjects();
    categoryObjects.forEach(categoryItem -> {
      categoriesToCategoryListItems.put(categoryItem.getCategory(), categoryItem);
      categoriesToUserListItems.put(categoryItem.getCategory(), new ArrayList<>());
    });
    chatUserListItems.addAll(categoryObjects);
  }

  private static boolean isSelf(ChatChannelUser chatUser) {
    return chatUser.getPlayer().isPresent() && chatUser.getPlayer().get().getSocialStatus() == SocialStatus.SELF;
  }

  public void setChatChannel(ChatChannel chatChannel) {
    Assert.state(this.chatChannel == null, "Channel has already been set");
    this.chatChannel = chatChannel;

    String channelName = chatChannel.getName();
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
    updateUserCount(chatChannel.getUsers().size());

    chatService.addUsersListener(channelName, usersChangeListener);

    // Maybe there already were some users; fetch them
    chatChannel.getUsers().forEach(this::onUserJoinedChannel);

    channelTabRoot.setOnCloseRequest(event -> {
      chatService.leaveChannel(chatChannel.getName());
      chatService.removeUsersListener(channelName, usersChangeListener);
    });

    searchFieldContainer.visibleProperty().bind(searchField.visibleProperty());
    closeSearchFieldButton.visibleProperty().bind(searchField.visibleProperty());
    addSearchFieldListener();
    topicPane.managedProperty().bind(topicPane.visibleProperty());
    updateChannelTopic();
    JavaFxUtil.addListener(chatChannel.topicProperty(), new WeakInvalidationListener(channelTopicListener));

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    JavaFxUtil.addListener(chatPrefs.hideFoeMessagesProperty(), ((observable, oldValue, newValue) -> {
      if (newValue) {
        chatChannel.getUsers().stream().filter(chatUser -> chatUser.getSocialStatus().stream().anyMatch(socialStatus -> socialStatus == FOE))
            .forEach(chatUser -> updateUserMessageDisplay(chatUser, "none"));
      } else {
        chatChannel.getUsers().stream().filter(chatUser -> chatUser.getSocialStatus().stream().anyMatch(socialStatus -> socialStatus == FOE))
            .forEach(chatUser -> updateUserMessageDisplay(chatUser, ""));
      }
    }));

    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), ((observable, oldValue, newValue) -> chatChannel.getUsers().forEach(this::updateUserMessageColor)));
  }

  private void updateChannelTopic() {
    boolean hasTopic = !Strings.isNullOrEmpty(chatChannel.getTopic());
    topicPane.setVisible(hasTopic);
    topicText.getChildren().clear();
    if (!hasTopic) {
      return;
    }
    String topic = chatChannel.getTopic();
    Arrays.stream(topic.split("\\s"))
        .forEach(word -> {
          if (URL_REGEX_PATTERN.matcher(word).matches()) {
            Hyperlink link = new Hyperlink(word);
            link.setOnAction(event -> platformService.showDocument(word));
            topicText.getChildren().add(link);
          } else {
            topicText.getChildren().add(new Label(word + " "));
          }
        });
  }

  private void updateUserCount(int count) {
    JavaFxUtil.runLater(() -> userSearchTextField.setPromptText(i18n.get("chat.userCount", count)));
  }

  @Override
  public void initialize() {
    super.initialize();

    userSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> userFilterController.filterUsers());

    channelTabScrollPaneVBox.setMinWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    channelTabScrollPaneVBox.setPrefWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    addUserFilterPopup();


    VirtualFlow<CategoryOrChatUserListItem, ChatUserListCell> chatUserFlow = VirtualFlow.createVertical(filteredChatUserList, chatUserListItem -> new ChatUserListCell(chatUserListItem, uiService));
    VirtualizedScrollPane<VirtualFlow<CategoryOrChatUserListItem, ChatUserListCell>> chatUserScrollPane = new VirtualizedScrollPane<>(chatUserFlow);
    VBox.setVgrow(chatUserScrollPane, Priority.ALWAYS);
    chatUserListViewBox.getChildren().add(chatUserScrollPane);

    autoCompletionHelper.bindTo(messageTextField());

    initializeSideToggle();
  }

  private void initializeSideToggle() {
    toggleSidePaneButton.setSelected(preferencesService.getPreferences().getChat().isPlayerListShown());
    JavaFxUtil.bind(channelTabScrollPaneVBox.visibleProperty(), toggleSidePaneButton.selectedProperty());
    JavaFxUtil.bind(channelTabScrollPaneVBox.managedProperty(), channelTabScrollPaneVBox.visibleProperty());
    JavaFxUtil.addListener(toggleSidePaneButton.selectedProperty(), (observable, oldValue, newValue) -> splitPane.setDividerPositions(newValue ? 0.8 : 1));
    JavaFxUtil.addListener(toggleSidePaneButton.selectedProperty(), (observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getChat().setPlayerListShown(newValue);
      preferencesService.storeInBackground();
    });
  }

  @Override
  protected void onClosed(Event event) {
    super.onClosed(event);
  }

  @NotNull
  private List<CategoryOrChatUserListItem> createCategoryTreeObjects() {
    return Arrays.stream(ChatUserCategory.values())
        .map(CategoryOrChatUserListItem::new)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  boolean isUsernameMatch(ChatChannelUser user) {
    String lowerCaseSearchString = user.getUsername().toLowerCase(US);
    return lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase(US));
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

    if (playerService.getPlayerForUsername(chatMessage.getUsername())
        .filter(player -> player.getSocialStatus() == FOE)
        .isPresent()) {
      log.debug("Ignored ping from {}", chatMessage.getUsername());
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
    ChatChannelUser chatUser = chatService.getChatUser(login, chatChannel.getName());
    Optional<Player> currentPlayerOptional = playerService.getCurrentPlayer();

    if (currentPlayerOptional.isPresent()) {
      return "";
    }

    if (chatUser.isModerator()) {
      return CSS_CLASS_MODERATOR;
    }

    return super.getMessageCssClass(login);
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
    String color;
    if (chatUser.getColor().isPresent()) {
      color = JavaFxUtil.toRgbCode(chatUser.getColor().get());
    } else {
      color = "";
    }
    JavaFxUtil.runLater(() -> getJsObject().call("updateUserMessageColor", chatUser.getUsername(), color));
  }

  private void removeUserMessageClass(ChatChannelUser chatUser, String cssClass) {
    //TODO: DOM Exception 12 when cssClass string is empty string, not sure why cause .remove in the js should be able to handle it
    if (cssClass.isEmpty()) {
      return;
    }
    //Workaround for issue #1080 https://github.com/FAForever/downlords-faf-client/issues/1080
    JavaFxUtil.runLater(() -> {
      try {
        engine.executeScript("removeUserMessageClass('" + String.format(USER_CSS_CLASS_FORMAT, chatUser.getUsername()) + "','" + cssClass + "');");
      } catch (Exception ignored) {
        //before with "getJsObject().call..." if the engine was not yet loaded the Exception was ignored and hence I know to the same
        //TODO: only accept calls after the engine loaded the page completely
      }
    });
  }

  private void addUserMessageClass(ChatChannelUser player, String cssClass) {
    JavaFxUtil.runLater(() -> getJsObject().call("addUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, player.getUsername()), cssClass));
  }

  private void updateUserMessageDisplay(ChatChannelUser chatUser, String display) {
    JavaFxUtil.runLater(() -> getJsObject().call("updateUserMessageDisplay", chatUser.getUsername(), display));
  }

  private void associateChatUserWithPlayer(Player player, ChatChannelUser chatUser) {
    chatUserService.associatePlayerToChatUser(chatUser, player);

    updateCssClass(chatUser);
    updateChatUserListItemsForCategories(chatUser);
  }

  private void onUserJoinedChannel(ChatChannelUser chatUser) {
    Optional<Player> playerOptional = playerService.getPlayerForUsername(chatUser.getUsername());
    playerOptional.ifPresentOrElse(player -> associateChatUserWithPlayer(player, chatUser), () -> updateChatUserListItemsForCategories(chatUser));
  }

  /**
   * Adds and removes chat user items from the chat user list depending on the user's categories. For instance, if the
   * user is a moderator, he'll be added to the moderator category (if missing) and if he's no longer a friend, he will
   * be removed from the friends category.
   */
  private void updateChatUserListItemsForCategories(ChatChannelUser chatUser) {
    List<CategoryOrChatUserListItem> userListItems;
    synchronized (userNamesToListItems) {
      userNamesToListItems.computeIfAbsent(chatUser.getUsername(), s -> new ArrayList<>());
      userListItems = userNamesToListItems.get(chatUser.getUsername());
    }
    Set<ChatUserCategory> chatUserCategorySet = chatUser.getChatUserCategories();
    Arrays.stream(ChatUserCategory.values())
        .forEach(category -> {
          List<CategoryOrChatUserListItem> categoryUserList = categoriesToUserListItems.get(category);
          if (chatUserCategorySet.contains(category) && userListItems.stream().noneMatch(categoryUserList::contains)) {
            CategoryOrChatUserListItem userItem = new CategoryOrChatUserListItem(chatUser, category);
            userListItems.add(userItem);
            categoryUserList.add(userItem);
            addToTreeItemSorted(userItem);
          } else if (!chatUserCategorySet.contains(category) && userListItems.stream().anyMatch(categoryUserList::contains)) {
            List<CategoryOrChatUserListItem> itemsToRemove = userListItems.stream().filter(categoryUserList::contains).collect(Collectors.toList());
            userListItems.removeAll(itemsToRemove);
            categoryUserList.removeAll(itemsToRemove);
            JavaFxUtil.runLater(() -> chatUserListItems.removeAll(itemsToRemove));
          }
        });
  }

  private void addToTreeItemSorted(CategoryOrChatUserListItem child) {
    ChatUserCategory category = child.getCategory();
    CategoryOrChatUserListItem parent = categoriesToCategoryListItems.get(category);
    JavaFxUtil.runLater(() -> {
      synchronized (chatUserListItems) {
        for (int index = chatUserListItems.indexOf(parent) + 1; index < chatUserListItems.size(); index++) {
          CategoryOrChatUserListItem otherItem = chatUserListItems.get(index);

          if (otherItem.getCategory() != category || CHAT_USER_ITEM_COMPARATOR.compare(child, otherItem) > 0) {
            chatUserListItems.add(index, child);
            return;
          }
        }
        chatUserListItems.add(child);
      }
    });
  }

  private void updateCssClass(ChatChannelUser chatUser) {
    JavaFxUtil.runLater(() -> {
      if (chatUser.getPlayer().isPresent()) {
        removeUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);
      } else {
        addUserMessageClass(chatUser, CSS_CLASS_CHAT_ONLY);
      }
      if (chatUser.isModerator()) {
        addUserMessageClass(chatUser, CSS_CLASS_MODERATOR);
      } else {
        removeUserMessageClass(chatUser, CSS_CLASS_MODERATOR);
      }
    });
  }

  private void onUserLeft(String username) {
    List<CategoryOrChatUserListItem> listItemsToBeRemoved = userNamesToListItems.remove(username);

    if (listItemsToBeRemoved != null) {
      JavaFxUtil.runLater(() -> chatUserListItems.removeAll(listItemsToBeRemoved));
      Arrays.stream(ChatUserCategory.values())
          .filter(categoriesToUserListItems::containsKey)
          .map(categoriesToUserListItems::get)
          .forEach(categoryOrChatUserListItems -> listItemsToBeRemoved.forEach(categoryOrChatUserListItems::remove));
    }
  }

  // FIXME use this again
//  private void updateRandomColorsAllowed(ChatUserHeader parent, ChatChannelUser chatUser, ChatUserItemController chatUserItemController) {
//    chatUserItemController.setRandomColorAllowed(
//        (parent == othersTreeItem || parent == chatOnlyTreeItem)
//            && chatUser.getPlayer().isPresent()
//            && chatUser.getPlayer().get().getSocialStatus() != SELF
//    );
//  }

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
    ChatChannelUser chatUser = chatService.getChatUser(username, chatChannel.getName());

    Optional<Player> playerOptional = playerService.getPlayerForUsername(username);

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    String color = "";
    String display = "";

    if (chatPrefs.getHideFoeMessages() && playerOptional.isPresent() && playerOptional.get().getSocialStatus() == FOE) {
      display = "display: none;";
    } else {
      if (chatUser.getColor().isPresent()) {
        color = createInlineStyleFromColor(chatUser.getColor().get());
      }
    }

    return String.format("%s%s", color, display);
  }

  void setUserFilter(Predicate<CategoryOrChatUserListItem> predicate) {
    filteredChatUserList.setPredicate(predicate);
  }

  @Subscribe
  public void onChatUserCategoryChange(ChatUserCategoryChangeEvent event) {
    // We could add a listener on chatChannelUser.socialStatusProperty() but this would result in thousands of mostly idle
    // listeners which we're trying to avoid.
    ChatChannelUser chatUser = event.getChatUser();
    if (chatChannel.getUsers().contains(chatUser)) {
      if (chatUser.getSocialStatus().stream().anyMatch(socialStatus -> socialStatus == FOE)) {
        updateUserMessageDisplay(chatUser, "none");
      } else {
        updateUserMessageDisplay(chatUser, "");
      }
      updateCssClass(chatUser);
      updateUserMessageColor(chatUser);
      updateChatUserListItemsForCategories(chatUser);
    }
  }

  @VisibleForTesting
  List<CategoryOrChatUserListItem> getChatUserItemsByCategory(ChatUserCategory category) {
    CategoryOrChatUserListItem categoryItem = categoriesToCategoryListItems.get(category);
    if (categoryItem == null) {
      return Collections.emptyList();
    }
    return filteredChatUserList.stream().filter(item -> item.getUser() != null && item.getCategory() == category).collect(Collectors.toList());
  }

  @VisibleForTesting
  boolean checkUsersAreInList(ChatUserCategory category, String... usernames) {
    List<String> names = Arrays.asList(usernames);
    long foundItems = getChatUserItemsByCategory(category).stream()
        .map(userItem -> userItem.getUser().getUsername()).filter(names::contains).count();
    return foundItems == names.size();
  }
}
