package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.TabController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.theme.ThemeService.CHAT_CONTAINER;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_EXTENDED;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_EXTENDED;
import static com.google.common.html.HtmlEscapers.htmlEscaper;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static javafx.scene.AccessibleAttribute.ITEM_AT_INDEX;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some logic has to be performed in
 * interaction with JavaScript, like when the user clicks a link.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractChatTabController extends TabController {

  static final String CSS_CLASS_CHAT_ONLY = "chat_only";
  private static final String MESSAGE_CONTAINER_ID = "chat-container";
  private static final String MESSAGE_ITEM_CLASS = "chat-section";
  private static final PseudoClass UNREAD_PSEUDO_STATE = PseudoClass.getPseudoClass("unread");
  private static final org.springframework.core.io.Resource CHAT_JS_RESOURCE = new ClassPathResource(
      "/js/chat_container.js");
  private static final org.springframework.core.io.Resource AUTOLINKER_JS_RESOURCE = new ClassPathResource(
      "/js/Autolinker.min.js");
  private static final org.springframework.core.io.Resource JQUERY_JS_RESOURCE = new ClassPathResource(
      "js/jquery-2.1.4.min.js");
  private static final org.springframework.core.io.Resource JQUERY_HIGHLIGHT_JS_RESOURCE = new ClassPathResource(
      "js/jquery.highlight-5.closure.js");
  private static final String CHANNEL_LINK_HTML_FORMAT = "<a href=\"javascript:void(0);\" onClick=\"java.openChannel('%1$s')\">%1$s</a>";

  /**
   * A pattern identifying all strings with a # in front and not starting with a number. Those are interpreted as
   * irc-channels.
   */
  private static final Pattern CHANNEL_USER_PATTERN = Pattern.compile("(^|\\s)#[a-zA-Z]\\S+", CASE_INSENSITIVE);

  private static final String EMOTICON_IMG_TEMPLATE = "<img src=\"data:image/svg+xml;base64,%s\" width=\"24\" height=\"24\" />";

  private static final String ACTION_PREFIX = "/me ";
  private static final String JOIN_PREFIX = "/join ";
  private static final String WHOIS_PREFIX = "/whois ";
  /**
   * Added if a message is what IRC calls an "action".
   */
  private static final String ACTION_CSS_CLASS = "action";
  private static final String MESSAGE_CSS_CLASS = "message";

  protected final LoginService loginService;
  protected final ChatService chatService;
  protected final PlayerService playerService;
  protected final TimeService timeService;
  protected final I18n i18n;
  protected final NotificationService notificationService;
  protected final UiService uiService;
  protected final ThemeService themeService;
  protected final WebViewConfigurer webViewConfigurer;
  protected final EmoticonService emoticonService;
  protected final CountryFlagService countryFlagService;
  protected final ChatPrefs chatPrefs;
  protected final NotificationPrefs notificationPrefs;
  protected final FxApplicationThreadExecutor fxApplicationThreadExecutor;
  protected final NavigationHandler navigationHandler;

  /**
   * Messages that arrived before the web view was ready. Those are appended as soon as it is ready.
   */
  private final List<ChatMessage> waitingMessages = new ArrayList<>();
  private final IntegerProperty unreadMessagesCount = new SimpleIntegerProperty();
  protected final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  protected final ObservableValue<String> channelName = chatChannel.map(ChatChannel::getName);

  private final Consumer<ChatMessage> messageListener = this::onChatMessage;

  private int lastEntryId;
  private boolean isChatReady;

  public Button emoticonsButton;
  @VisibleForTesting
  protected WeakReference<Popup> emoticonsPopupWindowWeakReference;

  private ChatMessage lastMessage;
  private WebEngine engine;

  @VisibleForTesting
  Pattern mentionPattern;

  @Override
  protected void onInitialize() {
    mentionPattern = Pattern.compile(
        "(^|[^A-Za-z0-9-])" + Pattern.quote(loginService.getUsername()) + "([^A-Za-z0-9-]|$)", CASE_INSENSITIVE);

    initChatView();

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.openProperty().unbind();
        oldValue.removeMessageListener(messageListener);
      }
      if (newValue != null) {
        newValue.openProperty().bind(showing.and(StageHolder.getStage().focusedProperty()).when(attached));
        newValue.addMessageListener(messageListener);
      }
    }));

    showing.subscribe(selected -> {
      clearUnreadIfFocused();
      if (selected) {
        // We have to request focus after the tab is fully added to the scene which happens sometime after selection
        fxApplicationThreadExecutor.runLater(messageTextField()::requestFocus);
      }
    });

    unreadMessagesCount.subscribe(
        (oldValue, newValue) -> incrementUnreadMessageCount(newValue.intValue() - oldValue.intValue()));
    getRoot().setOnClosed(this::onClosed);
  }

  @Override
  public void onAttached() {
    addAttachedSubscription(StageHolder.getStage().focusedProperty().subscribe(() -> {
      clearUnreadIfFocused();
      messageTextField().requestFocus();
    }));
  }

  @Override
  public void onDetached() {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      channel.openProperty().unbind();
      channel.removeMessageListener(messageListener);
    }
  }

  private void clearUnreadIfFocused() {
    if (hasFocus()) {
      setUnread(false);
    }
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
    if (tabPane == null) {
      return false;
    }

    if (!JavaFxUtil.isVisibleRecursively(tabPane)) {
      return false;
    }

    Window window = tabPane.getScene().getWindow();
    return window.isFocused() && window.isShowing();
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
    Node tabSkin = (Node) skin.queryAccessibleAttribute(ITEM_AT_INDEX, tabIndex);
    tabSkin.pseudoClassStateChanged(UNREAD_PSEUDO_STATE, unread);

    if (!unread) {
      synchronized (unreadMessagesCount) {
        unreadMessagesCount.setValue(0);
      }
    }
  }

  @Override
  public abstract Tab getRoot();

  protected void incrementUnreadMessagesCount() {
    synchronized (unreadMessagesCount) {
      unreadMessagesCount.set(unreadMessagesCount.get() + 1);
    }
  }

  public ChatChannel getChatChannel() {
    return chatChannel.get();
  }

  public void setChatChannel(ChatChannel chatChannel) {
    this.chatChannel.set(chatChannel);
  }

  public ObjectProperty<ChatChannel> chatChannelProperty() {
    return chatChannel;
  }

  private void incrementUnreadMessageCount(int delta) {
    chatService.incrementUnreadMessagesCount(delta);
  }

  protected void onClosed(Event event) {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      chatService.leaveChannel(channel);
      channel.removeMessageListener(messageListener);
    }
    messageTextField().setOnKeyReleased(null);
  }

  protected abstract TextInputControl messageTextField();

  private void initChatView() {
    WebView messagesWebView = getMessagesWebView();
    webViewConfigurer.configureWebView(messagesWebView);

    messagesWebView.zoomProperty().bindBidirectional(chatPrefs.zoomProperty());

    configureBrowser(messagesWebView);
    loadChatContainer();
  }

  private void loadChatContainer() {
    try (Reader reader = new InputStreamReader(themeService.getThemeFileUrl(CHAT_CONTAINER).openStream())) {
      String chatContainerHtml = CharStreams.toString(reader)
                                            .replace("{chat-container-js}", CHAT_JS_RESOURCE.getURL().toExternalForm())
                                            .replace("{auto-linker-js}",
                                                     AUTOLINKER_JS_RESOURCE.getURL().toExternalForm())
                                            .replace("{jquery-js}", JQUERY_JS_RESOURCE.getURL().toExternalForm())
                                            .replace("{jquery-highlight-js}",
                                                     JQUERY_HIGHLIGHT_JS_RESOURCE.getURL().toExternalForm());

      engine.loadContent(chatContainerHtml);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void configureBrowser(WebView messagesWebView) {
    engine = messagesWebView.getEngine();

    configureLoadListener();
  }

  private void configureLoadListener() {
    engine.getLoadWorker()
          .stateProperty()
          .addListener((observable, oldValue, newValue) -> sendWaitingMessagesIfLoaded(newValue));
  }

  private void sendWaitingMessagesIfLoaded(State newValue) {
    if (newValue != State.SUCCEEDED) {
      return;
    }

    synchronized (waitingMessages) {
      waitingMessages.forEach(AbstractChatTabController.this::addMessage);
      waitingMessages.clear();
      scrollToBottomIfDesired();
      isChatReady = true;
    }
  }

  protected abstract WebView getMessagesWebView();

  protected JSObject getJsObject() {
    return (JSObject) engine.executeScript("window");
  }

  protected void callJsMethod(String methodName, Object... args) {
    try {
      getJsObject().call(methodName, args);
    } catch (Exception e) {
      log.warn("Error when calling JS method: ", e);
    }
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

    hideEmoticonsWindow();
  }

  private void hideEmoticonsWindow() {
    if (emoticonsPopupWindowWeakReference != null) {
      PopupWindow window = emoticonsPopupWindowWeakReference.get();
      if (window != null && window.isShowing()) {
        window.hide();
      }
    }
  }

  private void sendMessage() {
    TextInputControl messageTextField = messageTextField();
    messageTextField.setDisable(true);

    final String text = messageTextField.getText();
    chatService.sendMessageInBackground(chatChannel.get(), text).thenRunAsync(() -> {
      messageTextField.clear();
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
    }, fxApplicationThreadExecutor).exceptionally(throwable -> {
      throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
      log.warn("Message could not be sent: {}", text, throwable);
      notificationService.addImmediateErrorNotification(throwable, "chat.sendFailed");
      fxApplicationThreadExecutor.execute(() -> {
        messageTextField.setDisable(false);
        messageTextField.requestFocus();
      });
      return null;
    });
  }

  private void sendAction(final TextInputControl messageTextField, final String text) {
    messageTextField.setDisable(true);

    chatService.sendActionInBackground(chatChannel.get(), text.replaceFirst(Pattern.quote(ACTION_PREFIX), ""))
               .thenRunAsync(() -> {
                 messageTextField.clear();
                 messageTextField.setDisable(false);
                 messageTextField.requestFocus();
               }, fxApplicationThreadExecutor)
               .exceptionally(throwable -> {
                 throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
                 log.warn("Message could not be sent: {}", text, throwable);
                 notificationService.addImmediateErrorNotification(throwable, "chat.sendFailed");
                 fxApplicationThreadExecutor.execute(() -> messageTextField.setDisable(false));
                 return null;
               });
  }

  protected void onChatMessage(ChatMessage chatMessage) {
    synchronized (waitingMessages) {
      if (!isChatReady) {
        waitingMessages.add(chatMessage);
      } else {
        fxApplicationThreadExecutor.execute(() -> {
          addMessage(chatMessage);
          removeTopmostMessages();
          scrollToBottomIfDesired();
        });
      }
    }
  }

  private void scrollToBottomIfDesired() {
    JavaFxUtil.assertApplicationThread();
    engine.executeScript("scrollToBottomIfDesired()");
  }

  private void removeTopmostMessages() {
    JavaFxUtil.assertApplicationThread();
    int maxMessageItems = chatPrefs.getMaxMessages();

    int numberOfMessages = (int) engine.executeScript(
        "document.getElementsByClassName('" + MESSAGE_ITEM_CLASS + "').length");
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
    JavaFxUtil.assertApplicationThread();
    try {
      if (requiresNewChatSection(chatMessage)) {
        appendChatMessageSection(chatMessage);
      } else {
        appendMessage(chatMessage);
      }
      lastMessage = chatMessage;
    } catch (IOException e) {
      throw new AssetLoadException("Could not add message", e, "chat.message.addError");
    }
  }

  private boolean requiresNewChatSection(ChatMessage chatMessage) {
    if (lastMessage == null) {
      return true;
    }
    if (!lastMessage.username().equals(chatMessage.username())) {
      return true;
    }
    if (lastMessage.time().isBefore(chatMessage.time().minus(1, MINUTES))) {
      return true;
    }
    return lastMessage.action();
  }

  private void appendMessage(ChatMessage chatMessage) throws IOException {
    URL themeFileUrl;
    if (chatPrefs.getChatFormat() == ChatFormat.COMPACT) {
      themeFileUrl = themeService.getThemeFileUrl(CHAT_TEXT_COMPACT);
    } else {
      themeFileUrl = themeService.getThemeFileUrl(CHAT_TEXT_EXTENDED);
    }

    String html = renderHtml(chatMessage, themeFileUrl, null);

    insertIntoContainer(html, "chat-section-" + lastEntryId);
  }

  private void appendChatMessageSection(ChatMessage chatMessage) throws IOException {
    URL themeFileURL;
    if (chatPrefs.getChatFormat() == ChatFormat.COMPACT) {
      themeFileURL = themeService.getThemeFileUrl(CHAT_SECTION_COMPACT);
    } else {
      themeFileURL = themeService.getThemeFileUrl(CHAT_SECTION_EXTENDED);
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

    String username = chatMessage.username();

    Optional<PlayerBean> playerOptional = playerService.getPlayerByNameIfOnline(chatMessage.username());
    Optional<String> clanOptional = playerOptional.map(PlayerBean::getClan);

    String avatarUrl = playerOptional.map(PlayerBean::getAvatar).map(AvatarBean::getUrl).map(URL::toString).orElse("");
    String countryFlagUrl = playerOptional.map(PlayerBean::getCountry)
                                          .flatMap(countryFlagService::getCountryFlagUrl)
                                          .map(URL::toString)
                                          .orElse("");
    String clanTag = clanOptional.orElse("");
    String decoratedClanTag = clanOptional.map(tag -> i18n.get("chat.clanTagFormat", tag)).orElse("");

    String timeString = timeService.asShortTime(chatMessage.time());
    html = html.replace("{time}", timeString)
               .replace("{avatar}", avatarUrl)
               .replace("{username}", username)
               .replace("{clan-tag}", clanTag)
               .replace("{decorated-clan-tag}", decoratedClanTag)
               .replace("{country-flag}", StringUtils.defaultString(countryFlagUrl))
               .replace("{section-id}", String.valueOf(sectionId));

    Collection<String> cssClasses = new ArrayList<>();
    cssClasses.add(String.format("user-%s", chatMessage.username()));
    if (chatMessage.action()) {
      cssClasses.add(ACTION_CSS_CLASS);
    } else {
      cssClasses.add(MESSAGE_CSS_CLASS);
    }

    html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses));

    Optional.ofNullable(getMessageCssClass(username)).ifPresent(cssClasses::add);

    String text = htmlEscaper().escape(chatMessage.message()).replace("\\", "\\\\");
    text = convertUrlsToHyperlinks(text);
    text = replaceChannelNamesWithHyperlinks(text);
    text = transformEmoticonShortcodesToImages(text);

    Matcher matcher = mentionPattern.matcher(text);
    if (matcher.find()) {
      text = matcher.replaceAll("<span class='self'>" + matcher.group() + "</span>");
      onMention(chatMessage);
    }

    return html.replace("{css-classes}", Joiner.on(' ').join(cssClasses))
               .replace("{inline-style}", getInlineStyle(username))
               // Always replace text last in case the message contains one of the placeholders.
               .replace("{text}", text);
  }

  @VisibleForTesting
  protected String transformEmoticonShortcodesToImages(String text) {
    return emoticonService.getEmoticonShortcodeDetectorPattern()
                          .matcher(text)
                          .replaceAll((matchResult) -> String.format(EMOTICON_IMG_TEMPLATE,
                                                                     emoticonService.getBase64SvgContentByShortcode(
                                                                         matchResult.group())));
  }

  @VisibleForTesting
  protected String replaceChannelNamesWithHyperlinks(String text) {
    return CHANNEL_USER_PATTERN.matcher(text).replaceAll(matchResult -> {
      String channelName = matchResult.group();
      return (channelName.startsWith(" ") ? " " : "") + transformToChannelLinkHtml(channelName);
    });
  }

  @VisibleForTesting
  String transformToChannelLinkHtml(String channelName) {
    return String.format(CHANNEL_LINK_HTML_FORMAT, channelName.strip());
  }

  protected void onMention(ChatMessage chatMessage) {
    // Default implementation does nothing
  }

  protected String getMessageCssClass(String login) {
    Optional<PlayerBean> playerOptional = playerService.getPlayerByNameIfOnline(login);
    if (playerOptional.isEmpty()) {
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
    JavaFxUtil.assertApplicationThread();
    return (String) engine.executeScript("link('" + text.replace("'", "\\'") + "')");
  }

  private void insertIntoContainer(String html, String containerId) {
    ((JSObject) engine.executeScript("document.getElementById('" + containerId + "')")).call("insertAdjacentHTML",
                                                                                             "beforeend", html);
    getMessagesWebView().requestLayout();
  }

  /**
   * Subclasses may override in order to perform actions when the view is being displayed.
   */
  protected void onDisplay() {
  }

  public void openEmoticonsPopupWindow() {
    Bounds screenBounds = emoticonsButton.localToScreen(emoticonsButton.getBoundsInLocal());
    double anchorX = screenBounds.getMaxX() - 5;
    double anchorY = screenBounds.getMinY() - 5;

    if (emoticonsPopupWindowWeakReference != null) {
      PopupWindow window = emoticonsPopupWindowWeakReference.get();
      if (window != null) {
        window.show(emoticonsButton.getScene().getWindow(), anchorX, anchorY);
        messageTextField().requestFocus();
        return;
      }
    }

    EmoticonsWindowController controller = uiService.loadFxml("theme/chat/emoticons/emoticons_window.fxml");
    controller.setTextInputControl(messageTextField());
    messageTextField().requestFocus();
    Popup window = PopupUtil.createPopup(AnchorLocation.WINDOW_BOTTOM_RIGHT, controller.getRoot());
    window.setConsumeAutoHidingEvents(false);
    window.show(emoticonsButton.getScene().getWindow(), anchorX, anchorY);
    emoticonsPopupWindowWeakReference = new WeakReference<>(window);
  }

  @VisibleForTesting
  String getHtmlBodyContent() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    String[] content = new String[1];
    fxApplicationThreadExecutor.execute(() -> {
      try {
        content[0] = (String) engine.executeScript("document.body.innerHTML");
      } finally {
        latch.countDown();
      }
    });
    latch.await();
    return content[0];
  }
}
