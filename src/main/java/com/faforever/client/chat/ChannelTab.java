package com.faforever.client.chat;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.irc.IrcMessage;
import com.faforever.client.irc.IrcService;
import com.faforever.client.user.UserService;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class ChannelTab extends Tab {

  private static final Resource MESSAGE_ITEM_HTML_RESOURCE = new ClassPathResource("/themes/default/message_list_item.html");
  private static final DateTimeFormatter SHORT_TIME_FORMAT = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
  public static final ClassPathResource CHAT_HTML_RESOURCE = new ClassPathResource("/themes/default/message_list.html");

  @FXML
  private WebView messagesWebView;

  @FXML
  private VBox usersVBox;

  @FXML
  private TextField messageTextField;

  @Autowired
  UserService userService;

  @Autowired
  IrcService ircService;

  @Autowired
  FxmlLoader fxmlLoader;


  private final String channelName;

  /**
   * Set to true as soon as the chat is loaded. chatReadyLatch would be sufficient, however this takes off some
   * overhead
   */
  private boolean isChatReady;
  private WebEngine engine;
  private List<IrcMessage> waitingMessages;

  public ChannelTab(String channelName) {
    this.channelName = channelName;
    waitingMessages = new ArrayList<>();

    setClosable(true);
    setId(channelName);
    setText(channelName);
  }

  @PostConstruct
  void init() {
    fxmlLoader.loadCustomControl("channel_tab.fxml", this);
    initChatView();
  }

  private WebEngine initChatView() {
    messagesWebView.setContextMenuEnabled(false);
    messagesWebView.setOnScroll(new EventHandler<ScrollEvent>() {
      @Override
      public void handle(ScrollEvent event) {

      }
    });

    engine = messagesWebView.getEngine();
    engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
      if (Worker.State.SUCCEEDED.equals(newValue)) {
        waitingMessages.forEach(this::appendMessage);
        isChatReady = true;
      }
    });

    try {
      this.engine.load(CHAT_HTML_RESOURCE.getURL().toExternalForm());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return engine;
  }

  @FXML
  private void onSendMessage(ActionEvent actionEvent) {
    String text = messageTextField.getText();
    ircService.sendMessage(channelName, text);
    messageTextField.clear();
    onMessage(new IrcMessage(Instant.now(), userService.getUsername(), text));
  }

  public void onMessage(IrcMessage ircMessage) {
    if (!isChatReady) {
      waitingMessages.add(ircMessage);
    } else {
      appendMessage(ircMessage);
    }
  }

  private void appendMessage(IrcMessage ircMessage) {
    String timeString = SHORT_TIME_FORMAT.format(
        ZonedDateTime.ofInstant(ircMessage.getTime(), TimeZone.getDefault().toZoneId())
    );

    try (InputStream inputStream = MESSAGE_ITEM_HTML_RESOURCE.getInputStream()) {
      String html = IOUtils.toString(inputStream);
      String avatar = getAvatarForUser(ircMessage.getNick());
      html = String.format(html, timeString, avatar, ircMessage.getNick(), ircMessage.getMessage());

      JSObject htmlTag = (JSObject) engine.executeScript("document.querySelector('body')");
      htmlTag.call("insertAdjacentHTML", "beforeend", html);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getAvatarForUser(String sender) {
    return "";
  }

}
