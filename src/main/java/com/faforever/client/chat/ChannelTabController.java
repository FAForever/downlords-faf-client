package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import netscape.javascript.JSObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;

public class ChannelTabController extends AbstractChatTabController {

  @VisibleForTesting
  static final String CSS_CLASS_MODERATOR = "moderator";
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String USER_CSS_CLASS_FORMAT = "user-%s";
  /**
   * Keeps track of which ChatUserControl in which pane belongs to which user.
   */
  private final Map<String, Map<Pane, ChatUserItemController>> userToChatUserControls;
  @FXML
  Button advancedUserFilter;
  @FXML
  HBox searchFieldContainer;
  @FXML
  Button closeSearchFieldButton;
  @FXML
  TextField searchField;
  @FXML
  VBox channelTabScrollPaneVBox;
  @FXML
  TitledPane moderatorsTitlePane;
  @FXML
  TitledPane friendsTitlePane;
  @FXML
  TitledPane othersTitlePane;
  @FXML
  TitledPane chatOnlyTitlePane;
  @FXML
  TitledPane foesTitlePane;
  @FXML
  Tab channelTabRoot;
  @FXML
  WebView messagesWebView;
  @FXML
  Pane moderatorsPane;
  @FXML
  Pane friendsPane;
  @FXML
  Pane foesPane;
  @FXML
  Pane othersPane;
  @FXML
  Pane chatOnlyPane;
  @FXML
  TextField userSearchTextField;
  @FXML
  TextField messageTextField;

  @Resource
  FilterUserController filterUserController;
  @Resource
  ConfigurableApplicationContext applicationContext;
  @Resource
  I18n i18n;
  @Resource
  ThreadPoolExecutor threadPoolExecutor;

  private Channel channel;
  private Popup filterUserPopup;
  private MapChangeListener<String, ChatUser> usersChangeListener;
  private ChangeListener<ChatColorMode> chatColorModeChangeListener;

  public ChannelTabController() {
    userToChatUserControls = FXCollections.observableMap(new ConcurrentHashMap<>());
  }

  // TODO clean this up
  public Map<String, Map<Pane, ChatUserItemController>> getUserToChatUserControls() {
    return userToChatUserControls;
  }

  public void setChannel(Channel channel) {
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
    threadPoolExecutor.execute(() -> {
      channel.getUsers().forEach(ChannelTabController.this::onUserJoinedChannel);
    });

    channelTabRoot.setOnCloseRequest(event -> {
      chatService.leaveChannel(channel.getName());
      chatService.removeUsersListener(channelName, usersChangeListener);
    });

    searchFieldContainer.visibleProperty().bind(searchField.visibleProperty());
    closeSearchFieldButton.visibleProperty().bind(searchField.visibleProperty());
    addSearchFieldListener();

    channel.topicProperty().addListener((observable, oldValue, newValue) -> {
      setTopic(newValue);
    });
  }

  private void updateUserCount(int count) {
    Platform.runLater(() -> userSearchTextField.setPromptText(i18n.get("chat.userCount", count)));
  }

  @FXML
  void initialize() {
    userSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      filterChatUserControlsBySearchString();
    });

    chatColorModeChangeListener = (observable, oldValue, newValue) -> {
      if (newValue != DEFAULT) {
        setAllMessageColors();
      } else {
        removeAllMessageColors();
      }
    };
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

  //TODO: I don't like how this is public
  public boolean isUsernameMatch(ChatUserItemController chatUserItemController) {
    String lowerCaseSearchString = chatUserItemController.getPlayerInfoBean().getUsername().toLowerCase();
    return lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase());
  }

  /**
   * Inserts the given ChatUserControl into the given Pane such that it is correctly sorted alphabetically.
   */
  private void addChatUserItemSorted(Pane pane, ChatUserItemController chatUserItemController) {
    ObservableList<Node> children = pane.getChildren();

    Pane chatUserItemRoot = chatUserItemController.getRoot();
    if (chatUserItemController.getPlayerInfoBean().getSocialStatus() == SELF) {
      children.add(0, chatUserItemRoot);
      return;
    }

    String thisUsername = chatUserItemController.getPlayerInfoBean().getUsername();
    for (Node child : children) {
      String otherUsername = ((ChatUserItemController) child.getUserData()).getPlayerInfoBean().getUsername();

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

  @PostConstruct
  @Override
  void postConstruct() {
    super.postConstruct();

    channelTabScrollPaneVBox.setMinWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    channelTabScrollPaneVBox.setPrefWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    addChatColorListener();
    addUserFilterPopup();
  }

  @Override
  protected TextInputControl getMessageTextField() {
    return messageTextField;
  }

  @Override
  protected void onWebViewLoaded() {
    setTopic(channel.getTopic());
  }

  private void setTopic(String topic) {
    Platform.runLater(() ->
        ((JSObject) getMessagesWebView().getEngine().executeScript("document.getElementById('channel-topic')"))
            .setMember("innerHTML", convertUrlsToHyperlinks(topic))
    );
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  protected void onMention(ChatMessage chatMessage) {
    if (!hasFocus()) {
      audioController.playChatMentionSound();
      showNotificationIfNecessary(chatMessage);
      incrementUnreadMessagesCount(1);
      setUnread(true);
    }
  }

  @Override
  protected String getMessageCssClass(String login) {
    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(login);
    if (playerInfoBean != null
        && !playerInfoBean.equals(playerService.getCurrentPlayer())
        && playerInfoBean.getModeratorForChannels().contains(channel.getName())) {
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
    filterUserPopup.getContent().setAll(filterUserController.getRoot());
    filterUserController.setChannelController(this);
  }

  public void updateUserMessageColor(ChatUser chatUser) {
    String color = "";
    if (chatUser.getColor() != null) {
      color = JavaFxUtil.toRgbCode(chatUser.getColor());
    }
    getJsObject().call("updateUserMessageColor", chatUser.getUsername(), color);
  }

  private void removeUserMessageClass(PlayerInfoBean playerInfoBean, String cssClass) {
    //TODO: DOM Exception 12 when cssClass string is empty string, not sure why cause .remove in the js should be able to handle it
    if (cssClass.isEmpty()) {
      return;
    }
    Platform.runLater(() -> getJsObject().call("removeUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, playerInfoBean.getUsername()), cssClass));

  }

  private void setUserMessageClass(PlayerInfoBean playerInfoBean, String cssClass) {
    Platform.runLater(() -> getJsObject().call("setUserMessageClass", String.format(USER_CSS_CLASS_FORMAT, playerInfoBean.getUsername()), cssClass));
  }

  private void updateUserMessageDisplay(PlayerInfoBean playerInfoBean, String display) {
    Platform.runLater(() -> getJsObject().call("updateUserMessageDisplay", String.format(USER_CSS_CLASS_FORMAT, playerInfoBean.getUsername()), display));
  }

  private void onUserJoinedChannel(ChatUser chatUser) {
    JavaFxUtil.assertBackgroundThread();

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String username = chatUser.getUsername();
    PlayerInfoBean player = playerService.createAndGetPlayerForUsername(username);

    player.moderatorForChannelsProperty().bind(chatUser.moderatorInChannelsProperty());
    player.usernameProperty().addListener((observable, oldValue, newValue) -> {
      for (Map.Entry<Pane, ChatUserItemController> entry : userToChatUserControls.get(oldValue).entrySet()) {
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
    userToChatUserControls.putIfAbsent(username, new HashMap<>(targetPanesForUser.size(), 1));

    for (Pane pane : targetPanesForUser) {
      ChatUserItemController chatUserItemController = createChatUserControlForPlayerIfNecessary(pane, player);

      // Apply filter if exists
      if (!userSearchTextField.textProperty().get().isEmpty()) {
        chatUserItemController.setVisible(isUsernameMatch(chatUserItemController));
      }
      if (filterUserPopup.isShowing()) {
        filterUserController.filterUser(chatUserItemController);
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

    for (Map.Entry<Pane, ChatUserItemController> entry : paneToChatUserControlMap.entrySet()) {
      Platform.runLater(() -> entry.getKey().getChildren().remove(entry.getValue().getRoot()));
    }
    paneToChatUserControlMap.clear();
    userToChatUserControls.remove(username);
  }

  private ChatUserItemController addToPane(PlayerInfoBean playerInfoBean, Pane pane) {
    return createChatUserControlForPlayerIfNecessary(pane, playerInfoBean);
  }

  private void removeFromPane(PlayerInfoBean playerInfoBean, Pane pane) {
    Map<Pane, ChatUserItemController> paneChatUserControlMap = userToChatUserControls.get(playerInfoBean.getUsername());
    if (paneChatUserControlMap == null) {
      // User has not yet been added to this pane; no need to remove him
      return;
    }
    Platform.runLater(() -> {
      ChatUserItemController chatUserItemController = paneChatUserControlMap.remove(pane);
      pane.getChildren().remove(chatUserItemController.getRoot());
    });
  }

  /**
   * Creates a {@link ChatUserItemController} for the given playerInfoBean and adds it to the given pane if there isn't
   * already such a control in this pane. After the control has been added, the user search filter is applied.
   */
  private ChatUserItemController createChatUserControlForPlayerIfNecessary(Pane pane, PlayerInfoBean playerInfoBean) {
    String username = playerInfoBean.getUsername();
    if (!userToChatUserControls.containsKey(username)) {
      userToChatUserControls.put(username, new HashMap<>(1, 1));
    }

    Map<Pane, ChatUserItemController> paneToChatUserControlMap = userToChatUserControls.get(username);

    ChatUserItemController existingChatUserItemController = paneToChatUserControlMap.get(pane);
    if (existingChatUserItemController != null) {
      return existingChatUserItemController;
    }

    if (!applicationContext.isActive()) {
      logger.warn("Application context has been closed, not creating control for player {}", playerInfoBean.getUsername());
    }
    ChatUserItemController chatUserItemController = applicationContext.getBean(ChatUserItemController.class);
    chatUserItemController.setPlayerInfoBean(playerInfoBean);
    paneToChatUserControlMap.put(pane, chatUserItemController);

    chatUserItemController.setColorsAllowedInPane((pane == othersPane || pane == chatOnlyPane) && playerInfoBean.getSocialStatus() != SELF);

    Platform.runLater(() -> {
      addChatUserItemSorted(pane, chatUserItemController);
      isUsernameMatch(chatUserItemController);
    });

    return chatUserItemController;
  }

  private Collection<Pane> getTargetPanesForUser(PlayerInfoBean playerInfoBean) {
    ArrayList<Pane> panes = new ArrayList<>(3);

    if (playerInfoBean.getModeratorForChannels().contains(channel.getName())) {
      panes.add(moderatorsPane);
    }

    Pane pane = getPaneForSocialStatus(playerInfoBean.getSocialStatus());
    if (pane == othersPane && playerInfoBean.isChatOnly()) {
      panes.add(chatOnlyPane);
    } else {
      panes.add(pane);
    }

    return panes;
  }

  @FXML
  void onKeyReleased(KeyEvent event) {
    if (event.getCode() == KeyCode.ESCAPE) {
      onSearchFieldClose();
    } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
      searchField.clear();
      searchField.setVisible(!searchField.isVisible());
      searchField.requestFocus();
    }
  }

  @FXML
  void onSearchFieldClose() {
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

  @FXML
  void onAdvancedUserFilter(ActionEvent actionEvent) {
    if (filterUserPopup.isShowing()) {
      filterUserPopup.hide();
      return;
    }

    Button button = (Button) actionEvent.getSource();

    Bounds screenBounds = advancedUserFilter.localToScreen(advancedUserFilter.getBoundsInLocal());
    filterUserPopup.show(button.getScene().getWindow(), screenBounds.getMinX(), screenBounds.getMaxY());
  }
}
