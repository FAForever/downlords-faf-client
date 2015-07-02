package com.faforever.client.chat;

import com.faforever.client.legacy.OnJoinChannelsRequestListener;
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
import java.util.List;
import java.util.Map;

public class ChatController implements
    OnChatMessageListener,
    OnChatDisconnectedListener,
    OnPrivateChatMessageListener,
    OnChatUserJoinedChannelListener,
    OnChatUserLeftChannelListener,
    OnJoinChannelsRequestListener {

  @Autowired
  ChatService chatService;

  @Autowired
  ChatTabFactory chatTabFactory;

  @Autowired
  UserService userService;

  @FXML
  Node chatRoot;

  @FXML
  TabPane chatsTabPane;

  @FXML
  Pane connectingProgressPane;

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
    chatService.addOnJoinChannelsRequestListener(this);
  }

  @FXML
  private void initialize() {
    onDisconnected(null);
  }

  @Override
  public void onMessage(String channelName, ChatMessage chatMessage) {
    Platform.runLater(() -> {
      addAndGetChannelTab(channelName).onChatMessage(chatMessage);
    });
  }

  private AbstractChatTab addAndGetChannelTab(String playerOrChannelName) {
    JavaFxUtil.assertApplicationThread();

    if (!nameToChatTab.containsKey(playerOrChannelName)) {
      AbstractChatTab tab = chatTabFactory.createChannelTab(playerOrChannelName);
      addTab(playerOrChannelName, tab);
    }
    return nameToChatTab.get(playerOrChannelName);
  }

  private AbstractChatTab addAndGetPrivateMessageTab(String username) {
    JavaFxUtil.assertApplicationThread();

    if (!nameToChatTab.containsKey(username)) {
      AbstractChatTab tab = chatTabFactory.createPrivateMessageTab(username);
      addTab(username, tab);
    }

    return nameToChatTab.get(username);
  }

  private void addTab(String playerOrChannelName, AbstractChatTab tab) {
    nameToChatTab.put(playerOrChannelName, tab);
    tab.setOnClosed(event -> nameToChatTab.remove(playerOrChannelName));

    if (chatService.isDefaultChannel(tab.getId())) {
      chatsTabPane.getTabs().add(0, tab);
    } else {
      chatsTabPane.getTabs().add(tab);
    }

    chatsTabPane.getSelectionModel().select(0);
  }

  @Override
  public void onDisconnected(Exception e) {
    connectingProgressPane.setVisible(true);
    chatsTabPane.setVisible(false);
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

  @Override
  public void onJoinChannelsRequest(List<String> channelNames) {
    Platform.runLater(() -> channelNames.forEach(this::addAndGetChannelTab));
  }
}
