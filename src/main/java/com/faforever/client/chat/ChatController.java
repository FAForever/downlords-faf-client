package com.faforever.client.chat;

import com.faforever.client.legacy.OnJoinChannelsRequestListener;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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

  public ChatController() {
    nameToChatTabController = new HashMap<>();
  }

  @PostConstruct
  void postConstrut() {
    chatService.addOnMessageListener(this);
    chatService.addOnChatDisconnectedListener(this);
    chatService.addOnPrivateChatMessageListener(this);
    chatService.addOnChatUserJoinedChannelListener(this);
    chatService.addOnChatUserLeftChannelListener(this);
    chatService.addOnJoinChannelsRequestListener(this);
    chatService.addOnChatDisconnectedListener(this::onDisconnected);

    userService.addOnLogoutListener(this::onLoggedOut);
  }

  private void onLoggedOut() {
    chatsTabPane.getTabs().clear();
  }

  @FXML
  private void initialize() {
    onDisconnected();
  }

  @Override
  public void onDisconnected() {
    connectingProgressPane.setVisible(true);
    chatsTabPane.setVisible(false);
    nameToChatTabController.clear();
    chatsTabPane.getTabs().removeAll();
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
    tab.setOnClosed(event -> nameToChatTabController.remove(playerOrChannelName));

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

  @Override
  public void onJoinChannelsRequest(List<String> channelNames) {
    channelNames.forEach(chatService::joinChannel);
  }
}
