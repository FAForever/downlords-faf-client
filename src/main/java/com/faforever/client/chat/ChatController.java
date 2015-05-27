package com.faforever.client.chat;

import com.faforever.client.user.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

public class ChatController implements
    OnMessageListener,
    OnDisconnectedListener,
    OnPrivateMessageListener,
    OnChannelJoinedListener {

  @Autowired
  ChatService chatService;

  @Autowired
  ChannelTabFactory channelTabFactory;

  @Autowired
  UserService userService;

  @FXML
  private TabPane chatsTabPane;

  @FXML
  private Pane connectingProgressPane;

  private final Map<String, ChannelTab> nameToChatTab;

  public ChatController() {
    nameToChatTab = new HashMap<>();
  }

  @PostConstruct
  void init() {
    chatService.addOnMessageListener(this);
    chatService.addOnDisconnectedListener(this);
    chatService.addOnPrivateMessageListener(this);
    chatService.addOnChannelJoinedListener(this);
  }

  public void configure() {
    chatService.connect();
  }

  @FXML
  private void initialize() {
    onDisconnected(null);
  }

  @Override
  public void onMessage(String channelName, ChatMessage chatMessage) {
    Platform.runLater(() -> {
      addAndGetChannel(channelName).onMessage(chatMessage);
    });
  }

  private ChannelTab addAndGetChannel(String channelName) {
    if (!nameToChatTab.containsKey(channelName)) {
      ChannelTab channelTab = channelTabFactory.createChannelTab(channelName);
      nameToChatTab.put(channelName, channelTab);
      chatsTabPane.getTabs().add(channelTab);
    }
    return nameToChatTab.get(channelName);
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
    addAndGetChannel(sender).onMessage(chatMessage);
  }
}
