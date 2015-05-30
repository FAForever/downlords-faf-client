package com.faforever.client.chat;

import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.JavaFxUtil;
import com.sun.webkit.network.URLs;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a
 * JavaFX canvas isn't selectable, but text within a WebView is. This comes with some ugly implications; some of the
 * logic has to be performed in interaction with JavaScript, like when the user clicks a link.
 */
public abstract class AbstractChatTab extends Tab {

  private static final String CLAN_TAG_FORMAT = "[%s] ";
  private static final ClassPathResource CHAT_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_container.html");
  private static final Resource MESSAGE_ITEM_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_message.html");
  private static final DateTimeFormatter SHORT_TIME_FORMAT = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
  private static final double ZOOM_STEP = 0.2d;
  private static final String MESSAGE_CONTAINER_ID = "chat-container";
  private static final String MESSAGE_ITEM_CLASS = "chat-message";
  private static final String CSS_STYLE_SELF = "self";

  /**
   * This is the member name within the JavaScript code that provides access to this chat tab instance.
   */
  private static final String CHAT_TAB_REFERENCE_IN_JAVASCRIPT = "chatTab";

  private EventHandler<MouseEvent> MOVE_HANDLER = (MouseEvent event) -> {
    lastMouseX = event.getScreenX();
    lastMouseY = event.getScreenY();
  };

  @Autowired
  UserService userService;

  @Autowired
  ChatService chatService;

  @Autowired
  ChatController chatController;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  ChatUserControlFactory chatUserControlFactory;

  @Autowired
  HostServices hostServices;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  PlayerService playerService;

  private boolean isChatReady;
  private WebEngine engine;
  private List<ChatMessage> waitingMessages;
  private Map<String, String> userToCssStyle;
  private double lastMouseX;
  private double lastMouseY;

  /**
   * Either a channel like "#aeolus" or a user like "Visionik".
   */
  private String receiver;
  private String fxmlFile;

  public AbstractChatTab(String receiver, String fxmlFile) {
    this.receiver = receiver;
    this.fxmlFile = fxmlFile;

    userToCssStyle = new HashMap<>();
    waitingMessages = new ArrayList<>();

    setClosable(true);
  }

  @PostConstruct
  void postConstruct() {
    fxmlLoader.loadCustomControl(fxmlFile, this);
    initChatView();
    userToCssStyle.put(userService.getUsername(), CSS_STYLE_SELF);
  }

  private WebEngine initChatView() {
    WebView messagesWebView = getMessagesWebView();

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
    messagesWebView.addEventHandler(MouseEvent.MOUSE_MOVED, MOVE_HANDLER);
    messagesWebView.zoomProperty().addListener((observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getChat().setZoom(newValue.doubleValue());
      preferencesService.storeInBackground();
    });
    Double zoom = preferencesService.getPreferences().getChat().getZoom();
    if (zoom != null) {
      messagesWebView.setZoom(zoom);
    }

    engine = messagesWebView.getEngine();
    engine.setUserDataDirectory(preferencesService.getPreferencesDirectory().toFile());
    ((JSObject) engine.executeScript("window")).setMember(CHAT_TAB_REFERENCE_IN_JAVASCRIPT, this);
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

  protected abstract WebView getMessagesWebView();

  /**
   * Called from JavaScript when user clicked a URL.
   */
  public void openUrl(String url) {
    hostServices.showDocument(url);
  }

  /**
   * Called from JavaScript when user hovers over a URL.
   */
  public void previewUrl(String urlString) {
    try {
      URL url = new URL(urlString);
      String protocol = url.getProtocol();

      if (!"http".equals(protocol) && !"https".equals(protocol)) {
        // TODO log unhandled protocol
        return;
      }

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setInstanceFollowRedirects(true);

      long contentLength = connection.getContentLengthLong();
      String contentType = connection.getContentType();

      Popup popup = new Popup();
      popup.getContent().setAll(new Label(contentType));
      popup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);
     // popup.show(getTabPane(), lastMouseX, lastMouseY);
    } catch (IOException e) {
      // TODO log
    }
  }

  private Window getWindow(final Node node) {
    final Scene scene = node == null ? null : node.getScene();
    return scene == null ? null : scene.getWindow();
  }

  public void onSendMessage(ActionEvent actionEvent) {
    TextInputControl messageTextField = getMessageTextField();

    String text = messageTextField.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }

    chatService.sendMessage(receiver, text);
    messageTextField.clear();
    onMessage(new ChatMessage(Instant.now(), userService.getUsername(), text));
  }

  protected abstract TextInputControl getMessageTextField();

  public void onMessage(ChatMessage chatMessage) {
    if (!isChatReady) {
      waitingMessages.add(chatMessage);
    } else {
      appendMessage(chatMessage);
      removeTopmostMessages();
      scrollToBottomIfDesired();
    }
  }

  private void scrollToBottomIfDesired() {
    // TODO add the "if desired" part
    engine.executeScript("window.scrollTo(0, document.body.scrollHeight);");
  }

  private void removeTopmostMessages() {
    int maxMessageItems = preferencesService.getPreferences().getChat().getMaxItems();

    int numberOfMessages = (int) engine.executeScript("document.getElementsByClassName('" + MESSAGE_ITEM_CLASS + "').length");
    while (numberOfMessages > maxMessageItems) {
      engine.executeScript("document.getElementsByClassName('" + MESSAGE_ITEM_CLASS + "')[0].remove()");
      numberOfMessages--;
    }
  }

  private void appendMessage(ChatMessage chatMessage) {
    String timeString = SHORT_TIME_FORMAT.format(
        ZonedDateTime.ofInstant(chatMessage.getTime(), TimeZone.getDefault().toZoneId())
    );

    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(chatMessage.getLogin());

    try (InputStream inputStream = MESSAGE_ITEM_HTML_RESOURCE.getInputStream()) {
      String login = chatMessage.getLogin();
      String html = IOUtils.toString(inputStream);

      String avatar = "";
      String clanTag = "";
      if (playerInfoBean != null) {
        avatar = playerInfoBean.getAvatarUrl();

        if (StringUtils.isNotEmpty(playerInfoBean.getClan())) {
          clanTag = String.format(CLAN_TAG_FORMAT, playerInfoBean.getClan());
        }
      }

      String text = StringEscapeUtils.escapeHtml4(chatMessage.getMessage()).replace("\\", "\\\\");
      text = convertUrlsIntoHyperlinks(text);
      text = highlightOwnUsername(text);

      html = html.replace("{time}", timeString)
          .replace("{avatar}", StringUtils.defaultString(avatar))
          .replace("{username}", login)
          .replace("{clan-tag}", clanTag)
          .replace("{text}", text);

      if (userToCssStyle.containsKey(login)) {
        html = html.replace("{css-class}", userToCssStyle.get(login));
      }

      addToMessageContainer(html);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String highlightOwnUsername(String text) {
    return text.replaceAll(
        "(?:^|\\s])" + userService.getUsername() + "(?:\\s|$)",
        "<span class='own-username'>" + userService.getUsername() + "</span>"
    );
  }

  private String convertUrlsIntoHyperlinks(String text) {
    return (String) engine.executeScript("link('" + text.replace("'", "\\'") + "')");
  }

  private void addToMessageContainer(String html) {
    ((JSObject) engine.executeScript("document.getElementById('" + MESSAGE_CONTAINER_ID + "')"))
        .call("insertAdjacentHTML", "beforeend", html);
  }
}
