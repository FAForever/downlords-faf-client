package com.faforever.client.chat;

import com.faforever.client.legacy.message.OnPlayerInfoMessageListener;
import com.faforever.client.legacy.message.PlayerInfoMessage;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ChatController implements
    OnMessageListener,
    OnDisconnectedListener,
    OnPrivateMessageListener,
    OnUserJoinedListener,
    OnUserListListener,
    OnPlayerInfoMessageListener, OnUserLeftListener {

  @Autowired
  ChatService chatService;

  @Autowired
  ChannelTabFactory channelTabFactory;

  @Autowired
  UserService userService;

  @Autowired
  PlayerService playerService;

  @FXML
  private TabPane chatsTabPane;

  @FXML
  private Pane connectingProgressPane;

  private final Map<String, ChannelTab> nameToChatTab;
  private ObservableMap<String, PlayerInfoMessage> playerInfos;

  public ChatController() {
    nameToChatTab = new HashMap<>();
    playerInfos = new ObservableMapWrapper<>(new HashMap<>());
  }

  @PostConstruct
  void init() {
    chatService.addOnMessageListener(this);
    chatService.addOnDisconnectedListener(this);
    chatService.addOnPrivateMessageListener(this);
    chatService.addOnUserJoinedListener(this);
    chatService.addOnUserListListener(this);
    chatService.addOnUserLeftListener(this);

    playerService.addOnPlayerInfoListener(this);
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
    addAndGetChannel(channelName).onMessage(chatMessage);
  }

  private ChannelTab addAndGetChannel(String channelName) {
    if (!nameToChatTab.containsKey(channelName)) {
      ChannelTab channelTab = channelTabFactory.createChannelTab(channelName, playerInfos);
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
    return chatUser.getLogin().equals(userService.getUsername());
  }

  @Override
  public void onPrivateMessage(String sender, ChatMessage chatMessage) {
    addAndGetChannel(sender).onMessage(chatMessage);
  }

  @Override
  public void onChatUserList(String channelName, Set<ChatUser> users) {
    addAndGetChannel(channelName).setUsersAsync(users);
  }

  @Override
  public void onPlayerInfoMessage(PlayerInfoMessage playerInfoMessage) {
    playerInfos.put(playerInfoMessage.login, playerInfoMessage);
  }

  @Override
  public void onUserLeft(String login) {
    for (ChannelTab channelTab : nameToChatTab.values()) {
      channelTab.onUserLeft(login);
    }
  }
}
