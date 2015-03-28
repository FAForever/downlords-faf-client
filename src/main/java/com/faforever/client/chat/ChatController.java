package com.faforever.client.chat;

import com.faforever.client.irc.IrcService;
import com.faforever.client.irc.IrcUser;
import com.faforever.client.irc.OnChannelJoinedListener;
import com.faforever.client.irc.OnConnectedListener;
import com.faforever.client.irc.OnDisconnectedListener;
import com.faforever.client.irc.OnMessageListener;
import com.faforever.client.irc.OnPrivateMessageListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatController implements OnMessageListener, OnConnectedListener, OnDisconnectedListener, OnPrivateMessageListener, OnChannelJoinedListener {

  @Autowired
  Environment environment;

  @Autowired
  IrcService ircService;

  @FXML
  private TextField messageTextField;

  @FXML
  private TabPane chatsTabPane;

  @FXML
  private Pane chatContentPane;

  @FXML
  private Pane connectingProgressPane;

  private final Map<String, ChannelTab> nameToChatTab;

  public ChatController() {
    nameToChatTab = new HashMap<>();
  }

  public void load() {
    ircService.addOnMessageListener(this);
    ircService.addOnConnectedListener(this);
    ircService.addOnDisconnectedListener(this);
    ircService.addOnPrivateMessageListener(this);
    ircService.addOnChannelJoinedListener(this);

    ircService.connect();
  }

  @FXML
  private void initialize() {
    onDisconnected();
  }

  @Override
  public void onMessage(String channelName, Instant instant, String sender, String message) {
    addAndGetChannel(channelName).onMessage(instant, sender, message);
  }

  private ChannelTab addAndGetChannel(String channelName) {
    if (!nameToChatTab.containsKey(channelName)) {
      ChannelTab channelTab = new ChannelTab(this, channelName);
      nameToChatTab.put(channelName, channelTab);
      chatsTabPane.getTabs().add(channelTab);
    }
    return nameToChatTab.get(channelName);
  }

  @Override
  public void onConnected() {
  }

  @Override
  public void onDisconnected() {
    connectingProgressPane.setVisible(true);
    chatContentPane.setVisible(false);
  }

  @Override
  public void onChannelJoined(String channelName, List<IrcUser> users) {
    connectingProgressPane.setVisible(false);
    chatContentPane.setVisible(true);

    addAndGetChannel(channelName);
  }

  @Override
  public void onPrivateMessage(String sender, Instant instant, String message) {
    addAndGetChannel(sender).onMessage(instant, sender, message);
  }

  public void onSendMessage(ActionEvent actionEvent) {
    String target = chatsTabPane.getSelectionModel().getSelectedItem().getId();
    ircService.sendMessage(target, messageTextField.getText());
    messageTextField.clear();
  }
}
