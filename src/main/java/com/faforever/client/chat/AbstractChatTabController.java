package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.common.io.CharStreams;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static com.faforever.client.theme.UiService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.UiService.CHAT_SECTION_EXTENDED;
import static com.faforever.client.theme.UiService.CHAT_TEXT_COMPACT;
import static com.faforever.client.theme.UiService.CHAT_TEXT_EXTENDED;
import static com.github.nocatch.NoCatch.noCatch;
import static com.google.common.html.HtmlEscapers.htmlEscaper;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javafx.scene.AccessibleAttribute.ITEM_AT_INDEX;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some of the logic has to be
 * performed in interaction with JavaScript, like when the user clicks a link.
 */
public abstract class AbstractChatTabController implements Controller<Tab> {

  static final String CSS_CLASS_CHAT_ONLY = "chat_only";
  private static final String MESSAGE_CONTAINER_ID = "chat-container";
  private static final String MESSAGE_ITEM_CLASS = "chat-section";
  private static final PseudoClass UNREAD_PSEUDO_STATE = PseudoClass.getPseudoClass("unread");
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final org.springframework.core.io.Resource CHAT_JS_RESOURCE = new ClassPathResource("/js/chat_container.js");
  private static final org.springframework.core.io.Resource AUTOLINKER_JS_RESOURCE = new ClassPathResource("/js/Autolinker.min.js");
  private static final org.springframework.core.io.Resource JQUERY_JS_RESOURCE = new ClassPathResource("js/jquery-2.1.4.min.js");
  private static final org.springframework.core.io.Resource JQUERY_HIGHLIGHT_JS_RESOURCE = new ClassPathResource("js/jquery.highlight-5.closure.js");

  public static final String CHANNEL_NAME_GROUP_NAME = "channelName";
  /**
   * A pattern identifying all strings with a # in front and not starting with a number. Those are interpreted as
   * irc-channels.
   */
  private static final Pattern CHANNEL_USER_PATTERN = Pattern.compile("(^|\\s)(?<" + CHANNEL_NAME_GROUP_NAME + ">#[a-zA-Z]\\S+)", CASE_INSENSITIVE);

  private static final String ACTION_PREFIX = "/me ";
  private static final String JOIN_PREFIX = "/join ";
  private static final String WHOIS_PREFIX = "/whois ";
  /**
   * Added if a message is what IRC calls an "action".
   */
  private static final String ACTION_CSS_CLASS = "action";
  private static final String MESSAGE_CSS_CLASS = "message";
  protected final UserService userService;
  protected final ChatService chatService;
  protected final PreferencesService preferencesService;
  protected final PlayerService playerService;
  protected final AudioService audioService;
  protected final TimeService timeService;
  protected final I18n i18n;
  protected final NotificationService notificationService;
  protected final ReportingService reportingService;
  protected final UiService uiService;
  protected final EventBus eventBus;
  protected final WebViewConfigurer webViewConfigurer;
  private final ImageUploadService imageUploadService;
  private final CountryFlagService countryFlagService;

  /**
   * Messages that arrived before the web view was ready. Those are appended as soon as it is ready.
   */
  private final List<ChatMessage> waitingMessages;
  private final IntegerProperty unreadMessagesCount;
  private final ChangeListener<Boolean> resetUnreadMessagesListener;
  private final ChangeListener<Number> zoomChangeListener;
  private final ChangeListener<Boolean> tabPaneFocusedListener;
  private final ChangeListener<Boolean> stageFocusedListener;
  private int lastEntryId;
  private boolean isChatReady;
  /**
   * Either a channel like "#aeolus" or a user like "Visionik".
   */
  private String receiver;
  private Pattern mentionPattern;
  private ChatMessage lastMessage;
  WebEngine engine;

  @Inject
  // TODO cut dependencies
  public AbstractChatTabController(WebViewConfigurer webViewConfigurer,
                                   UserService userService, ChatService chatService,
                                   PreferencesService preferencesService,
                                   PlayerService playerService, AudioService audioService,
                                   TimeService timeService, I18n i18n,
                                   ImageUploadService imageUploadService,
                                   NotificationService notificationService, ReportingService reportingService, UiService uiService,
                                   EventBus eventBus, CountryFlagService countryFlagService) {

    this.webViewConfigurer = webViewConfigurer;
    this.uiService = uiService;
    this.chatService = chatService;
    this.userService = userService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.audioService = audioService;
    this.timeService = timeService;
    this.i18n = i18n;
    this.imageUploadService = imageUploadService;
    this.notificationService = notificationService;
    this.reportingService = reportingService;
    this.eventBus = eventBus;
    this.countryFlagService = countryFlagService;

    waitingMessages = new ArrayList<>();
    unreadMessagesCount = new SimpleIntegerProperty();
    resetUnreadMessagesListener = (observable, oldValue, newValue) -> {
      if (hasFocus()) {
        setUnread(false);
      }
    };
    zoomChangeListener = (observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getChat().setZoom(newValue.doubleValue());
      preferencesService.storeInBackground();
    };
    stageFocusedListener = (window, windowFocusOld, windowFocusNew) -> {
      if (getRoot() != null
          && getRoot().getTabPane() != null
          && getRoot().getTabPane().isVisible()) {
        messageTextField().requestFocus();
      }
    };
    tabPaneFocusedListener = (focusedTabPane, oldTabPaneFocus, newTabPaneFocus) -> {
      if (newTabPaneFocus) {
        messageTextField().requestFocus();
      }
    };
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
        && tabPane.getScene().getWindow().isFocused()
        && tabPane.getScene().getWindow().isShowing();
  }

  protected void setUnread(boolean unread) {
    TabPane tabPane = getRoot().getTabPane();
    if (tabPane == null) {
      return;
    }
    TabPaneSkin skin = (TabPaneSkin) tabPane.getSkin();
    if (skin == null) {
      return;
    }
    int tabIndex = tabPane.getTabs().indexOf(getRoot());
    if (tabIndex == -1) {
      // Tab has been closed
      return;
    }
    Node tab = (Node) skin.queryAccessibleAttribute(ITEM_AT_INDEX, tabIndex);
    tab.pseudoClassStateChanged(UNREAD_PSEUDO_STATE, unread);

    if (!unread) {
      synchronized (unreadMessagesCount) {
        unreadMessagesCount.setValue(0);
      }
    }
  }

  public abstract Tab getRoot();

  protected void incrementUnreadMessagesCount(int delta) {
    synchronized (unreadMessagesCount) {
      unreadMessagesCount.set(unreadMessagesCount.get() + delta);
    }
  }

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  public void initialize() {
    mentionPattern = Pattern.compile("\\b(" + Pattern.quote(userService.getUsername()) + ")\\b", CASE_INSENSITIVE);

    initChatView();

    addFocusListeners();
    addImagePasteListener();

    unreadMessagesCount.addListener((observable, oldValue, newValue) -> chatService.incrementUnreadMessagesCount(newValue.intValue() - oldValue.intValue()));
    JavaFxUtil.addListener(StageHolder.getStage().focusedProperty(), new WeakChangeListener<>(resetUnreadMessagesListener));
    JavaFxUtil.addListener(getRoot().selectedProperty(), new WeakChangeListener<>(resetUnreadMessagesListener));

    getRoot().setOnClosed(this::onClosed);
  }

  protected void onClosed(Event event) {
    // Subclasses may override but need to call super
  }

  /**
   * Registers listeners necessary to focus the message input field when changing to another message tab, changing from
   * another tab to the "chat" tab or re-focusing the window.
   */
  private void addFocusListeners() {
    JavaFxUtil.addListener(getRoot().selectedProperty(), (observable, oldValue, newValue) -> {
      if (newValue) {
        // Since a tab is marked as "selected" before it's rendered, the text field can't be selected yet.
        // So let's schedule the focus to be executed afterwards
        Platform.runLater(messageTextField()::requestFocus);
      }
    });

    JavaFxUtil.addListener(getRoot().tabPaneProperty(), (tabPane, oldTabPane, newTabPane) -> {
      if (newTabPane == null) {
        return;
      }
      JavaFxUtil.addListener(StageHolder.getStage().focusedProperty(), new WeakChangeListener<>(stageFocusedListener));
      JavaFxUtil.addListener(newTabPane.focusedProperty(), new WeakChangeListener<>(tabPaneFocusedListener));
    });
  }

  private void addImagePasteListener() {
    TextInputControl messageTextField = messageTextField();
    messageTextField.setOnKeyReleased(event -> {
      if (isPaste(event)
          && Clipboard.getSystemClipboard().hasImage()) {
        pasteImage();
      }
    });
  }

  protected abstract TextInputControl messageTextField();

  private boolean isPaste(KeyEvent event) {
    return (event.getCode() == KeyCode.V && event.isShortcutDown())
        || (event.getCode() == KeyCode.INSERT && event.isShiftDown());
  }

  private void pasteImage() {
    TextInputControl messageTextField = messageTextField();
    int currentCaretPosition = messageTextField.getCaretPosition();

    messageTextField.setDisable(true);

    Clipboard clipboard = Clipboard.getSystemClipboard();
    Image image = clipboard.getImage();

    imageUploadService.uploadImageInBackground(image).thenAccept(url -> {
      messageTextField.insertText(currentCaretPosition, url);
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
      messageTextField.positionCaret(messageTextField.getLength());
    }).exceptionally(throwable -> {
      messageTextField.setDisable(false);
      return null;
    });
  }

  private void initChatView() {
    WebView messagesWebView = getMessagesWebView();
    webViewConfigurer.configureWebView(messagesWebView);

    messagesWebView.zoomProperty().addListener(new WeakChangeListener<>(zoomChangeListener));

    configureBrowser(messagesWebView);
    loadChatContainer();
  }

  private void loadChatContainer() {
    try (Reader reader = new InputStreamReader(uiService.getThemeFileUrl(CHAT_CONTAINER).openStream())) {
      String chatContainerHtml = CharStreams.toString(reader)
          .replace("{chat-container-js}", CHAT_JS_RESOURCE.getURL().toExternalForm())
          .replace("{auto-linker-js}", AUTOLINKER_JS_RESOURCE.getURL().toExternalForm())
          .replace("{jquery-js}", JQUERY_JS_RESOURCE.getURL().toExternalForm())
          .replace("{jquery-highlight-js}", JQUERY_HIGHLIGHT_JS_RESOURCE.getURL().toExternalForm());

      engine.loadContent(chatContainerHtml);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void configureBrowser(WebView messagesWebView) {
    engine = messagesWebView.getEngine();

    configureZoomLevel();
    configureLoadListener();
  }

  private void configureZoomLevel() {
    Double zoom = preferencesService.getPreferences().getChat().getZoom();
    if (zoom != null) {
      getMessagesWebView().setZoom(zoom);
    }
  }

  private void configureLoadListener() {
    JavaFxUtil.addListener(engine.getLoadWorker().stateProperty(), (observable, oldValue, newValue) -> {
      if (newValue != Worker.State.SUCCEEDED) {
        return;
      }
      synchronized (waitingMessages) {
        waitingMessages.forEach(AbstractChatTabController.this::addMessage);
        waitingMessages.clear();
        isChatReady = true;
        onWebViewLoaded();
      }
    });
  }

  protected abstract WebView getMessagesWebView();

  protected JSObject getJsObject() {
    return (JSObject) engine.executeScript("window");
  }

  protected void onWebViewLoaded() {
    // Default implementation does nothing, can be overridden by subclass.
  }

  public void onSendMessage() {
    TextInputControl messageTextField = messageTextField();

    String text = messageTextField.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }

    if (text.startsWith(ACTION_PREFIX)) {
      sendAction(messageTextField, text);
    } else if (text.startsWith(JOIN_PREFIX)) {
      chatService.joinChannel(text.replaceFirst(Pattern.quote(JOIN_PREFIX), ""));
      messageTextField.clear();
    } else if (text.startsWith(WHOIS_PREFIX)) {
      chatService.whois(text.replaceFirst(Pattern.quote(JOIN_PREFIX), ""));
      messageTextField.clear();
    } else {
      sendMessage();
    }
  }

  private void sendMessage() {
    TextInputControl messageTextField = messageTextField();
    messageTextField.setDisable(true);

    final String text = messageTextField.getText();
    chatService.sendMessageInBackground(receiver, text).thenAccept(message -> {
      messageTextField.clear();
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
    }).exceptionally(throwable -> {
      throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
      logger.warn("Message could not be sent: {}", text, throwable);
      notificationService.addImmediateErrorNotification(throwable, "chat.sendFailed");

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
          onChatMessage(new ChatMessage(userService.getUsername(), Instant.now(), userService.getUsername(), message, true));
        })
        .exceptionally(throwable -> {
          throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
          // TODO onDisplay error to user somehow
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
          addMessage(chatMessage);
          removeTopmostMessages();
          scrollToBottomIfDesired();
        });
      }
    }
  }

  private void scrollToBottomIfDesired() {
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

  /**
   * Either inserts a new chat entry or, if the same user as before sent another message, appends it do the previous
   * entry.
   */
  private void addMessage(ChatMessage chatMessage) {
    noCatch(() -> {
      if (requiresNewChatSection(chatMessage)) {
        appendChatMessageSection(chatMessage);
      } else {
        appendMessage(chatMessage);
      }
      lastMessage = chatMessage;
    });
  }

  private boolean requiresNewChatSection(ChatMessage chatMessage) {
    return lastMessage == null
        || !lastMessage.getUsername().equals(chatMessage.getUsername())
        || lastMessage.getTime().isBefore(chatMessage.getTime().minus(1, MINUTES))
        || lastMessage.isAction();
  }

  private void appendMessage(ChatMessage chatMessage) throws IOException {
    URL themeFileUrl;
    if (preferencesService.getPreferences().getChat().getChatFormat() == ChatFormat.COMPACT) {
      themeFileUrl = uiService.getThemeFileUrl(CHAT_TEXT_COMPACT);
    } else {
      themeFileUrl = uiService.getThemeFileUrl(CHAT_TEXT_EXTENDED);
    }

    String html = renderHtml(chatMessage, themeFileUrl, null);

    insertIntoContainer(html, "chat-section-" + lastEntryId);
  }

  private void appendChatMessageSection(ChatMessage chatMessage) throws IOException {
    URL themeFileURL;
    if (preferencesService.getPreferences().getChat().getChatFormat() == ChatFormat.COMPACT) {
      themeFileURL = uiService.getThemeFileUrl(CHAT_SECTION_COMPACT);
    } else {
      themeFileURL = uiService.getThemeFileUrl(CHAT_SECTION_EXTENDED);
    }

    String html = renderHtml(chatMessage, themeFileURL, ++lastEntryId);
    insertIntoContainer(html, MESSAGE_CONTAINER_ID);
    appendMessage(chatMessage);
  }

  private String renderHtml(ChatMessage chatMessage, URL themeFileUrl, @Nullable Integer sectionId) throws IOException {
    String html;
    try (Reader reader = new InputStreamReader(themeFileUrl.openStream())) {
      html = CharStreams.toString(reader);
    }

    String login = chatMessage.getUsername();
    String avatarUrl = "";
    String clanTag = "";
    String decoratedClanTag = "";
    String countryFlagUrl = "";

    Optional<Player> playerOptional = playerService.getPlayerForUsername(chatMessage.getUsername());
    if (playerOptional.isPresent()) {
      Player player = playerOptional.get();
      avatarUrl = player.getAvatarUrl();
      countryFlagUrl = countryFlagService.getCountryFlagUrl(player.getCountry())
          .map(URL::toString)
          .orElse("");

      if (StringUtils.isNotEmpty(player.getClan())) {
        clanTag = player.getClan();
        decoratedClanTag = i18n.get("chat.clanTagFormat", clanTag);
      }
    }

    String timeString = timeService.asShortTime(chatMessage.getTime());
    html = html.replace("{time}", timeString)
        .replace("{avatar}", StringUtils.defaultString(avatarUrl))
        .replace("{username}", login)
        .replace("{clan-tag}", clanTag)
        .replace("{decorated-clan-tag}", decoratedClanTag)
        .replace("{country-flag}", StringUtils.defaultString(countryFlagUrl))
        .replace("{section-id}", String.valueOf(sectionId));

    Collection<String> cssClasses = new ArrayList<>();
    cssClasses.add(String.format("user-%s", chatMessage.getUsername()));
    if (chatMessage.isAction()) {
      cssClasses.add(ACTION_CSS_CLASS);
    } else {
      cssClasses.add(MESSAGE_CSS_CLASS);
    }

    html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses));

    Optional.ofNullable(getMessageCssClass(login)).ifPresent(cssClasses::add);

    String text = htmlEscaper().escape(chatMessage.getMessage()).replace("\\", "\\\\");
    text = convertUrlsToHyperlinks(text);
    text = replaceChannelNamesWithHyperlinks(text);

    Matcher matcher = mentionPattern.matcher(text);
    if (matcher.find()) {
      text = matcher.replaceAll("<span class='self'>" + matcher.group(1) + "</span>");
      onMention(chatMessage);
    }

    return html
        .replace("{css-classes}", Joiner.on(' ').join(cssClasses))
        .replace("{inline-style}", getInlineStyle(login))
        // Always replace text last in case the message contains one of the placeholders.
        .replace("{text}", text);
  }

  @VisibleForTesting
  protected String replaceChannelNamesWithHyperlinks(String text) {
    Matcher channelMatcher = CHANNEL_USER_PATTERN.matcher(text);
    while (channelMatcher.find()) {
      String channelName = channelMatcher.group(CHANNEL_NAME_GROUP_NAME);
      text = text.replace(channelName, "<a href=\"javascript:void(0);\" onClick=\"java.openChannel('" + channelName + "')\">" + channelName + "</a>");
    }
    return text;
  }

  protected void onMention(ChatMessage chatMessage) {
    // Default implementation does nothing
  }

  protected void showNotificationIfNecessary(ChatMessage chatMessage) {
    Stage stage = StageHolder.getStage();
    if (stage.isFocused() && stage.isShowing()) {
      return;
    }

    Optional<Player> playerOptional = playerService.getPlayerForUsername(chatMessage.getUsername());
    String identIconSource = playerOptional.map(player -> String.valueOf(player.getId())).orElseGet(chatMessage::getUsername);

    if (preferencesService.getPreferences().getNotification().isPrivateMessageToastEnabled()) {
      notificationService.addNotification(new TransientNotification(
          chatMessage.getUsername(),
          chatMessage.getMessage(),
          IdenticonUtil.createIdenticon(identIconSource),
          event -> {
            eventBus.post(new NavigateEvent(NavigationItem.CHAT));
            stage.toFront();
            getRoot().getTabPane().getSelectionModel().select(getRoot());
          })
      );
    }
  }

  protected String getMessageCssClass(String login) {
    Optional<Player> playerOptional = playerService.getPlayerForUsername(login);
    if (!playerOptional.isPresent()) {
      return CSS_CLASS_CHAT_ONLY;
    }

    return playerOptional.get().getSocialStatus().getCssClass();
  }

  protected String getInlineStyle(String username) {
    // To be overridden by subclasses
    return "";
  }

  @VisibleForTesting
  String createInlineStyleFromColor(Color messageColor) {
    return String.format("color: %s;", JavaFxUtil.toRgbCode(messageColor));
  }

  protected String convertUrlsToHyperlinks(String text) {
    return (String) engine.executeScript("link('" + text.replace("'", "\\'") + "')");
  }

  private void insertIntoContainer(String html, String containerId) {
    ((JSObject) engine.executeScript("document.getElementById('" + containerId + "')"))
        .call("insertAdjacentHTML", "beforeend", html);
    getMessagesWebView().requestLayout();
  }

  /**
   * Subclasses may override in order to perform actions when the view is being displayed.
   */
  protected void onDisplay() {
    messageTextField().requestFocus();
  }

  /**
   * Subclasses may override in order to perform actions when the view is no longer being displayed.
   */
  protected void onHide() {

  }
}
