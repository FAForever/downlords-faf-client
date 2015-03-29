package com.faforever.client.chat;

import com.faforever.client.irc.IrcMessage;
import com.faforever.client.irc.IrcService;
import com.faforever.client.irc.IrcUser;
import com.faforever.client.irc.OnChannelJoinedListener;
import com.faforever.client.irc.OnDisconnectedListener;
import com.faforever.client.irc.OnMessageListener;
import com.faforever.client.irc.OnPrivateMessageListener;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatController implements OnMessageListener, OnDisconnectedListener, OnPrivateMessageListener, OnChannelJoinedListener {

  @Autowired
  Environment environment;

  @Autowired
  IrcService ircService;

  @Autowired
  ChannelTabFactory channelTabFactory;

  @FXML
  private TabPane chatsTabPane;

  @FXML
  private Pane connectingProgressPane;

  private final Map<String, ChannelTab> nameToChatTab;

  public ChatController() {
    nameToChatTab = new HashMap<>();
  }

  public void load() {
    ircService.addOnMessageListener(this);
    ircService.addOnDisconnectedListener(this);
    ircService.addOnPrivateMessageListener(this);
    ircService.addOnChannelJoinedListener(this);

    ircService.connect();
  }

  @FXML
  private void initialize() {
    onDisconnected(null);
  }

  @Override
  public void onMessage(String channelName, IrcMessage ircMessage) {
    addAndGetChannel(channelName).onMessage(ircMessage);
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
  public void onChannelJoined(String channelName, List<IrcUser> users) {
    connectingProgressPane.setVisible(false);
    chatsTabPane.setVisible(true);

    addAndGetChannel(channelName);
  }

  @Override
  public void onPrivateMessage(String sender, IrcMessage ircMessage) {
    addAndGetChannel(sender).onMessage(ircMessage);
  }
}
