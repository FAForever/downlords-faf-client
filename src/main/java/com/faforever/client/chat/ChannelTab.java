package com.faforever.client.chat;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;

import java.io.IOException;
import java.time.Instant;

public class ChannelTab extends Tab {

  private final ChatController chatController;
  @FXML
  private ListView<MessageListItem> chatListView;

  @FXML
  private ListView<MessageListItem> usersListView;

  public ChannelTab(ChatController chatController, String channelName) {
    this.chatController = chatController;

    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/channel_tab.fxml"));
    fxmlLoader.setController(this);
    fxmlLoader.setRoot(this);

    try {
      fxmlLoader.load();
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }

    setClosable(true);
    setId(channelName);
    setText(channelName);
  }

  public void onMessage(Instant instant, String sender, String message) {
    chatListView.getItems().add(new MessageListItem(
        instant, null, sender, message
    ));
  }
}
