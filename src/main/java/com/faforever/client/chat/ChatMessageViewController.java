package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.exception.AssetLoadException;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.NodeController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.ConcurrentUtil;
import com.faforever.client.util.PopupUtil;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;
import javafx.stage.PopupWindow.AnchorLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.faforever.client.theme.ThemeService.CHAT_CONTAINER;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_EXTENDED;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_EXTENDED;
import static com.google.common.html.HtmlEscapers.htmlEscaper;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Locale.US;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * A chat tab displays messages in a {@link WebView}. The WebView is used since text on a JavaFX canvas isn't
 * selectable, but text within a WebView is. This comes with some ugly implications; some logic has to be performed in
 * interaction with JavaScript, like when the user clicks a link.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChatMessageViewController extends NodeController<VBox> {

  private static final String MESSAGE_CONTAINER_ID = "chat-container";
  private static final String MESSAGE_ITEM_CLASS = "chat-section";

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
  /**
   * Added if a message is what IRC calls an "onAction".
   */
  private static final String ACTION_CSS_CLASS = "onAction";
  private static final String MESSAGE_CSS_CLASS = "message";

  private final TimeService timeService;
  private final NotificationService notificationService;
  private final ThemeService themeService;
  private final WebViewConfigurer webViewConfigurer;
  private final EmoticonService emoticonService;
  private final CountryFlagService countryFlagService;
  private final ChatService chatService;
  private final ChatPrefs chatPrefs;
  private final FxApplicationThreadExecutor fxApplicationThreadExecutor;

  public Button emoticonsButton;
  public TextField messageTextField;
  public WebView messagesWebView;
  public VBox root;
  public Node emoticonsWindow;
  public Label typingLabel;
  public EmoticonsWindowController emoticonsWindowController;

  /**
   * Messages that arrived before the web view was ready. Those are appended as soon as it is ready.
   */
  private final List<ChatMessage> waitingMessages = new ArrayList<>();
  private final List<String> userMessageHistory = new ArrayList<>();
  private final ObjectProperty<ChatChannel> chatChannel = new SimpleObjectProperty<>();
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers);
  private final ReadOnlyStringWrapper chatMessageTextHtml = new ReadOnlyStringWrapper();
  private final ReadOnlyStringWrapper chatMessageSectionHtml = new ReadOnlyStringWrapper();
  private final Consumer<ChatMessage> messageListener = this::onChatMessage;
  private final ListChangeListener<ChatChannelUser> channelUserListChangeListener = this::updateChangedUsersStyles;
  private final ListChangeListener<ChatChannelUser> typingUserListChangeListener = this::updateTypingUsersLabel;

  private int lastEntryId;
  private boolean isChatReady;

  private ChatMessage lastMessage;
  private WebEngine engine;
  private Popup emoticonsPopup;

  private String currentUserMessage = "";
  private int curMessageHistoryIndex = 0;

  @VisibleForTesting
  Pattern mentionPattern;

  @Override
  protected void onInitialize() {
    JavaFxUtil.bindManagedToVisible(typingLabel);

    mentionPattern = Pattern.compile(
        "(^|[^A-Za-z0-9-])" + Pattern.quote(chatService.getCurrentUsername()) + "([^A-Za-z0-9-]|$)", CASE_INSENSITIVE);
    engine = messagesWebView.getEngine();

    chatMessageTextHtml.bind(chatPrefs.chatFormatProperty().map(chatFormat -> switch (chatFormat) {
      case COMPACT -> CHAT_TEXT_COMPACT;
      case EXTENDED -> CHAT_TEXT_EXTENDED;
    }).map(themeFile -> {
      try {
        return themeService.getThemeFileUrl(themeFile);
      } catch (IOException e) {
        return null;
      }
    }).map(url -> {
      try (Reader reader = new InputStreamReader(url.openStream())) {
        return CharStreams.toString(reader);
      } catch (IOException e) {
        return null;
      }
    }).orElse(""));

    chatMessageSectionHtml.bind(chatPrefs.chatFormatProperty().map(chatFormat -> switch (chatFormat) {
      case COMPACT -> CHAT_SECTION_COMPACT;
      case EXTENDED -> CHAT_SECTION_EXTENDED;
    }).map(themeFile -> {
      try {
        return themeService.getThemeFileUrl(themeFile);
      } catch (IOException e) {
        return null;
      }
    }).map(url -> {
      try (Reader reader = new InputStreamReader(url.openStream())) {
        return CharStreams.toString(reader);
      } catch (IOException e) {
        return null;
      }
    }).orElse(""));

    initChatView();

    messageTextField.setOnKeyPressed(this::handleKeyEvent);
    messageTextField.textProperty().subscribe(this::updateTypingState);

    currentUserMessage = "";
    curMessageHistoryIndex = 0;

    chatPrefs.hideFoeMessagesProperty().when(showing).subscribe(this::hideFoeMessages);
    chatPrefs.chatColorModeProperty().when(showing).subscribe(() -> {
      ObservableList<ChatChannelUser> users = this.users.getValue();
      if (users != null) {
        users.forEach(this::updateUserMessageColor);
      }
    });

    chatChannel.when(attached).subscribe(((oldValue, newValue) -> {
      userMessageHistory.clear();
      if (oldValue != null) {
        oldValue.removeMessageListener(messageListener);
        oldValue.removeUserListener(channelUserListChangeListener);
        oldValue.removeTypingUserListener(typingUserListChangeListener);
      }
      if (newValue != null) {
        newValue.addMessageListener(messageListener);
        newValue.addUsersListeners(channelUserListChangeListener);
        newValue.addTypingUsersListener(typingUserListChangeListener);
      }
    }));

    emoticonsWindowController.setTextInputControl(messageTextField);
    emoticonsPopup = PopupUtil.createPopup(AnchorLocation.WINDOW_BOTTOM_RIGHT, emoticonsWindow);
    emoticonsPopup.setConsumeAutoHidingEvents(false);

    createAutoCompletionHelper().bindTo(messageTextField);
  }

  private AutoCompletionHelper createAutoCompletionHelper() {
    return new AutoCompletionHelper(currentWord -> {
      ObservableList<ChatChannelUser> users = this.users.getValue();
      return users == null ? List.of() : users.stream()
                                              .map(ChatChannelUser::getUsername)
                                              .filter(username -> username.toLowerCase(US)
                                                                          .startsWith(currentWord.toLowerCase()))
                                              .sorted()
                                              .collect(Collectors.toList());
    });
  }

  private void updateTypingState() {
    ChatChannel channel = chatChannel.get();
    if (!messageTextField.getText().isEmpty()) {
      chatService.setActiveTypingState(channel);
    } else if (!messageTextField.isDisabled()) {
      chatService.setDoneTypingState(channel);
    }
  }

  @Override
  public void onAttached() {
    addAttachedSubscription(StageHolder.getStage().focusedProperty().subscribe(messageTextField::requestFocus));
  }

  @Override
  public void onDetached() {
    ChatChannel channel = chatChannel.get();
    if (channel != null) {
      channel.removeUserListener(channelUserListChangeListener);
      channel.removeMessageListener(messageListener);
    }
  }

  private void handleKeyEvent(KeyEvent event) {
    updateMessageWithHistory(event);
  }

  private void updateMessageWithHistory(KeyEvent event) {
    if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP) {
      if (curMessageHistoryIndex == 0) {
        currentUserMessage = messageTextField.getText();
      }

      switch (event.getCode()) {
        case DOWN -> curMessageHistoryIndex--;
        case UP -> curMessageHistoryIndex++;
      }

      int userMessageCount = userMessageHistory.size();
      if (curMessageHistoryIndex <= 0) {
        curMessageHistoryIndex = 0;
        messageTextField.setText(currentUserMessage);
      } else if (curMessageHistoryIndex <= userMessageCount) {
        messageTextField.setText(userMessageHistory.get(userMessageCount - curMessageHistoryIndex));
      } else {
        curMessageHistoryIndex = userMessageCount;
      }

      messageTextField.positionCaret(messageTextField.getText().length());
    } else {
      curMessageHistoryIndex = 0;
    }
  }

  @Override
  public VBox getRoot() {
    return root;
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

  private void initChatView() {
    webViewConfigurer.configureWebView(messagesWebView);

    messagesWebView.zoomProperty().bindBidirectional(chatPrefs.zoomProperty());

    engine.getLoadWorker().stateProperty().subscribe(this::sendWaitingMessagesIfLoaded);
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

  private void sendWaitingMessagesIfLoaded(State newValue) {
    if (newValue != State.SUCCEEDED) {
      return;
    }

    synchronized (waitingMessages) {
      waitingMessages.forEach(ChatMessageViewController.this::addMessage);
      waitingMessages.clear();
      isChatReady = true;
      scrollToBottomIfDesired();
    }
  }

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

  private void updateUserMessageColor(ChatChannelUser user) {
    String color = user.getColor().map(JavaFxUtil::toRgbCode).orElse("");
    fxApplicationThreadExecutor.execute(() -> callJsMethod("updateUserMessageColor", user.getUsername(), color));
  }

  private void updateUserMessageVisibility(ChatChannelUser user, boolean shouldHide) {
    String displayPropertyValue = shouldHide ? "none" : "";
    fxApplicationThreadExecutor.execute(
        () -> callJsMethod("updateUserMessageDisplay", user.getUsername(), displayPropertyValue));
  }

  public void onSendMessage() {
    String text = messageTextField.getText();
    if (StringUtils.isEmpty(text)) {
      return;
    }

    updateUserMessageHistory(text);
    sendMessage();
    hideEmoticonsWindow();
  }

  private void updateUserMessageHistory(String text) {
    if (userMessageHistory.size() >= 50) {
      userMessageHistory.removeFirst();
      userMessageHistory.add(text);
    } else {
      userMessageHistory.add(text);
    }
  }

  private void hideEmoticonsWindow() {
    emoticonsPopup.hide();
  }

  private void sendMessage() {
    messageTextField.setDisable(true);

    final String text = messageTextField.getText();
    CompletableFuture<Void> sendFuture;
    if (text.startsWith(ACTION_PREFIX)) {
      sendFuture = chatService.sendActionInBackground(chatChannel.get(),
                                                      text.replaceFirst(Pattern.quote(ACTION_PREFIX), ""));
    } else {
      sendFuture = chatService.sendMessageInBackground(chatChannel.get(), text);
    }

    sendFuture.whenComplete((result, throwable) -> {
      if (throwable != null) {
        throwable = ConcurrentUtil.unwrapIfCompletionException(throwable);
        log.warn("Message could not be sent: {}", text, throwable);
        notificationService.addImmediateErrorNotification(throwable, "chat.sendFailed");
      }
    }).whenCompleteAsync((result, throwable) -> {
      if (throwable == null) {
        messageTextField.clear();
      }
      messageTextField.setDisable(false);
      messageTextField.requestFocus();
    }, fxApplicationThreadExecutor);
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
    if (!lastMessage.sender().getUsername().equals(chatMessage.sender().getUsername())) {
      return true;
    }
    if (lastMessage.time().isBefore(chatMessage.time().minus(1, MINUTES))) {
      return true;
    }
    return lastMessage.action();
  }

  private void appendMessage(ChatMessage chatMessage) throws IOException {
    String html = fillTemplateHtml(chatMessage, chatMessageTextHtml.get(), null);

    insertIntoContainer(html, "chat-section-" + lastEntryId);
  }

  private void appendChatMessageSection(ChatMessage chatMessage) throws IOException {
    String html = fillTemplateHtml(chatMessage, chatMessageSectionHtml.get(), ++lastEntryId);
    insertIntoContainer(html, MESSAGE_CONTAINER_ID);
    appendMessage(chatMessage);
  }

  private String fillTemplateHtml(ChatMessage chatMessage, String htmlTemplate,
                                  @Nullable Integer sectionId) throws IOException {
    ChatChannelUser sender = chatMessage.sender();
    String username = sender.getUsername();

    Optional<PlayerBean> playerOptional = sender.getPlayer();
    Optional<String> clanOptional = playerOptional.map(PlayerBean::getClan);

    String avatarUrl = playerOptional.map(PlayerBean::getAvatar).map(AvatarBean::getUrl).map(URL::toString).orElse("");
    String countryFlagUrl = playerOptional.map(PlayerBean::getCountry)
                                          .flatMap(countryFlagService::getCountryFlagUrl)
                                          .map(URL::toString)
                                          .orElse("");
    String clanTag = clanOptional.orElse("");
    String decoratedClanTag = clanOptional.map("[%s]"::formatted).orElse("");

    String timeString = timeService.asShortTime(chatMessage.time());
    String html = htmlTemplate.replace("{time}", timeString)
                              .replace("{avatar}", avatarUrl)
                              .replace("{username}", username)
                              .replace("{clan-tag}", clanTag)
                              .replace("{decorated-clan-tag}", decoratedClanTag)
                              .replace("{country-flag}", StringUtils.defaultString(countryFlagUrl))
                              .replace("{section-id}", String.valueOf(sectionId));

    Collection<String> cssClasses = new ArrayList<>();
    cssClasses.add(String.format("user-%s", username));
    if (chatMessage.action()) {
      cssClasses.add(ACTION_CSS_CLASS);
    } else {
      cssClasses.add(MESSAGE_CSS_CLASS);
    }

    html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses));

    String text = htmlEscaper().escape(chatMessage.message()).replace("\\", "\\\\");
    text = convertUrlsToHyperlinks(text);
    text = replaceChannelNamesWithHyperlinks(text);
    text = transformEmoticonShortcodesToImages(text);

    Matcher matcher = mentionPattern.matcher(text);
    if (matcher.find()) {
      text = matcher.replaceAll("<span class='self'>" + matcher.group() + "</span>");
    }

    return html.replace("{inline-style}", getInlineStyle(sender))
               // Always replace text last in case the message contains one of the placeholders.
               .replace("{text}", text);
  }

  @VisibleForTesting
  String transformEmoticonShortcodesToImages(String text) {
    return emoticonService.getEmoticonShortcodeDetectorPattern()
                          .matcher(text)
                          .replaceAll((matchResult) -> String.format(EMOTICON_IMG_TEMPLATE,
                                                                     emoticonService.getBase64SvgContentByShortcode(
                                                                         matchResult.group())));
  }

  @VisibleForTesting
  String replaceChannelNamesWithHyperlinks(String text) {
    return CHANNEL_USER_PATTERN.matcher(text).replaceAll(matchResult -> {
      String channelName = matchResult.group();
      return (channelName.startsWith(" ") ? " " : "") + transformToChannelLinkHtml(channelName);
    });
  }

  @VisibleForTesting
  String transformToChannelLinkHtml(String channelName) {
    return String.format(CHANNEL_LINK_HTML_FORMAT, channelName.strip());
  }

  @VisibleForTesting
  String getInlineStyle(ChatChannelUser chatChannelUser) {
    if (chatPrefs.isHideFoeMessages() && chatChannelUser.getCategory() == ChatUserCategory.FOE) {
      return "display: none;";
    } else {
      return chatChannelUser.getColor()
                            .map(color -> String.format("color: %s;", JavaFxUtil.toRgbCode(color)))
                            .orElse("");
    }
  }

  protected String convertUrlsToHyperlinks(String text) {
    JavaFxUtil.assertApplicationThread();
    return (String) engine.executeScript("link('" + text.replace("'", "\\'") + "')");
  }

  private void insertIntoContainer(String html, String containerId) {
    ((JSObject) engine.executeScript("document.getElementById('" + containerId + "')")).call("insertAdjacentHTML",
                                                                                             "beforeend", html);
    messagesWebView.requestLayout();
  }

  private void updateChangedUsersStyles(Change<? extends ChatChannelUser> change) {
    while (change.next()) {
      if (change.wasUpdated()) {
        List<ChatChannelUser> changedUsers = List.copyOf(change.getList().subList(change.getFrom(), change.getTo()));
        for (ChatChannelUser user : changedUsers) {
          updateUserMessageVisibility(user, user.getCategory() == ChatUserCategory.FOE);
          updateUserMessageColor(user);
        }
      }
    }
  }

  private void updateTypingUsersLabel(Change<? extends ChatChannelUser> change) {
    List<ChatChannelUser> typingUsers = List.copyOf(change.getList());
    typingLabel.setVisible(!typingUsers.isEmpty());
    typingLabel.setText(
        String.join(", ", typingUsers.stream().map(ChatChannelUser::getUsername).toList()) + " is typing");
  }

  private void hideFoeMessages(boolean shouldHide) {
    ObservableList<ChatChannelUser> users = this.users.getValue();
    if (users != null) {
      users.stream()
           .filter(user -> user.getCategory() == ChatUserCategory.FOE)
           .forEach(user -> updateUserMessageVisibility(user, shouldHide));
    }
  }

  public void openEmoticonsPopupWindow() {
    Bounds screenBounds = emoticonsButton.localToScreen(emoticonsButton.getBoundsInLocal());
    double anchorX = screenBounds.getMaxX() - 5;
    double anchorY = screenBounds.getMinY() - 5;

    messageTextField.requestFocus();
    emoticonsPopup.show(emoticonsButton.getScene().getWindow(), anchorX, anchorY);
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
