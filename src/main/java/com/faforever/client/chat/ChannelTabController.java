package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.JavaFxUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
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
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.faforever.client.chat.ChatColorMode.DEFAULT;
import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;

public class ChannelTabController extends AbstractChatTabController {

  @VisibleForTesting
  static final String CSS_CLASS_MODERATOR = "moderator";
  /**
   * Keeps track of which ChatUserControl in which pane belongs to which user.
   */
  private final Map<String, Map<Pane, ChatUserControl>> userToChatUserControls;
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
  ApplicationContext applicationContext;
  @Resource
  I18n i18n;
  private String channelName;
  private Popup filterUserPopup;

  public ChannelTabController() {
    userToChatUserControls = FXCollections.observableMap(new ConcurrentHashMap<>());
  }

  public Map<String, Map<Pane, ChatUserControl>> getUserToChatUserControls() {
    return userToChatUserControls;
  }

  public void setChannelName(String channelName) {
    super.setReceiver(channelName);
    this.channelName = channelName;
    channelTabRoot.setId(channelName);
    channelTabRoot.setText(channelName);

    userSearchTextField.setPromptText(i18n.get("chat.userCount", chatService.getChatUsersForChannel(channelName).size()));
    chatService.getChatUsersForChannel(channelName).addListener((InvalidationListener) change -> {
      Platform.runLater(() -> userSearchTextField.setPromptText(i18n.get("chat.userCount", chatService.getChatUsersForChannel(channelName).size())));
    });

    chatService.addChannelUserListListener(channelName, change -> {
      if (change.wasAdded()) {
        onUserJoinedChannel(change.getValueAdded());
      } else if (change.wasRemoved()) {
        onUserLeft(change.getValueRemoved().getUsername());
      }
    });

    // Maybe there were already elements; fetch them
    ConcurrentUtil.executeInBackground(new Task<Void>() {
      @Override
      protected Void call() throws Exception {
        ObservableMap<String, ChatUser> chatUsersForChannel = chatService.getChatUsersForChannel(channelName);
        synchronized (chatUsersForChannel) {
          chatUsersForChannel.values().forEach(ChannelTabController.this::onUserJoinedChannel);
        }
        return null;
      }
    });

    channelTabRoot.setOnCloseRequest(event -> chatService.leaveChannel(channelName));

    searchFieldContainer.visibleProperty().bind(searchField.visibleProperty());
    closeSearchFieldButton.visibleProperty().bind(searchField.visibleProperty());
    addSearchFieldListener();
  }

  @Override
  public Tab getRoot() {
    return channelTabRoot;
  }

  @Override
  protected TextInputControl getMessageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  protected String getMessageCssClass(String login) {
    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(login);
    if (playerInfoBean != null && playerInfoBean.getModeratorForChannels().contains(channelName)) {
      return CSS_CLASS_MODERATOR;
    }

    return super.getMessageCssClass(login);

  }

  @FXML
  void initialize() {
    userSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      filterChatUserControlsBySearchString();
    });
  }

  /**
   * Hides all chat user controls whose username does not contain the string entered in the {@link
   * #userSearchTextField}.
   */
  private void filterChatUserControlsBySearchString() {
    synchronized (userToChatUserControls) {
      for (Map<Pane, ChatUserControl> chatUserControlMap : userToChatUserControls.values()) {
        for (Map.Entry<Pane, ChatUserControl> chatUserControlEntry : chatUserControlMap.entrySet()) {
          ChatUserControl chatUserControl = chatUserControlEntry.getValue();
          boolean display = isUsernameMatch(chatUserControl);
          chatUserControl.setVisible(display);
          chatUserControl.setManaged(display);
        }
      }
    }
  }

  //TODO: I don't like how this is public
  public boolean isUsernameMatch(ChatUserControl chatUserControl) {
    String lowerCaseSearchString = chatUserControl.getPlayerInfoBean().getUsername().toLowerCase();
    return lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase());
  }

  @PostConstruct
  void init() {
    channelTabScrollPaneVBox.setMinWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    channelTabScrollPaneVBox.setPrefWidth(preferencesService.getPreferences().getChat().getChannelTabScrollPaneWidth());
    addChatColorListener();
    addUserFilterPopup();
  }

  private void addChatColorListener() {
    preferencesService.getPreferences().getChat().chatColorModeProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != DEFAULT) {
        setAllMessageColors();
      } else {
        removeAllMessageColors();
      }
    });
  }

  private void addUserFilterPopup() {
    filterUserPopup = new Popup();
    filterUserPopup.setAutoFix(false);
    filterUserPopup.setAutoHide(true);
    filterUserPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);
    filterUserPopup.getContent().setAll(filterUserController.getRoot());
    filterUserController.setChannelController(this);
  }

  private void setAllMessageColors() {
    ObservableMap<String, ChatUser> chatUsersForChannel = chatService.getChatUsersForChannel(channelName);
    Map<String, String> userToColor = new HashMap<>();
    chatUsersForChannel.values().stream().filter(chatUser -> chatUser.getColor() != null).forEach(chatUser -> {
      userToColor.put(chatUser.getUsername(), JavaFxUtil.toRgbCode(chatUser.getColor()));
    });
    getJsObject().call("setAllMessageColors", new Gson().toJson(userToColor));
  }

  private void removeAllMessageColors() {
    getJsObject().call("removeAllMessageColors");
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
    Platform.runLater(() -> getJsObject().call("removeUserMessageClass", String.format("user-%s", playerInfoBean.getUsername()), cssClass));

  }
  private void setUserMessageClass(PlayerInfoBean playerInfoBean, String cssClass) {
    Platform.runLater(() -> getJsObject().call("setUserMessageClass", String.format("user-%s", playerInfoBean.getUsername()), cssClass));
  }

  private void updateUserMessageDisplay(PlayerInfoBean playerInfoBean, String display) {
    Platform.runLater(() -> getJsObject().call("updateUserMessageDisplay", String.format("user-%s", playerInfoBean.getUsername()), display));
  }

  private void onUserJoinedChannel(ChatUser chatUser) {
    JavaFxUtil.assertBackgroundThread();

    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();

    String username = chatUser.getUsername();
    PlayerInfoBean playerInfoBean = playerService.registerAndGetPlayerForUsername(username);

    playerInfoBean.moderatorForChannelsProperty().bind(chatUser.moderatorInChannelsProperty());
    playerInfoBean.usernameProperty().bind(chatUser.usernameProperty());
    playerInfoBean.usernameProperty().addListener((observable, oldValue, newValue) -> {
      for (Map.Entry<Pane, ChatUserControl> entry : userToChatUserControls.get(oldValue).entrySet()) {
        Pane pane = entry.getKey();
        ChatUserControl chatUserControl = entry.getValue();

        pane.getChildren().remove(chatUserControl);
        addChatUserControlSorted(pane, chatUserControl);
      }
    });

    playerInfoBean.socialStatusProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == OTHER && playerInfoBean.isChatOnly()) {
        addToPane(playerInfoBean, chatOnlyPane);
        setUserMessageClass(playerInfoBean, CSS_CLASS_CHAT_ONLY);
      } else {
        addToPane(playerInfoBean, getPaneForSocialStatus(newValue));
        setUserMessageClass(playerInfoBean, newValue.getCssClass());
      }

      if (chatPrefs.getHideFoeMessages() && newValue == FOE) {
        updateUserMessageDisplay(playerInfoBean, "none");
      }

      if (oldValue == OTHER && playerInfoBean.isChatOnly()) {
        removeFromPane(playerInfoBean, chatOnlyPane);
        removeUserMessageClass(playerInfoBean, CSS_CLASS_CHAT_ONLY);
      } else {
        removeFromPane(playerInfoBean, getPaneForSocialStatus(oldValue));
        removeUserMessageClass(playerInfoBean, oldValue.getCssClass());
      }

      if (chatPrefs.getHideFoeMessages() && oldValue == FOE) {
        updateUserMessageDisplay(playerInfoBean, "");
      }
    });

    playerInfoBean.chatOnlyProperty().addListener((observable, oldValue, newValue) -> {
      if (playerInfoBean.getSocialStatus() == OTHER && !chatUser.getModeratorInChannels().contains(username)) {
        if (newValue) {
          removeFromPane(playerInfoBean, othersPane);
          addToPane(playerInfoBean, chatOnlyPane);
          setUserMessageClass(playerInfoBean, CSS_CLASS_CHAT_ONLY);
        } else {
          removeFromPane(playerInfoBean, chatOnlyPane);
          addToPane(playerInfoBean, getPaneForSocialStatus(playerInfoBean.getSocialStatus()));
          removeUserMessageClass(playerInfoBean, CSS_CLASS_CHAT_ONLY);
        }
      }
    });

    playerInfoBean.getModeratorForChannels().addListener((SetChangeListener<String>) change -> {
      if (change.wasAdded()) {
        addToPane(playerInfoBean, moderatorsPane);
        removeFromPane(playerInfoBean, othersPane);
        removeFromPane(playerInfoBean, chatOnlyPane);
        setUserMessageClass(playerInfoBean, CSS_CLASS_MODERATOR);

      } else {
        removeFromPane(playerInfoBean, moderatorsPane);
        SocialStatus socialStatus = playerInfoBean.getSocialStatus();
        if (socialStatus == OTHER || socialStatus == SELF) {
          addToPane(playerInfoBean, othersPane);
        }
        removeUserMessageClass(playerInfoBean, CSS_CLASS_MODERATOR);
      }
    });

    chatPrefs.hideFoeMessagesProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue && playerInfoBean.getSocialStatus() == FOE) {
        updateUserMessageDisplay(playerInfoBean, "none");
      } else {
        updateUserMessageDisplay(playerInfoBean, "");
      }
    });

    chatUser.colorProperty().addListener((observable, oldValue, newValue) -> {
      Platform.runLater(() -> updateUserMessageColor(chatUser));
    });

    Collection<Pane> targetPanesForUser = getTargetPanesForUser(playerInfoBean);
    userToChatUserControls.putIfAbsent(username, new HashMap<>(targetPanesForUser.size(), 1));

    for (Pane pane : targetPanesForUser) {
      createChatUserControlForPlayerIfNecessary(pane, playerInfoBean);
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

    Map<Pane, ChatUserControl> paneToChatUserControlMap = userToChatUserControls.get(username);
    if (paneToChatUserControlMap == null) {
      return;
    }

    for (Map.Entry<Pane, ChatUserControl> entry : paneToChatUserControlMap.entrySet()) {
      Platform.runLater(() -> entry.getKey().getChildren().remove(entry.getValue()));
    }
    paneToChatUserControlMap.clear();
    userToChatUserControls.remove(username);
  }

  private ChatUserControl addToPane(PlayerInfoBean playerInfoBean, Pane pane) {
    return createChatUserControlForPlayerIfNecessary(pane, playerInfoBean);
  }

  private void removeFromPane(PlayerInfoBean playerInfoBean, Pane pane) {
    // Re-add Plateform.runLater() as soon as RT-40417 is fixed
//        Platform.runLater(() -> {
    Map<Pane, ChatUserControl> paneChatUserControlMap = userToChatUserControls.get(playerInfoBean.getUsername());
    if (paneChatUserControlMap == null) {
      // User has not yet been added to this pane; no need to remove him
      return;
    }
    Platform.runLater(() -> {
      ChatUserControl chatUserControl = paneChatUserControlMap.remove(pane);
      pane.getChildren().remove(chatUserControl);
    });
//        });
  }

  /**
   * Creates a {@link com.faforever.client.chat.ChatUserControl} for the given playerInfoBean and adds it to the given
   * pane if there isn't already such a control in this pane. After the control has been added, the user search filter
   * is applied.
   */
  private ChatUserControl createChatUserControlForPlayerIfNecessary(Pane pane, PlayerInfoBean playerInfoBean) {
    String username = playerInfoBean.getUsername();
    if (!userToChatUserControls.containsKey(username)) {
      userToChatUserControls.put(username, new HashMap<>(1, 1));
    }

    Map<Pane, ChatUserControl> paneToChatUserControlMap = userToChatUserControls.get(username);

    ChatUserControl existingChatUserControl = paneToChatUserControlMap.get(pane);
    if (existingChatUserControl != null) {
      return existingChatUserControl;
    }

    ChatUserControl chatUserControl = applicationContext.getBean(ChatUserControl.class);
    chatUserControl.setPlayerInfoBean(playerInfoBean);
    paneToChatUserControlMap.put(pane, chatUserControl);

    chatUserControl.setColorsAllowedInPane((pane == othersPane || pane == chatOnlyPane) && playerInfoBean.getSocialStatus() != SELF);

    Platform.runLater(() -> {
      addChatUserControlSorted(pane, chatUserControl);
      isUsernameMatch(chatUserControl);
    });

    return chatUserControl;
  }

  /**
   * Inserts the given ChatUserControl into the given Pane such that it is correctly sorted alphabetically.
   */
  private void addChatUserControlSorted(Pane pane, ChatUserControl chatUserControl) {
    ObservableList<Node> children = pane.getChildren();

    if (chatUserControl.getPlayerInfoBean().getSocialStatus() == SELF) {
      children.add(0, chatUserControl);
      return;
    }

    for (Node child : children) {
      if (!(child instanceof ChatUserControl)) {
        continue;
      }

      String newUser = chatUserControl.getPlayerInfoBean().getUsername();
      String nextUser = ((ChatUserControl) child).getPlayerInfoBean().getUsername();

      if (nextUser.equals(userService.getUsername())) {
        continue;
      }

      if (newUser.compareToIgnoreCase(nextUser) < 0) {
        children.add(children.indexOf(child), chatUserControl);
        return;
      }
    }

    children.add(chatUserControl);
  }

  private Collection<Pane> getTargetPanesForUser(PlayerInfoBean playerInfoBean) {
    ArrayList<Pane> panes = new ArrayList<>(3);

    if (playerInfoBean.getModeratorForChannels().contains(channelName)) {
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
      onSearchFieldClose(event);
    } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
      searchField.clear();
      searchField.setVisible(!searchField.isVisible());
      searchField.requestFocus();
    }
  }

  @FXML
  void onSearchFieldClose(Event event) {
    searchField.setVisible(false);
    searchField.clear();
  }

  public void addSearchFieldListener() {
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.isEmpty() || newValue.equals(" ")) {
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
