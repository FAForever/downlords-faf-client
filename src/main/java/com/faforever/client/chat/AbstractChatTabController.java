package com.faforever.client.chat;

import com.faforever.client.audio.AudioController;
import com.faforever.client.chat.UrlPreviewResolver.Preview;
import com.faforever.client.fx.HostService;
import com.faforever.client.game.PlayerCardTooltipController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ByteCopier;
import com.faforever.client.util.JavaFxUtil;
import com.faforever.client.util.TimeService;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pircbotx.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
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
public abstract class AbstractChatTabController {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final Resource CHAT_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_container.html");
  private static final Resource CHAT_JS_RESOURCE = new ClassPathResource("/js/chat_container.js");
  private static final Resource AUTOLINKER_JS_RESOURCE = new ClassPathResource("/js/Autolinker.min.js");
  private static final Resource MESSAGE_ITEM_HTML_RESOURCE = new ClassPathResource("/themes/default/chat_message.html");
  private static final String MESSAGE_CONTAINER_ID = "chat-container";
  private static final String MESSAGE_ITEM_CLASS = "chat-message";
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
  private final List<ChatMessage> waitingMessages;
  /**
   * Maps a user name to a css style class.
   */
  private final Map<String, Color> userToColor;
  @Autowired
  UserService userService;
  @Autowired
  ChatService chatService;
  @Autowired
  HostService hostService;
  @Autowired
  PreferencesService preferencesService;
  @Autowired
  PlayerService playerService;
  @Autowired
  AudioController audioController;
  @Autowired
  TimeService timeService;
  @Autowired
  PlayerCardTooltipController playerCardTooltipController;
  @Autowired
  I18n i18n;
  @Autowired
  ImageUploadService imageUploadService;
  @Autowired
  UrlPreviewResolver urlPreviewResolver;
  @Autowired
  ChatController chatController;
  private boolean isChatReady;
  private WebEngine engine;
  private double lastMouseX;
  private double lastMouseY;
  private final EventHandler<MouseEvent> moveHandler = (MouseEvent event) -> {
    lastMouseX = event.getScreenX();
    lastMouseY = event.getScreenY();
  };
  /**
   * Either a channel like "#aeolus" or a user like "Visionik".
   */
  private String receiver;

  /**
   * Stores possible values for autocompletion (when strted typing a name, then pressing TAB). This value needs to be
   * set to {@code null} after the message has been sent or the caret has been moved in order to restart the
   * autocompletion on next TAB press.
   */
  private List<String> possibleAutoCompletions;
  private int nextAutoCompleteIndex;
  private String autoCompletePartialName;
  private Pattern mentionPattern;
  private Popup playerCardTooltip;
  private Tooltip linkPreviewTooltip;

  public AbstractChatTabController() {
    userToColor = new HashMap<>();
    waitingMessages = new ArrayList<>();
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  @PostConstruct
  void postConstruct() {
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    userToColor.put(userService.getUsername(), chatPrefs.getSelfChatColor());
    mentionPattern = Pattern.compile("\\b" + Pattern.quote(userService.getUsername()) + "\\b");

    Platform.runLater(this::initChatView);

    addFocusListeners();
    addImagePasteListener();
  }

  /**
   * Registers listeners necessary to focus the message input field when changing to another message tab, changing from
   * another tab to the "chat" tab or re-focusing the window.
   */
  private void addFocusListeners() {
    getRoot().selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        // Since a tab is marked as "selected" before it's rendered, the text field can't be selected yet.
        // So let's schedule the focus to be executed afterwards
        Platform.runLater(getMessageTextField()::requestFocus);
      }
    });

    getRoot().tabPaneProperty().addListener((tabPane, oldTabPane, newTabPane) -> {
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

  private void addImagePasteListener() {
    TextInputControl messageTextField = getMessageTextField();
    messageTextField.setOnKeyReleased(event -> {
      if (isPaste(event)
          && Clipboard.getSystemClipboard().hasImage()) {
        pasteImage();
      }
    });
  }

  public abstract Tab getRoot();

  protected abstract TextInputControl getMessageTextField();

  private boolean isPaste(KeyEvent event) {
    return (event.getCode() == KeyCode.V && event.isShortcutDown())
        || (event.getCode() == KeyCode.INSERT && event.isShiftDown());
  }

  private void pasteImage() {
    TextInputControl messageTextField = getMessageTextField();
    int currentCaretPosition = messageTextField.getCaretPosition();

    messageTextField.setDisable(true);

    Clipboard clipboard = Clipboard.getSystemClipboard();
    Image image = clipboard.getImage();

    imageUploadService.uploadImageInBackground(image).thenAccept(url -> {
      messageTextField.insertText(currentCaretPosition, url);
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
    }).exceptionally(throwable -> {
      messageTextField.setDisable(false);
      return null;
    });
  }

  private void initChatView() {
    WebView messagesWebView = getMessagesWebView();
    JavaFxUtil.configureWebView(messagesWebView, preferencesService);

    messagesWebView.addEventHandler(MouseEvent.MOUSE_MOVED, moveHandler);
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
        synchronized (waitingMessages) {
          waitingMessages.forEach(AbstractChatTabController.this::appendMessage);
          waitingMessages.clear();
          isChatReady = true;
        }
      }
    });

    try (InputStream inputStream = CHAT_HTML_RESOURCE.getInputStream()) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ByteCopier.from(inputStream).to(byteArrayOutputStream).copy();

      String chatContainerHtml = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8)
          .replace("{chat-container-js}", CHAT_JS_RESOURCE.getURL().toExternalForm())
          .replace("{auto-linker-js}", AUTOLINKER_JS_RESOURCE.getURL().toExternalForm());

      engine.loadContent(chatContainerHtml);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract WebView getMessagesWebView();

  private void resetAutoCompletion() {
    possibleAutoCompletions = null;
    nextAutoCompleteIndex = -1;
    autoCompletePartialName = null;
  }

  /**
   * Called from JavaScript when user hovers over a user name.
   */
  public void playerInfo(String username) {
    PlayerInfoBean playerInfoBean = playerService.getPlayerForUsername(username);
    if (playerInfoBean == null) {
      return;
    }

    playerCardTooltipController.setPlayer(playerInfoBean);

    playerCardTooltip = new Popup();
    playerCardTooltip.getContent().setAll(playerCardTooltipController.getRoot());
    playerCardTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);
    playerCardTooltip.show(getRoot().getTabPane(), lastMouseX, lastMouseY - 10);
  }

  /**
   * Called from JavaScript when user no longer hovers over a user name.
   */
  public void hidePlayerInfo() {
    if (playerCardTooltip == null) {
      return;
    }
    playerCardTooltip.hide();
    playerCardTooltip = null;
  }

  /**
   * Called from JavaScript when user clicks on user name in chat
   */
  public void openPrivateMessageTab(String username) {
    if (playerCardTooltip == null) {
      return;
    }
    chatController.openPrivateMessageTabForUser(username);
  }

  /**
   * Called from JavaScript when user clicked a URL.
   */
  public void openUrl(String url) {
    hostService.showDocument(url);
  }

  /**
   * Called from JavaScript when user hovers over an URL.
   */
  public void previewUrl(String urlString) {
    Preview preview = urlPreviewResolver.resolvePreview(urlString);
    if (preview == null) {
      return;
    }

    linkPreviewTooltip = new Tooltip(preview.getDescription());
    linkPreviewTooltip.setAutoHide(true);
    linkPreviewTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);
    linkPreviewTooltip.setGraphic(preview.getNode());
    linkPreviewTooltip.setContentDisplay(ContentDisplay.TOP);
    linkPreviewTooltip.show(getRoot().getTabPane(), lastMouseX + 20, lastMouseY);
  }

  /**
   * Called from JavaScript when user no longer hovers over an URL.
   */
  public void hideUrlPreview() {
    if (linkPreviewTooltip != null) {
      linkPreviewTooltip.hide();
      linkPreviewTooltip = null;
    }
  }

  @FXML
  void onSendMessage() {
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
    possibleAutoCompletions = new ArrayList<>();

    autoCompletePartialName = getWordBeforeCaret(messageTextField);
    if (autoCompletePartialName.isEmpty()) {
      return;
    }

    nextAutoCompleteIndex = 0;

    possibleAutoCompletions.addAll(
        playerService.getPlayerNames().stream()
            .filter(playerName -> playerName.toLowerCase().startsWith(autoCompletePartialName.toLowerCase()))
            .sorted()
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
    chatService.sendMessageInBackground(receiver, text).thenAccept(message -> {
      messageTextField.clear();
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
      onChatMessage(new ChatMessage(Instant.now(), userService.getUsername(), message));
    }).exceptionally(throwable -> {
      // TODO display error to user somehow
      logger.warn("Message could not be sent: {}", text, throwable);
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
      return null;
    });
  }

  private void sendAction(final TextInputControl messageTextField, final String text) {
    messageTextField.setDisable(true);

    chatService.sendActionInBackground(receiver, text.replaceFirst(Pattern.quote(ACTION_PREFIX), ""))
        .thenAccept(message -> {
          messageTextField.clear();
          messageTextField.setDisable(false);
          messageTextField.requestFocus();
          onChatMessage(new ChatMessage(Instant.now(), userService.getUsername(), message, true));
        }).exceptionally(throwable -> {
      // TODO display error to user somehow
      logger.warn("Message could not be sent: {}", text, throwable);
      messageTextField.setDisable(false);
      return null;
    });
  }

  protected void onChatMessage(ChatMessage chatMessage) {
    synchronized (waitingMessages) {
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
          clanTag = i18n.get("chat.clanTagFormat", playerInfoBean.getClan());
        }
      }

      String text = htmlEscaper().escape(chatMessage.getMessage()).replace("\\", "\\\\");
      text = convertUrlsToHyperlinks(text);

      if (mentionPattern.matcher(text).find()) {
        text = highlightOwnUsername(text);
        if (!hasFocus()) {
          audioController.playChatMentionSound();
        }
      }

      html = html.replace("{time}", timeString)
          .replace("{avatar}", StringUtils.defaultString(avatarUrl))
          .replace("{username}", login)
          .replace("{clan-tag}", clanTag)
          .replace("{text}", text);

      Collection<String> cssClasses = new ArrayList<>();
      String color = "";
      if (userToColor.containsKey(login)) {
        color = userToColor.get(login).toString();
      }

      if (chatMessage.isAction()) {
        cssClasses.add(ACTION_CSS_CLASS);
      } else {
        cssClasses.add(MESSAGE_CSS_CLASS);
      }
      PlayerInfoBean playerInfo = playerService.getPlayerForUsername(chatMessage.getUsername());

      html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses));
      String inlineStyle = getPlayerColorInlineStyle(playerInfo);
      if (!inlineStyle.equals("")) {
        color = inlineStyle;
      }

      html = html.replace("{color}", color);
      addToMessageContainer(html);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getPlayerColorInlineStyle(PlayerInfoBean playerInfo) {
    if (playerInfo == null) {
      return "";
    }
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    //Mods, friends and foes are never randomly generated
    String messageColor = "";
    if (playerInfo.getModeratorForChannels() != null && playerInfo.getModeratorForChannels().size() > 0) {
      //TODO this is here because there is no straight forward way to know what channel the message came from
      for (Channel channel : chatService.getChannelsForUser(userService.getUsername())) {
        if (chatService.getLevelsForChatUser(channel, playerInfo.getUsername()).size() > 0) {
          messageColor = chatPrefs.getModsChatColor().toString();
        }
      }
    } else if (playerInfo.isFriend()) {
      messageColor = chatPrefs.getFriendsChatColor().toString();
    } else if (playerInfo.isFoe()) {
      messageColor = chatPrefs.getFoesChatColor().toString();
    }

    //
    if (!chatPrefs.getPrettyColors() && messageColor.equals("")) {
      if (playerInfo.isChatOnly()) {
        messageColor = chatPrefs.getIrcChatColor().toString();
      } else if (!playerInfo.getUsername().equals(userService.getUsername())) {
        messageColor = chatPrefs.getOthersChatColor().toString();
      }
    }

    if (messageColor.equals("")) {
      if (playerInfo.getUsername().equals(userService.getUsername())) {
        messageColor = chatPrefs.getSelfChatColor().toString();
      } else {
        ChatUser chatUser = chatService.getChatUser(playerInfo.getUsername());
        /*if chatUser is null the message is lost, this happens when application is just initialized and the message is received before
        the user is registered to chatService*/
        if (chatUser != null) {
          //FIXME chat user doesn't always have color even though it should
          logger.debug("User {}", chatUser.getUsername());
          messageColor = chatUser.getColor().toString();
        }
      }
    }
    if(messageColor != null) {
      return createInlineStyleFromHexColor(messageColor);
    } else {
      return "";
    }
  }

  @NotNull
  private String createInlineStyleFromHexColor(String messageColor) {
    return "\" style=\"color:#" + messageColor.substring(2, 8);
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
    if (!getRoot().isSelected()) {
      return false;
    }

    TabPane tabPane = getRoot().getTabPane();
    return tabPane != null
        && JavaFxUtil.isVisibleRecursively(tabPane)
        && tabPane.getScene().getWindow().isFocused();

  }
}
