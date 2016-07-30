package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ProgrammingError;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

public class ChatController {

  private final Map<String, AbstractChatTabController> nameToChatTabController;

  @Resource
  ChatService chatService;
  @Resource
  ApplicationContext applicationContext;
  @Resource
  UserService userService;

  @FXML
  Node chatRoot;
  @FXML
  TabPane chatsTabPane;
  @FXML
  Pane connectingProgressPane;
  @FXML
  VBox noOpenTabsContainer;
  @FXML
  TextField channelNameTextField;

  public ChatController() {
    nameToChatTabController = new HashMap<>();
  }

  @PostConstruct
  void postConstruct() {
    chatService.addOnMessageListener(this::onChannelMessage);
    chatService.addOnPrivateChatMessageListener(this::onPrivateMessage);
    chatService.addChannelsListener(change -> {
      if (change.wasRemoved()) {
        onChannelLeft(change.getValueRemoved());
      }
      if (change.wasAdded()) {
        onChannelJoined(change.getValueAdded());
      }
    });
    chatService.setOnOpenPrivateChatListener(this::openPrivateMessageTabForUser);

    chatService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED:
          onDisconnected();
          break;
        case CONNECTED:
          onConnected();
          break;
        case CONNECTING:
          onConnecting();
          break;
        default:
          throw new ProgrammingError("Uncovered connection state: " + newValue);
      }
    });

    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        onLoggedOut();
      }
    });
  }

  private void onChannelLeft(Channel channel) {
    removeTab(channel.getName());
  }

  private void onChannelJoined(Channel channel) {
    Platform.runLater(() -> getOrCreateChannelTab(channel.getName()));
  }

  private void onDisconnected() {
    connectingProgressPane.setVisible(true);
    chatsTabPane.setVisible(false);
    noOpenTabsContainer.setVisible(false);
  }

  private void onConnected() {
    connectingProgressPane.setVisible(false);
    chatsTabPane.setVisible(true);
    noOpenTabsContainer.setVisible(false);
  }

  private void onConnecting() {
    connectingProgressPane.setVisible(true);
    chatsTabPane.setVisible(false);
    noOpenTabsContainer.setVisible(false);
  }

  private void onLoggedOut() {
    Platform.runLater(() -> chatsTabPane.getTabs().clear());
  }

  private void removeTab(String playerOrChannelName) {
    nameToChatTabController.remove(playerOrChannelName);
    chatsTabPane.getTabs().remove(nameToChatTabController.remove(playerOrChannelName).getRoot());
  }

  private AbstractChatTabController getOrCreateChannelTab(String channelName) {
    JavaFxUtil.assertApplicationThread();

    if (!nameToChatTabController.containsKey(channelName)) {
      ChannelTabController tab = applicationContext.getBean(ChannelTabController.class);
      tab.setChannelName(channelName);
      addTab(channelName, tab);
    }
    return nameToChatTabController.get(channelName);
  }

  private void addTab(String playerOrChannelName, AbstractChatTabController tabController) {
    nameToChatTabController.put(playerOrChannelName, tabController);
    chatsTabPane.getTabs().add(tabController.getRoot());
  }

  @FXML
  void initialize() {
    onDisconnected();

    chatsTabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
      while (change.next()) {
        change.getRemoved().forEach(tab -> nameToChatTabController.remove(tab.getId()));
      }
    });

    chatsTabPane.getTabs().addListener((InvalidationListener) observable ->
        noOpenTabsContainer.setVisible(chatsTabPane.getTabs().isEmpty()));
  }

  private void onChannelMessage(ChatMessage chatMessage) {
    Platform.runLater(() -> getOrCreateChannelTab(chatMessage.getSource()).onChatMessage(chatMessage));
  }

  private void onPrivateMessage(ChatMessage chatMessage) {
    JavaFxUtil.assertBackgroundThread();
    Platform.runLater(() -> addAndGetPrivateMessageTab(chatMessage.getSource()).onChatMessage(chatMessage));
  }

  private AbstractChatTabController addAndGetPrivateMessageTab(String username) {
    JavaFxUtil.assertApplicationThread();

    if (!nameToChatTabController.containsKey(username)) {
      PrivateChatTabController tab = applicationContext.getBean(PrivateChatTabController.class);
      tab.setReceiver(username);
      addTab(username, tab);
    }

    return nameToChatTabController.get(username);
  }

  public Node getRoot() {
    return chatRoot;
  }

  public void openPrivateMessageTabForUser(String username) {
    if (username.equalsIgnoreCase(userService.getUsername())) {
      return;
    }
    AbstractChatTabController controller = addAndGetPrivateMessageTab(username);
    chatsTabPane.getSelectionModel().select(controller.getRoot());
  }

  @FXML
  void onJoinChannelButtonClicked() {
    String channelName = channelNameTextField.getText();
    channelNameTextField.clear();

    joinChannel(channelName);
  }

  private void joinChannel(String channelName) {
    chatService.addUsersListener(channelName, change -> {
      if (change.wasRemoved()) {
        onChatUserLeftChannel(channelName, change.getValueRemoved().getUsername());
      }
      if (change.wasAdded()) {
        onUserJoinedChannel(channelName, change.getValueAdded());
      }
    });
    chatService.joinChannel(channelName);
  }

  private void onChatUserLeftChannel(String channelName, String username) {
    if (!username.equalsIgnoreCase(userService.getUsername())) {
      return;
    }
    AbstractChatTabController chatTab = nameToChatTabController.get(channelName);
    if (chatTab != null) {
      chatsTabPane.getTabs().remove(chatTab.getRoot());
    }
  }

  private void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    Platform.runLater(() -> {
      if (isCurrentUser(chatUser)) {
        onConnected();
      }
    });
  }

  private boolean isCurrentUser(ChatUser chatUser) {
    return chatUser.getUsername().equalsIgnoreCase(userService.getUsername());
  }
}
