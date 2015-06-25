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
      addAndGetChatTab(channelName).onChatMessage(chatMessage);
    });
  }

  private AbstractChatTab addAndGetChatTab(String playerOrChanneLName) {
    if (!nameToChatTab.containsKey(playerOrChanneLName)) {
      AbstractChatTab tab = chatTabFactory.createChannelTab(playerOrChanneLName);
      addTab(playerOrChanneLName, tab);
    }
    return nameToChatTab.get(playerOrChanneLName);
  }

  private AbstractChatTab addAndGetPrivateMessageTab(String username) {
    if (!nameToChatTab.containsKey(username)) {
      AbstractChatTab tab = chatTabFactory.createPrivateMessageTab(username);
      addTab(username, tab);
    }

    return nameToChatTab.get(username);
  }

  private void addTab(String playerOrChannelName, AbstractChatTab tab) {
    nameToChatTab.put(playerOrChannelName, tab);
    chatsTabPane.getTabs().add(tab);
    tab.setOnClosed(event -> nameToChatTab.remove(playerOrChannelName));
  }

  @Override
  public void onDisconnected(Exception e) {
    connectingProgressPane.setVisible(true);
    chatsTabPane.setVisible(false);
  }

  @Override
  public void onUserJoinedChannel(String channelName, ChatUser chatUser) {
    Platform.runLater(() -> {
      addAndGetChatTab(channelName);

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

  public Node getRoot() {
    return chatRoot;
  }

  public void openPrivateMessageTabForUser(String username) {
    AbstractChatTab tab = addAndGetPrivateMessageTab(username);
    chatsTabPane.getSelectionModel().select(tab);
  }

  @Override
  public void onChatUserLeftChannel(String username, String channelName) {
    if (userService.getUsername().equals(username)) {
      chatsTabPane.getTabs().remove(nameToChatTab.get(channelName));
    }
  }
}
