package com.faforever.client.chat;

import com.faforever.client.user.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChatController implements
    OnMessageListener,
    OnDisconnectedListener,
    OnPrivateMessageListener,
    OnUserJoinedListener,
    OnUserListListener {

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

  public void load() {
    chatService.addOnMessageListener(this);
    chatService.addOnDisconnectedListener(this);
    chatService.addOnPrivateMessageListener(this);
    chatService.addOnUserJoinedListener(this);
    chatService.addOnUserListListener(this);

    chatService.connect();
  }

  @FXML
  private void initialize() {
    onDisconnected(null);
  }

  @Override
  public void onMessage(String channelName, ChatMessage chatMessage) {
    addAndGetChannel(channelName).onMessage(chatMessage);
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
  public void onChannelJoined(String channelName, ChatUser chatUser) {
    ChannelTab channelTab = addAndGetChannel(channelName);

    if (isCurrentUser(chatUser)) {
      connectingProgressPane.setVisible(false);
      chatsTabPane.setVisible(true);
    } else {
      channelTab.onUserJoined(chatUser);
    }
  }

  private boolean isCurrentUser(ChatUser chatUser) {
    return chatUser.getNick().equals(userService.getUsername());
  }

  @Override
  public void onPrivateMessage(String sender, ChatMessage chatMessage) {
    addAndGetChannel(sender).onMessage(chatMessage);
  }

  @Override
  public void onUserList(String channelName, Set<ChatUser> users) {
    addAndGetChannel(channelName).setUsers(users);
  }
}
