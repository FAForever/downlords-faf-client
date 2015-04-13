package com.faforever.client.chat;

import com.faforever.client.legacy.message.OnPlayerInfoMessageListener;
import com.faforever.client.legacy.message.PlayerInfoMessage;
import com.faforever.client.player.PlayerService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.BeanUpdatePolicy;
import javafx.collections.FXCollections;
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
    OnPlayerInfoMessageListener,
    OnUserLeftListener {

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
  private ObservableMap<String, PlayerInfoBean> playerInfoMap;

  public ChatController() {
    nameToChatTab = new HashMap<>();
    playerInfoMap = FXCollections.observableMap(new HashMap<>());
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
      ChannelTab channelTab = channelTabFactory.createChannelTab(channelName, playerInfoMap);
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
  public void onChannelJoined(String channelName, PlayerInfoBean playerInfoBean) {
    if (isCurrentUser(playerInfoBean)) {
      connectingProgressPane.setVisible(false);
      chatsTabPane.setVisible(true);
    } else {
      addOrUpdatePlayerInfo(playerInfoBean);
    }
  }

  private boolean isCurrentUser(PlayerInfoBean playerInfoBean) {
    return playerInfoBean.getUsername().equals(userService.getUsername());
  }

  @Override
  public void onPrivateMessage(String sender, ChatMessage chatMessage) {
    addAndGetChannel(sender).onMessage(chatMessage);
  }

  @Override
  public void onChatUserList(String channelName, Set<PlayerInfoBean> users) {
    addAndGetChannel(channelName).setPlayerInfoAsync(users);
  }

  @Override
  public void onPlayerInfoMessage(PlayerInfoMessage playerInfoMessage) {
    addOrUpdatePlayerInfo(new PlayerInfoBean(playerInfoMessage));
  }

  private void addOrUpdatePlayerInfo(PlayerInfoBean playerInfoBean) {
    String username = playerInfoBean.getUsername();

    if (!playerInfoMap.containsKey(username)) {
      playerInfoMap.put(username, playerInfoBean);
    } else {
      playerInfoMap.get(username).update(playerInfoBean, BeanUpdatePolicy.OVERRIDE);
    }
  }

  @Override
  public void onUserLeft(String login) {
    for (ChannelTab channelTab : nameToChatTab.values()) {
      channelTab.onUserLeft(login);
    }
  }
}
