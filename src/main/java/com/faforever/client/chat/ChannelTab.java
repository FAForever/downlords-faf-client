package com.faforever.client.chat;

import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.JavaFxUtil;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelTab extends AbstractChatTab implements OnChatUserControlDoubleClickListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

  private final String channelName;

  /**
   * Keeps track of which ChatUserControl in which pane belongs to which user.
   */
  private final Map<String, Map<Pane, ChatUserControl>> userToChatUserControls;

  public ChannelTab(String channelName) {
    super(channelName, "channel_tab.fxml");
    this.channelName = channelName;

    userToChatUserControls = FXCollections.observableMap(new ConcurrentHashMap<>());

    setId(channelName);
    setText(channelName);
  }

  void postConstruct() {
    super.postConstruct();

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
        chatService.getChatUsersForChannel(channelName).values().forEach(ChannelTab.this::onUserJoinedChannel);
        return null;
      }
    });
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }

  @Override
  protected TextInputControl getMessageTextField() {
    return messageTextField;
  }

  @FXML
  void initialize() {
    userSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      filterChatUserControlsBySearchString();
    });

    setOnCloseRequest(new EventHandler<Event>() {
      @Override
      public void handle(Event event) {
        chatService.leaveChannel(channelName);
      }
    });
  }

  private void onUserJoinedChannel(ChatUser chatUser) {
    JavaFxUtil.assertBackgroundThread();

    PlayerInfoBean playerInfoBean = playerService.registerAndGetPlayerForUsername(chatUser.getUsername());

    playerInfoBean.friendProperty().addListener(propertyChangeListenerToDisplayPlayerInPane(playerInfoBean, friendsPane));
    playerInfoBean.foeProperty().addListener(propertyChangeListenerToDisplayPlayerInPane(playerInfoBean, foesPane));
    playerInfoBean.moderatorProperty().addListener(propertyChangeListenerToDisplayPlayerInPane(playerInfoBean, moderatorsPane));
    playerInfoBean.chatOnlyProperty().addListener(propertyChangeListenerToDisplayPlayerInPane(playerInfoBean, moderatorsPane));

    String username = chatUser.getUsername();

    Collection<Pane> targetPanesForUser = getTargetPanesForUser(playerInfoBean);
    userToChatUserControls.putIfAbsent(username, new HashMap<>(targetPanesForUser.size(), 1));

    for (Pane pane : targetPanesForUser) {
      // Remove Plateform.runLater() as soon as RT-40417 is fixed
      Platform.runLater(() -> createChatUserControlForPlayerIfNecessary(pane, playerInfoBean));
    }
  }

  private void onUserLeft(String login) {
    JavaFxUtil.assertBackgroundThread();

    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(login);
    if (playerInfoBean == null) {
      return;
    }

    Map<Pane, ChatUserControl> paneChatUserControlMap = userToChatUserControls.get(playerInfoBean.getUsername());
    if (paneChatUserControlMap == null) {
      return;
    }

    for (Map.Entry<Pane, ChatUserControl> entry : paneChatUserControlMap.entrySet()) {
      Platform.runLater(() -> entry.getKey().getChildren().remove(entry.getValue()));
    }
  }

  @Override
  public void onChatUserControlDoubleClicked(ChatUserControl chatUserControl) {
    String targetUserName = chatUserControl.getPlayerInfoBean().getUsername();
    if (targetUserName.equals(userService.getUsername())) {
      return;
    }

    chatController.openPrivateMessageTabForUser(targetUserName);
  }

  private ChangeListener<Boolean> propertyChangeListenerToDisplayPlayerInPane(PlayerInfoBean playerInfoBean, Pane pane) {
    return (observable, oldValue, newValue) -> {
      if (newValue) {
        createChatUserControlForPlayerIfNecessary(pane, playerInfoBean);
      } else {
        Platform.runLater(() -> {
          Map<Pane, ChatUserControl> paneChatUserControlMap = userToChatUserControls.get(playerInfoBean.getUsername());
          pane.getChildren().remove(paneChatUserControlMap.get(pane));
        });
      }
    };
  }

  /**
   * Creates a {@link com.faforever.client.chat.ChatUserControl} for the given playerInfoBean and adds it to the given
   * pane if there isn't already such a control in this pane. After the control has been added, the user search filter
   * is applied.
   */
  private void createChatUserControlForPlayerIfNecessary(Pane pane, PlayerInfoBean playerInfoBean) {
    Map<Pane, ChatUserControl> paneToChatUserControlMap = userToChatUserControls.get(playerInfoBean.getUsername());

    ChatUserControl existingChatUserControl = paneToChatUserControlMap.get(pane);
    if (existingChatUserControl != null) {
      return;
    }

    ChatUserControl chatUserControl = chatUserControlFactory.createChatUserControl(playerInfoBean, this);
    paneToChatUserControlMap.put(pane, chatUserControl);

    Platform.runLater(() -> {
      addChatUserControlSorted(pane, chatUserControl);
      applyUserSearchFilter(chatUserControl);
    });
  }

  private void applyUserSearchFilter(ChatUserControl chatUserControl) {
    String lowerCaseSearchString = chatUserControl.getPlayerInfoBean().getUsername().toLowerCase();
    boolean display = lowerCaseSearchString.contains(userSearchTextField.getText().toLowerCase());
    chatUserControl.setVisible(display);
    chatUserControl.setManaged(display);
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

          applyUserSearchFilter(chatUserControl);
        }
      }
    }
  }

  /**
   * Inserts the given ChatUserControl into the given Pane such that it is correctly sorted alphabetically.
   */
  private void addChatUserControlSorted(Pane pane, ChatUserControl chatUserControl) {
    ObservableList<Node> children = pane.getChildren();
    for (Node child : children) {
      if (!(child instanceof ChatUserControl)) {
        continue;
      }

      String username1 = chatUserControl.getPlayerInfoBean().getUsername();
      String username2 = ((ChatUserControl) child).getPlayerInfoBean().getUsername();

      if (username1.compareTo(username2) < 0) {
        children.add(children.indexOf(child), chatUserControl);
        return;
      }
    }

    children.add(chatUserControl);
  }

  private Collection<Pane> getTargetPanesForUser(PlayerInfoBean playerInfoBean) {
    ArrayList<Pane> panes = new ArrayList<>(3);

    if (playerInfoBean.isFriend()) {
      panes.add(friendsPane);
    } else if (playerInfoBean.isFoe()) {
      panes.add(foesPane);
    }

    if (playerInfoBean.isModerator()) {
      panes.add(moderatorsPane);
    }

    if (playerInfoBean.isChatOnly()) {
      panes.add(chatOnlyPane);
    }

    if (panes.isEmpty()) {
      panes.add(othersPane);
    }

    return panes;
  }
}
