package com.faforever.client.chat;

import com.faforever.client.exception.ProgrammingError;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.theme.UiService;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.collections.WeakMapChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ChatController extends NodeController<AnchorPane> {

  private final ChatService chatService;
  private final UiService uiService;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  private final Map<ChatChannel, AbstractChatTabController> channelToChatTabController = new HashMap<>();
  private final ListChangeListener<Tab> tabListChangeListener = change -> {
    while (change.next()) {
      change.getRemoved().forEach(tab -> channelToChatTabController.remove((ChatChannel) tab.getUserData()));
    }
  };
  private final MapChangeListener<String, ChatChannel> channelChangeListener = change -> {
    if (change.wasRemoved()) {
      onChannelLeft(change.getValueRemoved());
    }
    if (change.wasAdded()) {
      onChannelJoined(change.getValueAdded());
    }
  };

  public AnchorPane chatRoot;
  public TabPane tabPane;
  public Pane connectingProgressPane;
  public VBox noOpenTabsContainer;
  public TextField channelNameTextField;

  @Override
  protected void onInitialize() {
    super.onInitialize();

    chatService.addChannelsListener(new WeakMapChangeListener<>(channelChangeListener));
    chatService.getChannels().forEach(this::onChannelJoined);

    chatService.connectionStateProperty().when(showing).subscribe(this::onConnectionStateChange);

    JavaFxUtil.addListener(tabPane.getTabs(), new WeakListChangeListener<>(tabListChangeListener));
  }

  private void onChannelLeft(ChatChannel chatChannel) {
    if (chatChannel.isPartyChannel()) {
      return;
    }

    removeTab(chatChannel);
  }

  private void onChannelJoined(ChatChannel chatChannel) {
    if (chatChannel.isPartyChannel()) {
      return;
    }

    addAndSelectTab(chatChannel);
  }

  private void onDisconnected() {
    fxApplicationThreadExecutor.execute(() -> {
      connectingProgressPane.setVisible(true);
      tabPane.setVisible(false);
      tabPane.getTabs().removeIf(Tab::isClosable);
    });
  }

  private void onConnected() {
    chatService.getChannels().forEach(this::onChannelJoined);
    fxApplicationThreadExecutor.execute(() -> {
      connectingProgressPane.setVisible(false);
      tabPane.setVisible(true);
    });
  }

  private void onConnecting() {
    fxApplicationThreadExecutor.execute(() -> {
      connectingProgressPane.setVisible(true);
      tabPane.setVisible(false);
    });
  }

  private void removeTab(ChatChannel chatChannel) {
    AbstractChatTabController controller = channelToChatTabController.remove(chatChannel);
    if (controller != null) {
      fxApplicationThreadExecutor.execute(() -> tabPane.getTabs().remove(controller.getRoot()));
    }
  }

  private void addAndSelectTab(ChatChannel chatChannel) {
    if (!channelToChatTabController.containsKey(chatChannel)) {
      fxApplicationThreadExecutor.execute(() -> {
        AbstractChatTabController tabController;
        if (chatChannel.isPrivateChannel()) {
          tabController = uiService.loadFxml("theme/chat/private_chat_tab.fxml");
        } else {
          tabController = uiService.loadFxml("theme/chat/channel_tab.fxml");
        }
        tabController.setChatChannel(chatChannel);
        channelToChatTabController.put(chatChannel, tabController);
        Tab tab = tabController.getRoot();
        tab.setUserData(chatChannel);


        if (chatService.isDefaultChannel(chatChannel)) {
          tabPane.getTabs().add(0, tab);
          tabPane.getSelectionModel().select(tab);
          tabController.onDisplay();
        } else {
          tabPane.getTabs().add(tabPane.getTabs().size() - 1, tab);

          if (chatChannel.isPrivateChannel() || tabPane.getSelectionModel().getSelectedIndex() == tabPane.getTabs()
                                                                                                         .size() - 1) {
            tabPane.getSelectionModel().select(tab);
            tabController.onDisplay();
          }
        }
      });
    }
  }

  private void onConnectionStateChange(ConnectionState newValue) {
    switch (newValue) {
      case DISCONNECTED -> onDisconnected();
      case CONNECTED -> onConnected();
      case CONNECTING -> onConnecting();
      default -> throw new ProgrammingError("Uncovered connection state: " + newValue);
    }
  }

  @Override
  public AnchorPane getRoot() {
    return chatRoot;
  }

  public void onJoinChannelButtonClicked() {
    String channelName = channelNameTextField.getText();
    if (!channelName.startsWith("#")) {
      channelName = "#" + channelName;
    }

    chatService.joinChannel(channelName);
    channelNameTextField.clear();
  }

  @Override
  protected void onNavigate(NavigateEvent navigateEvent) {
    if (tabPane.getTabs().size() > 1) {
      Tab tab = tabPane.getSelectionModel().getSelectedItem();
      Optional.ofNullable(channelToChatTabController.get((ChatChannel) tab.getUserData()))
              .ifPresent(AbstractChatTabController::onDisplay);
    }
  }
}
