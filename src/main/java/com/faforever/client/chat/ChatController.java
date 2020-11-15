package com.faforever.client.chat;

import com.faforever.client.chat.event.ChatMessageEvent;
import com.faforever.client.fx.AbstractViewController;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.main.event.JoinChannelEvent;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.user.event.LoggedOutEvent;
import com.faforever.client.util.ProgrammingError;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatController extends AbstractViewController<Node> {

  private final Map<String, AbstractChatTabController> nameToChatTabController;
  private final ChatService chatService;
  private final UiService uiService;
  private final UserService userService;
  private final NotificationService notificationService;
  private final EventBus eventBus;
  public Node chatRoot;
  public TabPane tabPane;
  public Pane connectingProgressPane;
  public VBox noOpenTabsContainer;
  public TextField channelNameTextField;

  public ChatController(ChatService chatService, UiService uiService, UserService userService, NotificationService notificationService, EventBus eventBus) {
    this.chatService = chatService;
    this.uiService = uiService;
    this.userService = userService;
    this.notificationService = notificationService;
    this.eventBus = eventBus;

    nameToChatTabController = new HashMap<>();
  }

  private void onChannelLeft(Channel channel) {
    Platform.runLater(() -> removeTab(channel.getName()));
  }

  private void onChannelJoined(Channel channel) {
    String channelName = channel.getName();
    chatService.addUsersListener(channelName, change -> {
      if (change.wasRemoved()) {
        onChatUserLeftChannel(change.getValueRemoved(), channelName);
      }
      if (change.wasAdded()) {
        onUserJoinedChannel(change.getValueAdded(), channelName);
      }
    });
  }

  private void onDisconnected() {
    Platform.runLater(() -> {
      connectingProgressPane.setVisible(true);
      tabPane.setVisible(false);
    });
  }

  private void onConnected() {
    Platform.runLater(() -> {
      connectingProgressPane.setVisible(false);
      tabPane.setVisible(true);
    });
  }

  private void onConnecting() {
    Platform.runLater(() -> {
      connectingProgressPane.setVisible(true);
      tabPane.setVisible(false);
    });
  }

  private void onLoggedOut() {
    Platform.runLater(() -> tabPane.getTabs().clear());
  }

  private void removeTab(String playerOrChannelName) {
    AbstractChatTabController controller = nameToChatTabController.get(playerOrChannelName);
    if (controller != null) {
      tabPane.getTabs().remove(controller.getRoot());
    }
  }

  private AbstractChatTabController getOrCreateChannelTab(String channelName) {
    JavaFxUtil.assertApplicationThread();
    if (!nameToChatTabController.containsKey(channelName)) {
      ChannelTabController tab = uiService.loadFxml("theme/chat/channel_tab.fxml");
      tab.setChannel(chatService.getOrCreateChannel(channelName));
      addTab(channelName, tab);
    }
    return nameToChatTabController.get(channelName);
  }

  private void addTab(String playerOrChannelName, AbstractChatTabController tabController) {
    JavaFxUtil.assertApplicationThread();
    nameToChatTabController.put(playerOrChannelName, tabController);
    Tab tab = tabController.getRoot();

    if (chatService.isDefaultChannel(playerOrChannelName)) {
      tabPane.getTabs().add(0, tab);
    } else {
      tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);
    }
    tabPane.getSelectionModel().select(tab);
    nameToChatTabController.get(tab.getId()).onDisplay();
  }

  @Override
  public void initialize() {
    super.initialize();
    eventBus.register(this);

    chatService.addChannelsListener(change -> {
      if (change.wasRemoved() && !change.getValueRemoved().getName().endsWith("'sParty")) {
        onChannelLeft(change.getValueRemoved());
      }
      if (change.wasAdded() && !change.getValueAdded().getName().endsWith("'sParty")) {
        onChannelJoined(change.getValueAdded());
      }
    });

    JavaFxUtil.addListener(chatService.connectionStateProperty(), (observable, oldValue, newValue) -> onConnectionStateChange(newValue));
    onConnectionStateChange(chatService.connectionStateProperty().get());

    JavaFxUtil.addListener(tabPane.getTabs(), (ListChangeListener<Tab>) change -> {
      while (change.next()) {
        change.getRemoved().forEach(tab -> nameToChatTabController.remove(tab.getId()));
      }
    });
  }

  @Subscribe
  public void onLoggedOutEvent(LoggedOutEvent event) {
    onLoggedOut();
  }

  private void onConnectionStateChange(ConnectionState newValue) {
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
  }

  @Subscribe
  public void onChatMessage(ChatMessageEvent event) {
    Platform.runLater(() -> {
      ChatMessage message = event.getMessage();
      if (message.getSource().endsWith("'sParty"))
        return;
      if (!message.isPrivate()) {
        getOrCreateChannelTab(message.getSource()).onChatMessage(message);
      } else {
        addAndGetPrivateMessageTab(message.getSource()).onChatMessage(message);
      }
    });
  }

  private AbstractChatTabController addAndGetPrivateMessageTab(String username) {
    JavaFxUtil.assertApplicationThread();
    if (!nameToChatTabController.containsKey(username)) {
      PrivateChatTabController tab = uiService.loadFxml("theme/chat/private_chat_tab.fxml");
      tab.setReceiver(username);
      addTab(username, tab);
    }

    return nameToChatTabController.get(username);
  }

  public Node getRoot() {
    return chatRoot;
  }

  @Subscribe
  public void onInitiatePrivateChatEvent(InitiatePrivateChatEvent event) {
    Platform.runLater(() -> openPrivateMessageTabForUser(event.getUsername()));
  }

  private void openPrivateMessageTabForUser(String username) {
    if (username.equalsIgnoreCase(userService.getUsername())) {
      return;
    }
    AbstractChatTabController controller = addAndGetPrivateMessageTab(username);
    Tab tab = controller.getRoot();
    tabPane.getSelectionModel().select(tab);
    nameToChatTabController.get(tab.getId()).onDisplay();
  }

  public void onJoinChannelButtonClicked() {
    String channelName = channelNameTextField.getText();
    channelNameTextField.clear();
    if (!channelName.startsWith("#")) {
      log.info("Channel name {} does not start with #", channelName);
      notificationService.addImmediateErrorNotification(new IllegalArgumentException(), "chat.error.noHashTag", channelName);
      return;
    }

    joinChannel(channelName);
  }

  private void joinChannel(String channelName) {
    chatService.joinChannel(channelName);
  }

  private void onChatUserLeftChannel(ChatChannelUser chatUser, String channelName) {
    if (isCurrentUser(chatUser)) {
      AbstractChatTabController chatTab = nameToChatTabController.get(channelName);
      if (chatTab != null) {
        Platform.runLater(() -> tabPane.getTabs().remove(chatTab.getRoot()));
      }
    }
  }

  private void onUserJoinedChannel(ChatChannelUser chatUser, String channelName) {
    if (isCurrentUser(chatUser)) {
      Platform.runLater(() -> {
        AbstractChatTabController tabController = getOrCreateChannelTab(channelName);
        onConnected();
        if (channelName.equals(chatService.getDefaultChannelName())) {
          Tab tab = tabController.getRoot();
          tabPane.getSelectionModel().select(tab);
          nameToChatTabController.get(tab.getId()).onDisplay();
        }
      });
    }
  }

  private boolean isCurrentUser(ChatChannelUser chatUser) {
    return chatUser.getUsername().equalsIgnoreCase(userService.getUsername());
  }

  @Override
  protected void onDisplay(NavigateEvent navigateEvent) {
    if (navigateEvent instanceof JoinChannelEvent) {
      chatService.joinChannel(((JoinChannelEvent) navigateEvent).getChannel());
      return;
    }
    if (!tabPane.getTabs().isEmpty()) {
      Tab tab = tabPane.getSelectionModel().getSelectedItem();
      Optional.ofNullable(nameToChatTabController.get(tab.getId())).ifPresent(AbstractChatTabController::onDisplay);
    }
  }

  @Override
  public void onHide() {
    super.onHide();
    if (!tabPane.getTabs().isEmpty()) {
      Tab tab = tabPane.getSelectionModel().getSelectedItem();
      Optional.ofNullable(nameToChatTabController.get(tab.getId())).ifPresent(AbstractChatTabController::onHide);
    }
  }
}
