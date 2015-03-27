package com.faforever.client.chat;

import com.faforever.client.irc.IrcClient;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;

public class ChatController {

  @Autowired
  Environment environment;

  @Autowired
  IrcClient ircClient;

  private ListView<Label> chatListView;
  private Node chat;

  public ChatController(Node chat) {
    this.chat = chat;
  }

  public void connectIrc() {
    Configuration<PircBotX> configuration = new Configuration.Builder()
        .setName(environment.getProperty("user.name"))
        .setLogin(environment.getProperty("user.password"))
        .setServerHostname(environment.getProperty("irc.host"))
        .setServerPort(environment.getProperty("irc.port", Integer.class))
        .addAutoJoinChannel(environment.getProperty("irc.defaultChannel"))
        .addListener(ircClient)
        .buildConfiguration();

    PircBotX pircBotX = new PircBotX(configuration);

    try {
      ircClient.addEventListener(MessageEvent.class, new IrcClient.IrcEventListener<MessageEvent>() {
        @Override
        public void onEvent(MessageEvent event) {
          onMessage(event.getMessage());
        }
      });


      ircClient.connect(pircBotX);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void onMessage(String message) {
    chatListView.getItems().add(new Label(message));
  }

}
