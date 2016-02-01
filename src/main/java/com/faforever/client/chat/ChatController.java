package com.faforever.client.chat;

import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.user.UserService;
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
import java.util.List;
import java.util.Map;

public class ChatController implements
    OnChatMessageListener,
    OnPrivateChatMessageListener,
    OnChatUserJoinedChannelListener,
    OnChatUserLeftChannelListener {

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
  void postConstrut() {
    chatService.addOnMessageListener(this);
    chatService.addOnPrivateChatMessageListener(this);
    chatService.addOnChatUserJoinedChannelListener(this);
    chatService.addOnChatUserLeftChannelListener(this);
    chatService.addOnJoinChannelsRequestListener(this::onJoinChannelsRequest);

    chatService.connectionStateProperty().addListener((observable, oldValue, newValue) -> {
      switch (newValue) {
        case DISCONNECTED:
          onDisconnected();
          break;
      }
    });

    userService.loggedInProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        onLoggedOut();
      }
    });
  }

  private void onDisconnected() {
    connectingProgressPane.setVisible(true);
    chatsTabPane.setVisible(false);
    chatsTabPane.getTabs().removeAll();
    nameToChatTabController.clear();
    noOpenTabsContainer.setVisible(false);
  }

  private void onLoggedOut() {
    chatsTabPane.getTabs().clear();
  }

  @FXML
  private void initialize() {
    onDisconnected();

    chatsTabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
      while (change.next()) {
        change.getRemoved().forEach(tab -> nameToChatTabController.remove(tab.getId()));
      }
    });

    chatsTabPane.getTabs().addListener((InvalidationListener) observable ->
        noOpenTabsContainer.setVisible(chatsTabPane.getTabs().isEmpty()));
  }

  @Override
  public void onMessage(String channelName, ChatMessage chatMessage) {
    Platform.runLater(() -> addAndGetChannelTab(channelName).onChatMessage(chatMessage));
  }

  private AbstractChatTabController addAndGetChannelTab(String channelName) {
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
    Tab tab = tabController.getRoot();

    if (chatService.isDefaultChannel(tab.getId())) {
      chatsTabPane.getTabs().add(0, tab);
    } else {
      chatsTabPane.getTabs().add(tab);
    }

    chatsTabPane.getSelectionModel().select(0);
  }

  @Override
  public void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    Platform.runLater(() -> {
      addAndGetChannelTab(channelName);

      if (isCurrentUser(chatUser)) {
        connectingProgressPane.setVisible(false);
        chatsTabPane.setVisible(true);
      }
    });
  }

  private boolean isCurrentUser(ChatUser chatUser) {
    return chatUser.getUsername().equals(userService.getUsername());
  }

  @Override
  public void onPrivateMessage(String sender, ChatMessage chatMessage) {
    JavaFxUtil.assertBackgroundThread();
    Platform.runLater(() -> addAndGetPrivateMessageTab(sender).onChatMessage(chatMessage));
  }

  private AbstractChatTabController addAndGetPrivateMessageTab(String username) {
    JavaFxUtil.assertApplicationThread();

    if (!nameToChatTabController.containsKey(username)) {
      PrivateChatTabController tab = applicationContext.getBean(PrivateChatTabController.class);
      tab.setUsername(username);
      addTab(username, tab);
    }

    return nameToChatTabController.get(username);
  }

  public Node getRoot() {
    return chatRoot;
  }

  public void openPrivateMessageTabForUser(String username) {
    if (username.equals(userService.getUsername())) {
      return;
    }
    AbstractChatTabController controller = addAndGetPrivateMessageTab(username);
    chatsTabPane.getSelectionModel().select(controller.getRoot());
  }

  @Override
  public void onChatUserLeftChannel(String username, String channelName) {
    if (userService.getUsername().equals(username)) {
      AbstractChatTabController chatTab = nameToChatTabController.get(channelName);
      if (chatTab != null) {
        chatsTabPane.getTabs().remove(chatTab.getRoot());
      }
    }
  }

  private void onJoinChannelsRequest(List<String> channelNames) {
    channelNames.forEach(chatService::joinChannel);
  }

  @FXML
  void onJoinChannel() {
    chatService.joinChannel(channelNameTextField.getText());
    channelNameTextField.clear();
  }
}
