package com.faforever.client.chat;

import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

public class ChatController implements
    OnChatMessageListener,
    OnChatDisconnectedListener,
    OnPrivateChatMessageListener,
    OnChatUserJoinedChannelListener,
    OnChatUserLeftChannelListener {

  @Autowired
  ChatService chatService;

  @Autowired
  ChatTabFactory chatTabFactory;

  @Autowired
  UserService userService;

  @FXML
  private Node chatRoot;

  @FXML
  private TabPane chatsTabPane;

  @FXML
  private Pane connectingProgressPane;

  private final Map<String, AbstractChatTab> nameToChatTab;

  public ChatController() {
    nameToChatTab = new HashMap<>();
  }

  @PostConstruct
  void init() {
    chatService.addOnMessageListener(this);
    chatService.addOnChatDisconnectedListener(this);
    chatService.addOnPrivateChatMessageListener(this);
    chatService.addOnChatUserJoinedChannelListener(this);
    chatService.addOnChatUserLeftChannelListener(this);
  }

  @FXML
  private void initialize() {
    onDisconnected(null);
  }

  @Override
  public void onMessage(String channelName, ChatMessage chatMessage) {
    Platform.runLater(() -> {
      addAndGetChannel(channelName).onChatMessage(chatMessage);
    });
  }

  private AbstractChatTab addAndGetChannel(String channelName) {
    if (!nameToChatTab.containsKey(channelName)) {
      AbstractChatTab tab = chatTabFactory.createChannelTab(channelName);
      nameToChatTab.put(channelName, tab);
      chatsTabPane.getTabs().add(tab);
      tab.setOnClosed(event -> nameToChatTab.remove(channelName));
    }
    return nameToChatTab.get(channelName);
  }

  private AbstractChatTab addAndSelectPrivateMessageTab(String username) {
    if (!nameToChatTab.containsKey(username)) {
      AbstractChatTab tab = chatTabFactory.createPrivateMessageTab(username);
      nameToChatTab.put(username, tab);
      chatsTabPane.getTabs().add(tab);
      tab.setOnClosed(event -> nameToChatTab.remove(username));
    }

    AbstractChatTab tab = nameToChatTab.get(username);
    chatsTabPane.getSelectionModel().select(tab);
    return tab;
  }

  @Override
  public void onDisconnected(Exception e) {
    connectingProgressPane.setVisible(true);
    chatsTabPane.setVisible(false);
  }

  @Override
  public void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    Platform.runLater(() -> {
      addAndGetChannel(channelName);

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
    Platform.runLater(() -> addAndGetChannel(sender).onChatMessage(chatMessage));
  }

  public Node getRoot() {
    return chatRoot;
  }

  public void openPrivateMessageTabForUser(String username) {
    addAndSelectPrivateMessageTab(username);
  }

  @Override
  public void onChatUserLeftChannel(String username, String channelName) {
    if (userService.getUsername().equals(username)) {
      chatsTabPane.getTabs().remove(nameToChatTab.get(channelName));
    }
  }
}
