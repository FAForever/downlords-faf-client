package com.faforever.client.chat;

import com.faforever.client.fx.HostService;
import com.faforever.client.fxml.FxmlLoader;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.sound.SoundController;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.util.TimeService;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.html.HtmlEscapers.htmlEscaper;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some of the logic has to be
 * performed in interaction with JavaScript, like when the user clicks a link.
 */
// TODO create a ChatTabController that does not extend Tab but encapsulate it
public abstract class AbstractChatTab extends Tab {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final ClassPathResource CHAT_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_container.html");
  private static final Resource MESSAGE_ITEM_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_message.html");
  private static final String MESSAGE_CONTAINER_ID = "chat-container";
  private static final String MESSAGE_ITEM_CLASS = "chat-message";
  private static final String CSS_STYLE_SELF = "self";

  /**
   * This is the member name within the JavaScript code that provides access to this chat tab instance.
   */
  private static final String CHAT_TAB_REFERENCE_IN_JAVASCRIPT = "chatTab";
  private static final String ACTION_PREFIX = "/me ";
  private static final String JOIN_PREFIX = "/join ";

  /**
   * Added if a message is what IRC calls an "action".
   */
  private static final String ACTION_CSS_CLASS = "action";
  private static final String MESSAGE_CSS_CLASS = "message";

  private EventHandler<MouseEvent> MOVE_HANDLER = (MouseEvent event) -> {
    lastMouseX = event.getScreenX();
    lastMouseY = event.getScreenY();
  };

  @Autowired
  UserService userService;

  @Autowired
  ChatService chatService;

  @Autowired
  FxmlLoader fxmlLoader;

  @Autowired
  HostService hostService;

  @Autowired
  PreferencesService preferencesService;

  @Autowired
  PlayerService playerService;

  @Autowired
  SoundController soundController;

  @Autowired
  TimeService timeService;

  @Autowired
  PlayerInfoTooltipController playerInfoTooltipController;

  @Autowired
  I18n i18n;

  @Autowired
  ImageUploadService imageUploadService;

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

  /**
   * Stores possible values for autocompletion (when strted typing a name, then pressing TAB). This value needs to be
   * set to {@code null} after the message has been sent or the caret has been moved in order to restart the
   * autocompletion on next TAB press.
   */
  private ArrayList<String> possibleAutoCompletions;
  private int nextAutoCompleteIndex;
  private String autoCompletePartialName;
  private Pattern mentionPattern;
  private Popup playerInfoTooltip;

  public AbstractChatTab(String receiver, String fxmlFile) {
    this.receiver = receiver;
    this.fxmlFile = fxmlFile;

    userToCssStyle = new HashMap<>();
    waitingMessages = new ArrayList<>();

    setClosable(true);
  }

  @PostConstruct
  void postConstruct() {
    userToCssStyle.put(userService.getUsername(), CSS_STYLE_SELF);
    mentionPattern = Pattern.compile("\\b" + Pattern.quote(userService.getUsername()) + "\\b");

    fxmlLoader.loadCustomControl(fxmlFile, this);
    initChatView();

    addFocusListeners();
    addImagePasteListener();
  }

  private void addImagePasteListener() {
    TextInputControl messageTextField = getMessageTextField();
    messageTextField.setOnKeyReleased(event -> {
      if (event.getCode() == KeyCode.V && event.isControlDown()
          || event.getCode() == KeyCode.INSERT && event.isShiftDown()
          && Clipboard.getSystemClipboard().hasImage()) {
        pasteImage();
      }
    });
  }

  private void pasteImage() {
    TextInputControl messageTextField = getMessageTextField();
    messageTextField.setDisable(true);

    Clipboard clipboard = Clipboard.getSystemClipboard();
    Image image = clipboard.getImage();

    imageUploadService.uploadImage(image, new Callback<String>() {
      @Override
      public void success(String url) {
        int currentCaretPosition = messageTextField.getCaretPosition();
        messageTextField.insertText(currentCaretPosition, url);
        sendMessage();

        messageTextField.setDisable(false);
      }

      @Override
      public void error(Throwable e) {
        messageTextField.setDisable(false);
      }
    });
  }

  /**
   * Registers listeners necessary to focus the message input field when changing to another message tab, changing from
   * another tab to the "chat" tab or re-focusing the window.
   */
  private void addFocusListeners() {
    selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        // Since a tab is marked as "selected" before it's rendered, the text field can't be selected yet.
        // So let's schedule the focus to be executed afterwards
        Platform.runLater(() -> getMessageTextField().requestFocus());
      }
    });

    tabPaneProperty().addListener((tabPane, oldTabPane, newTabPane) -> {
      if (newTabPane == null) {
        return;
      }
      newTabPane.sceneProperty().addListener((tabPane1, oldScene, newScene) -> {
        newScene.getWindow().focusedProperty().addListener((window, windowFocusOld, windowFocusNew) -> {
          if (newTabPane.isVisible()) {
            getMessageTextField().requestFocus();
          }
        });
      });
      newTabPane.focusedProperty().addListener((focusedTabPane, oldTabPaneFocus, newTabPaneFocus) -> {
        if (newTabPaneFocus) {
          getMessageTextField().requestFocus();
        }
      });
    });
  }

  private void resetAutoCompletion() {
    possibleAutoCompletions = null;
    nextAutoCompleteIndex = -1;
    autoCompletePartialName = null;
  }

  private WebEngine initChatView() {
    WebView messagesWebView = getMessagesWebView();
    JavaFxUtil.configureWebView(messagesWebView, preferencesService);

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
   * Called from JavaScript when user hovers over a user name.
   */
  public void playerInfo(String username) {
    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(username);
    if (playerInfoBean == null) {
      return;
    }

    playerInfoTooltipController.setPlayerInfoBean(playerInfoBean);

    playerInfoTooltip = new Popup();
    playerInfoTooltip.getContent().setAll(playerInfoTooltipController.getRoot());
    playerInfoTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);
    playerInfoTooltip.show(getTabPane(), lastMouseX, lastMouseY - 10);
  }

  /**
   * Called from JavaScript when user no longer hovers over a user name.
   */
  public void hidePlayerInfo() {
    if (playerInfoTooltip == null) {
      return;
    }
    playerInfoTooltip.hide();
    playerInfoTooltip = null;
  }

  /**
   * Called from JavaScript when user clicked a URL.
   */
  public void openUrl(String url) {
    hostService.showDocument(url);
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

  @FXML
  void onSendMessage(ActionEvent actionEvent) {
    TextInputControl messageTextField = getMessageTextField();

    String text = messageTextField.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }

    if (text.startsWith(ACTION_PREFIX)) {
      sendAction(messageTextField, text);
    } else if (text.startsWith(JOIN_PREFIX)) {
      chatService.joinChannel(text.replaceFirst(Pattern.quote(JOIN_PREFIX), ""));
    } else {
      sendMessage();
    }

    resetAutoCompletion();
  }

  @FXML
  void onKeyPressed(KeyEvent keyEvent) {
    if (!keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.TAB) {
      keyEvent.consume();
      autoComplete();
    }
  }

  private void autoComplete() {
    TextInputControl messageTextField = getMessageTextField();

    if (messageTextField.getText().isEmpty()) {
      return;
    }

    if (possibleAutoCompletions == null) {
      initializeAutoCompletion(messageTextField);

      if (possibleAutoCompletions.isEmpty()) {
        // There are no autocompletion matches
        resetAutoCompletion();
        return;
      }

      // It's the first autocomplete event at this location, just replace the text with the first user name
      messageTextField.selectPreviousWord();
      messageTextField.replaceSelection(possibleAutoCompletions.get(nextAutoCompleteIndex++));
      return;
    }

    // At this point, it's a subsequent auto complete event
    String wordBeforeCaret = getWordBeforeCaret(messageTextField);

    /*
     * We have to check if the previous word is the one we auto completed. If not we reset and start all over again
     * as the user started auto completion on another word.
     */
    if (!wordBeforeCaret.equals(possibleAutoCompletions.get(nextAutoCompleteIndex - 1))) {
      resetAutoCompletion();
      autoComplete();
      return;
    }

    if (possibleAutoCompletions.size() == 1) {
      // No need to cycle since there was only one match
      return;
    }

    if (possibleAutoCompletions.size() <= nextAutoCompleteIndex) {
      // Start over again in order to cycle
      nextAutoCompleteIndex = 0;
    }

    messageTextField.selectPreviousWord();
    messageTextField.replaceSelection(possibleAutoCompletions.get(nextAutoCompleteIndex++));
  }

  private void initializeAutoCompletion(TextInputControl messageTextField) {
    autoCompletePartialName = getWordBeforeCaret(messageTextField);

    possibleAutoCompletions = new ArrayList<>();
    nextAutoCompleteIndex = 0;

    possibleAutoCompletions.addAll(
        playerService.getPlayerNames().stream()
            .filter(playerName -> playerName.toLowerCase().startsWith(autoCompletePartialName.toLowerCase()))
            .collect(Collectors.toList())
    );
  }

  private String getWordBeforeCaret(TextInputControl messageTextField) {
    messageTextField.selectPreviousWord();
    String selectedText = messageTextField.getSelectedText();
    messageTextField.positionCaret(messageTextField.getAnchor());
    return selectedText;
  }

  private void sendMessage() {
    TextInputControl messageTextField = getMessageTextField();
    messageTextField.setDisable(true);

    final String text = messageTextField.getText();
    chatService.sendMessageInBackground(receiver, text, new Callback<String>() {
      @Override
      public void success(String message) {
        messageTextField.clear();
        messageTextField.setDisable(false);
        messageTextField.requestFocus();
        onChatMessage(new ChatMessage(Instant.now(), userService.getUsername(), message));
      }

      @Override
      public void error(Throwable e) {
        // TODO display error to user somehow
        logger.warn("Message could not be sent: {}", text, e);
        messageTextField.setDisable(false);
        messageTextField.requestFocus();
      }
    });
  }

  private void sendAction(final TextInputControl messageTextField, final String text) {
    messageTextField.setDisable(true);

    chatService.sendActionInBackground(receiver, text.replaceFirst(Pattern.quote(ACTION_PREFIX), ""), new Callback<String>() {
      @Override
      public void success(String message) {
        messageTextField.clear();
        messageTextField.setDisable(false);
        messageTextField.requestFocus();
        onChatMessage(new ChatMessage(Instant.now(), userService.getUsername(), message, true));
      }

      @Override
      public void error(Throwable e) {
        // TODO display error to user somehow
        logger.warn("Message could not be sent: {}", text, e);
        messageTextField.setDisable(false);
      }
    });
  }

  protected abstract TextInputControl getMessageTextField();

  public void onChatMessage(ChatMessage chatMessage) {
    if (!isChatReady) {
      waitingMessages.add(chatMessage);
    } else {
      Platform.runLater(() -> {
        appendMessage(chatMessage);
        removeTopmostMessages();
        scrollToBottomIfDesired();
      });
    }
  }

  private void scrollToBottomIfDesired() {
    if (getMessagesWebView().getParent() == null) {
      return;
    }

    engine.executeScript("scrollToBottomIfDesired()");
  }

  private void removeTopmostMessages() {
    int maxMessageItems = preferencesService.getPreferences().getChat().getMaxMessages();

    int numberOfMessages = (int) engine.executeScript("document.getElementsByClassName('" + MESSAGE_ITEM_CLASS + "').length");
    while (numberOfMessages > maxMessageItems) {
      engine.executeScript("document.getElementsByClassName('" + MESSAGE_ITEM_CLASS + "')[0].remove()");
      numberOfMessages--;
    }
  }

  private void appendMessage(ChatMessage chatMessage) {
    String timeString = timeService.asShortTime(chatMessage.getTime());

    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(chatMessage.getUsername());

    try (Reader reader = new InputStreamReader(MESSAGE_ITEM_HTML_RESOURCE.getInputStream())) {
      String login = chatMessage.getUsername();
      String html = CharStreams.toString(reader);

      String avatarUrl = "";
      String clanTag = "";
      if (playerInfoBean != null) {
        avatarUrl = playerInfoBean.getAvatarUrl();

        if (StringUtils.isNotEmpty(playerInfoBean.getClan())) {
          clanTag = i18n.get("chat.clanTagFormag", playerInfoBean.getClan());
        }
      }

      String text = htmlEscaper().escape(chatMessage.getMessage()).replace("\\", "\\\\");
      text = convertUrlsToHyperlinks(text);

      if (mentionPattern.matcher(text).find()) {
        text = highlightOwnUsername(text);
        if (!hasFocus()) {
          soundController.playChatMentionSound();
        }
      }

      html = html.replace("{time}", timeString)
          .replace("{avatar}", StringUtils.defaultString(avatarUrl))
          .replace("{username}", login)
          .replace("{clan-tag}", clanTag)
          .replace("{text}", text);

      Collection<String> cssClasses = new ArrayList<>();

      if (userToCssStyle.containsKey(login)) {
        cssClasses.add(userToCssStyle.get(login));
      }

      if (chatMessage.isAction()) {
        cssClasses.add(ACTION_CSS_CLASS);
      } else {
        cssClasses.add(MESSAGE_CSS_CLASS);
      }

      html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses));

      addToMessageContainer(html);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String highlightOwnUsername(String text) {
    // TODO outsource in html file
    return text.replaceAll(
        mentionPattern.pattern(),
        "<span class='own-username'>" + userService.getUsername() + "</span>"
    );
  }

  private String convertUrlsToHyperlinks(String text) {
    return (String) engine.executeScript("link('" + text.replace("'", "\\'") + "')");
  }

  private void addToMessageContainer(String html) {
    ((JSObject) engine.executeScript("document.getElementById('" + MESSAGE_CONTAINER_ID + "')"))
        .call("insertAdjacentHTML", "beforeend", html);
  }

  /**
   * Returns true if this chat tab is currently focused by the user. Returns false if a different tab is selected, the
   * user is not in "chat" or if the window has no focus.
   */
  protected boolean hasFocus() {
    if (!isSelected()) {
      return false;
    }

    TabPane tabPane = getTabPane();
    return tabPane != null
        && JavaFxUtil.isVisibleRecursively(tabPane)
        && tabPane.getScene().getWindow().isFocused();

  }
}
