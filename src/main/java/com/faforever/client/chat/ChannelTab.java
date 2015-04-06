package com.faforever.client.chat;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.legacy.message.PlayerInfoMessage;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class ChannelTab extends Tab {

  private static final ClassPathResource CHAT_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_container.html");
  private static final Resource MESSAGE_ITEM_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_message.html");
  private static final DateTimeFormatter SHORT_TIME_FORMAT = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
  private static final double ZOOM_STEP = 0.2d;
  private static final String MESSAGE_CONTAINER_ID = "chat-container";
  private static final String MESSAGE_ITEM_CLASS = "chat-message";
  private static final int MAX_MESSAGES_DISPLAYED = 256;

  @FXML
  WebView messagesWebView;

  @FXML
  VBox usersVBox;

  @FXML
  TextField messageTextField;

  @Autowired
  UserService userService;

  @Autowired
  ChatService chatService;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  ChatUserControlFactory chatUserControlFactory;

  @Autowired
  HostServices hostServices;

  @Autowired
  private PreferencesService preferencesService;

  private final String channelName;
  private ObservableMap<String, PlayerInfoMessage> playerInfoMap;

  /**
   * Set to true as soon as the chat is loaded. chatReadyLatch would be sufficient, however this takes off some
   * overhead
   */
  private boolean isChatReady;
  private WebEngine engine;
  private List<ChatMessage> waitingMessages;
  private ObservableMap<String, ChatUserControl> loginToUserControl;

  public ChannelTab(String channelName, ObservableMap<String, PlayerInfoMessage> playerInfoMap) {
    this.channelName = channelName;
    this.playerInfoMap = playerInfoMap;
    waitingMessages = new ArrayList<>();
    loginToUserControl = FXCollections.observableHashMap();

    setClosable(true);
    setId(channelName);
    setText(channelName);
  }

  @PostConstruct
  void init() {
    playerInfoMap.addListener((MapChangeListener<String, PlayerInfoMessage>) change -> {
      onPlayerInfoUpdated(change.getValueAdded());
    });
    fxmlLoader.loadCustomControl("channel_tab.fxml", this);
    initChatView();
  }

  @FXML
  void initialize() {
    loginToUserControl.addListener((MapChangeListener<String, ChatUserControl>) change -> {
      if (change.wasAdded()) {
        usersVBox.getChildren().add(change.getValueAdded());
      } else {
        usersVBox.getChildren().remove(change.getValueAdded());
      }
    });
  }

  private WebEngine initChatView() {
    messagesWebView.setContextMenuEnabled(false);
    messagesWebView.setOnScroll(event -> {
      if (event.isControlDown()) {
        if (event.getDeltaY() > 0) {
          messagesWebView.setZoom(messagesWebView.getZoom() + ZOOM_STEP);
        } else {
          messagesWebView.setZoom(messagesWebView.getZoom() - ZOOM_STEP);
        }
      }
    });
    messagesWebView.setOnKeyPressed(event -> {
      if (event.isControlDown() && (event.getCode() == KeyCode.DIGIT0 || event.getCode() == KeyCode.NUMPAD0)) {
        messagesWebView.setZoom(1);
      }
    });


    engine = messagesWebView.getEngine();
    engine.setUserDataDirectory(preferencesService.getPreferencesDirectory().toFile());
    ((JSObject) engine.executeScript("window")).setMember("channelTab", this);
    engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
      if (Worker.State.SUCCEEDED.equals(newValue)) {
        waitingMessages.forEach(this::appendMessage);
        waitingMessages.clear();
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

  /**
   * Called from JavaScript.
   */
  public void openUrl(String url) {
    hostServices.showDocument(url);
  }

  @FXML
  private void onSendMessage(ActionEvent actionEvent) {
    String text = messageTextField.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }

    chatService.sendMessage(channelName, text);
    messageTextField.clear();
    onMessage(new ChatMessage(Instant.now(), userService.getUsername(), text));
  }

  public void onMessage(ChatMessage chatMessage) {
    if (!isChatReady) {
      waitingMessages.add(chatMessage);
    } else {
      appendMessage(chatMessage);
      removeTopmostMessage();
      scrollToBottomIfDesired();
    }
  }

  private void scrollToBottomIfDesired() {
    engine.executeScript("window.scrollTo(0, document.body.scrollHeight);");
  }

  private void removeTopmostMessage() {
    int numberOfMessages = (int) engine.executeScript("document.getElementsByClassName('" + MESSAGE_ITEM_CLASS + "').length");
    if (numberOfMessages > MAX_MESSAGES_DISPLAYED) {
      engine.executeScript("document.getElementsByClassName('" + MESSAGE_ITEM_CLASS + "')[0].remove()");
    }
  }

  private void appendMessage(ChatMessage chatMessage) {
    String timeString = SHORT_TIME_FORMAT.format(
        ZonedDateTime.ofInstant(chatMessage.getTime(), TimeZone.getDefault().toZoneId())
    );

    try (InputStream inputStream = MESSAGE_ITEM_HTML_RESOURCE.getInputStream()) {
      String html = IOUtils.toString(inputStream);
      String avatar = getAvatarForUser(chatMessage.getLogin());

      String text = StringEscapeUtils.escapeHtml4(chatMessage.getMessage());
      text = (String) engine.executeScript("link('" + text.replace("'", "\\'") + "')");
      html = String.format(html, timeString, avatar, chatMessage.getLogin(), text);

      JSObject targetNode = (JSObject) engine.executeScript("document.getElementById('" + MESSAGE_CONTAINER_ID + "')");
      targetNode.call("insertAdjacentHTML", "beforeend", html);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getAvatarForUser(String sender) {
    return "";
  }

  public void onUserJoined(ChatUser chatUser) {
    addUser(chatUser);
  }

  /**
   * For adding single users, this method may be called directly. For adding a whole list, use the asynchronous {@link
   * #setUsersAsync(Set)} instead.
   */
  private void addUser(ChatUser chatUser) {
    addChatUserControl(
        chatUserControlFactory.newChatUserControl(chatUser)
    );
  }

  public void setUsersAsync(Set<ChatUser> chatUsers) {
    usersVBox.getChildren().clear();

    ConcurrentUtil.executeInBackground(
        new Task<Void>() {
          @Override
          protected Void call() throws Exception {
            ArrayList<ChatUser> sortedList = new ArrayList<>(chatUsers);
            Collections.sort(sortedList, ChatUser.SORT_BY_NAME_COMPARATOR);

            for (ChatUser chatUser : sortedList) {
              if (isCancelled()) {
                break;
              }

              ChatUserControl chatUserControl = chatUserControlFactory.newChatUserControl(chatUser);
              Platform.runLater(() -> addChatUserControl(chatUserControl));
            }

            return null;
          }
        }
    );
  }

  private void addChatUserControl(ChatUserControl chatUserControl) {
    String login = chatUserControl.getChatUser().getLogin();

    loginToUserControl.put(login, chatUserControl);

    PlayerInfoMessage playerInfoMessage = playerInfoMap.get(login);
    if (playerInfoMessage != null) {
      enrichChatUser(playerInfoMessage, chatUserControl);
    }
  }

  public void onPlayerInfoUpdated(PlayerInfoMessage playerInfoMessage) {
    ChatUserControl chatUserControl = loginToUserControl.get(playerInfoMessage.login);
    if (chatUserControl == null) {
      // Player info received before chat control is available (FAF login is faster than IRC login)
      return;
    }

    Platform.runLater(() -> enrichChatUser(playerInfoMessage, chatUserControl));
  }

  /**
   * Passes information from the FAF server to the chat user object.
   */
  private void enrichChatUser(PlayerInfoMessage playerInfoMessage, ChatUserControl chatUserControl) {
    chatUserControl.setAvatar(playerInfoMessage.avatar);
    chatUserControl.setClan(playerInfoMessage.clan);
    chatUserControl.setCountry(playerInfoMessage.country);
  }

  public void onUserLeft(String login) {
    loginToUserControl.remove(login);
  }
}
