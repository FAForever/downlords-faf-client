package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.UrlPreviewResolver.Preview;
import com.faforever.client.clan.ClanService;
import com.faforever.client.clan.ClanTooltipController;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.NavigateEvent;
import com.faforever.client.main.event.NavigationItem;
import com.faforever.client.notification.DismissAction;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.replay.ExternalReplayInfoGenerator;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.IdenticonUtil;
import com.faforever.client.util.TimeService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.eventbus.EventBus;
import com.google.common.io.CharStreams;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Worker;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
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
import javafx.stage.PopupWindow.AnchorLocation;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static com.faforever.client.theme.UiService.CHAT_ENTRY;
import static com.faforever.client.theme.UiService.CHAT_TEXT;
import static com.faforever.client.util.RatingUtil.getGlobalRating;
import static com.faforever.client.util.RatingUtil.getLeaderboardRating;
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

  /**
   * This is the member name within the JavaScript code that provides access to this chat tab instance.
   */
  private static final String CHAT_TAB_REFERENCE_IN_JAVASCRIPT = "chatTab";
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
  protected final PlatformService platformService;
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
  protected final ExternalReplayInfoGenerator externalReplayInfoGenerator;
  protected final ImageUploadService imageUploadService;
  protected final UrlPreviewResolver urlPreviewResolver;
  protected final AutoCompletionHelper autoCompletionHelper;
  protected final ClanService clanService;
  private final CountryFlagService countryFlagService;

  /**
   * Messages that arrived before the web view was ready. Those are appended as soon as it is ready.
   */
  private final List<ChatMessage> waitingMessages;
  private final IntegerProperty unreadMessagesCount;
  private final ChangeListener<Boolean> resetUnreadMessagesListener;
  private final ReplayService replayService;
  private final Pattern replayUrlPattern;
  @VisibleForTesting
  Popup clanInfoPopup;
  private int lastEntryId;
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
  private Pattern mentionPattern;
  private Tooltip linkPreviewTooltip;
  private ChangeListener<Boolean> stageFocusedListener;
  private Popup countryInfoPopup;
  private Popup playerInfoPopup;
  private ChatMessage lastMessage;

  @Inject
  // TODO cut dependencies
  public AbstractChatTabController(ClanService clanService, WebViewConfigurer webViewConfigurer,
                                   UserService userService, ChatService chatService,
                                   PlatformService platformService, PreferencesService preferencesService,
                                   PlayerService playerService, AudioService audioService,
                                   TimeService timeService, I18n i18n,
                                   ImageUploadService imageUploadService, UrlPreviewResolver urlPreviewResolver,
                                   NotificationService notificationService, ReportingService reportingService, UiService uiService,
                                   AutoCompletionHelper autoCompletionHelper, EventBus eventBus, CountryFlagService countryFlagService,
                                   ReplayService replayService, ClientProperties clientProperties, ExternalReplayInfoGenerator externalReplayInfoGenerator) {

    this.webViewConfigurer = webViewConfigurer;
    this.clanService = clanService;
    this.uiService = uiService;
    this.chatService = chatService;
    this.userService = userService;
    this.platformService = platformService;
    this.preferencesService = preferencesService;
    this.playerService = playerService;
    this.audioService = audioService;
    this.timeService = timeService;
    this.i18n = i18n;
    this.imageUploadService = imageUploadService;
    this.urlPreviewResolver = urlPreviewResolver;
    this.notificationService = notificationService;
    this.reportingService = reportingService;
    this.autoCompletionHelper = autoCompletionHelper;
    this.eventBus = eventBus;
    this.countryFlagService = countryFlagService;
    this.replayService = replayService;
    this.externalReplayInfoGenerator = externalReplayInfoGenerator;

    String urlFormat = clientProperties.getVault().getReplayDownloadUrlFormat();
    String[] splittedFormat = urlFormat.split("%s");
    replayUrlPattern = Pattern.compile(Pattern.quote(splittedFormat[0]) + "(\\d+)" + Pattern.compile(splittedFormat.length == 2 ? splittedFormat[1] : ""));

    waitingMessages = new ArrayList<>();
    unreadMessagesCount = new SimpleIntegerProperty();
    resetUnreadMessagesListener = (observable, oldValue, newValue) -> {
      if (hasFocus()) {
        setUnread(false);
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

    autoCompletionHelper.bindTo(messageTextField());
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
      stageFocusedListener = (window, windowFocusOld, windowFocusNew) -> {
        if (newTabPane.isVisible()) {
          messageTextField().requestFocus();
        }
      };
      JavaFxUtil.addListener(StageHolder.getStage().focusedProperty(), new WeakChangeListener<>(stageFocusedListener));

      JavaFxUtil.addListener(newTabPane.focusedProperty(), (focusedTabPane, oldTabPaneFocus, newTabPaneFocus) -> {
        if (newTabPaneFocus) {
          messageTextField().requestFocus();
        }
      });
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

    messagesWebView.addEventHandler(MouseEvent.MOUSE_MOVED, moveHandler);
    JavaFxUtil.addListener(messagesWebView.zoomProperty(), (observable, oldValue, newValue) -> {
      preferencesService.getPreferences().getChat().setZoom(newValue.doubleValue());
      preferencesService.storeInBackground();
    });

    Double zoom = preferencesService.getPreferences().getChat().getZoom();
    if (zoom != null) {
      messagesWebView.setZoom(zoom);
    }

    engine = messagesWebView.getEngine();
    getJsObject().setMember(CHAT_TAB_REFERENCE_IN_JAVASCRIPT, this);
    JavaFxUtil.addListener(engine.getLoadWorker().stateProperty(), (observable, oldValue, newValue) -> {
      if (Worker.State.SUCCEEDED == newValue) {
        synchronized (waitingMessages) {
          waitingMessages.forEach(AbstractChatTabController.this::addMessage);
          waitingMessages.clear();
          isChatReady = true;
          onWebViewLoaded();
        }
      }
    });

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

  protected abstract WebView getMessagesWebView();

  protected JSObject getJsObject() {
    return (JSObject) engine.executeScript("window");
  }

  protected void onWebViewLoaded() {
    // Default implementation does nothing, can be overridden by subclass.
  }

  /**
   * Called from JavaScript when user hovers over a clan tag.
   */
  public void clanInfo(String clanTag) {
    String clanTagWithReplacement = removeBrackets(clanTag);
    clanService.getClanByTag(clanTagWithReplacement).thenAccept(clan -> Platform.runLater(() -> {
      if (!clan.isPresent() || clanTagWithReplacement.isEmpty()) {
        return;
      }
      ClanTooltipController clanTooltipController = uiService.loadFxml("theme/chat/clan_tooltip.fxml");
      clanTooltipController.setClan(clan.get());
      clanInfoPopup = new Popup();
      clanTooltipController.getRoot().getStyleClass().add("tooltip");
      clanInfoPopup.getContent().setAll(clanTooltipController.getRoot());
      clanInfoPopup.setAnchorLocation(AnchorLocation.CONTENT_TOP_LEFT);
      clanInfoPopup.show(getRoot().getTabPane().getScene().getWindow(), lastMouseX, lastMouseY + 10);
      clanInfoPopup.setAutoHide(true);

    }));
  }

  /**
   * Called from JavaScript when user no longer hovers over a clan tag.
   */
  public void hideClanInfo() {
    if (clanInfoPopup == null) {
      return;
    }
    clanInfoPopup.hide();
    clanInfoPopup = null;
  }

  /**
   * Called from JavaScript when user clicks on clan tag.
   */
  public void showClanWebsite(String decoratedClanTag) {
    String clanTag = removeBrackets(decoratedClanTag);
    clanService.getClanByTag(clanTag).thenAccept(clan -> {
      if (!clan.isPresent()) {
        return;
      }
      platformService.showDocument(clan.get().getWebsiteUrl());
    });
  }

  private String removeBrackets(String tag) {
    return tag.replaceAll("[\\[\\]]", "");
  }

  /**
   * Called from JavaScript when user hovers over a country flag.
   */
  public void countryInfo(String flagPath) {
    countryInfoPopup = new Popup();
    Label label = new Label();
    label.getStyleClass().add("tooltip");
    countryInfoPopup.getContent().setAll(label);
    
    String flagName = FilenameUtils.getBaseName(flagPath);
    label.setText(i18n.getCountryNameLocalized(flagName));

    countryInfoPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);
    countryInfoPopup.show(getRoot().getTabPane(), lastMouseX, lastMouseY - 10);
  }

  /**
   * Called from JavaScript when user no longer hovers over a country flag.
   */
  public void hideCountryInfo() {
    if (countryInfoPopup == null) {
      return;
    }
    countryInfoPopup.hide();
    countryInfoPopup = null;
  }

  /**
   * Called from JavaScript when user hovers over a user name.
   */
  public void playerInfo(String username) {
    Player player = playerService.getPlayerForUsername(username);
    if (player == null || player.isChatOnly()) {
      return;
    }

    playerInfoPopup = new Popup();
    Label label = new Label();
    label.getStyleClass().add("tooltip");
    playerInfoPopup.getContent().setAll(label);

    label.textProperty().bind(Bindings.createStringBinding(
        () -> i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
        player.leaderboardRatingMeanProperty(), player.leaderboardRatingDeviationProperty(),
        player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()
    ));

    playerInfoPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);
    playerInfoPopup.show(getRoot().getTabPane(), lastMouseX, lastMouseY - 10);
  }

  /**
   * Called from JavaScript when user no longer hovers over a user name.
   */
  public void hidePlayerInfo() {
    if (playerInfoPopup == null) {
      return;
    }
    playerInfoPopup.hide();
    playerInfoPopup = null;
  }

  /**
   * Called from JavaScript when user clicks on user name in chat
   */
  public void openPrivateMessageTab(String username) {
    eventBus.post(new InitiatePrivateChatEvent(username));
  }

  /**
   * Called from JavaScript when user clicked a URL.
   */
  public void openUrl(String url) {
    Matcher replayUrlMatcher = replayUrlPattern.matcher(url);
    if (!replayUrlMatcher.matches()) {
      platformService.showDocument(url);
      return;
    }

    String replayId = replayUrlMatcher.group(1);

    replayService.findById(Integer.parseInt(replayId))
        .thenAccept(replay -> Platform.runLater(() -> externalReplayInfoGenerator.showExternalReplayInfo(replay, replayId)));
  }

  /**
   * Called from JavaScript when user hovers over an URL.
   */
  public void previewUrl(String urlString) {
    urlPreviewResolver.resolvePreview(urlString)
        .thenAccept(optionalPreview -> {
          if (!optionalPreview.isPresent()) {
            return;
          }
          Preview preview = optionalPreview.get();
          linkPreviewTooltip = new Tooltip(preview.getDescription());
          linkPreviewTooltip.setAutoHide(true);
          linkPreviewTooltip.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);
          linkPreviewTooltip.setGraphic(preview.getNode());
          linkPreviewTooltip.setContentDisplay(ContentDisplay.TOP);
          linkPreviewTooltip.show(getRoot().getTabPane(), lastMouseX + 20, lastMouseY);
        });
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
      logger.warn("Message could not be sent: {}", text, throwable);
      notificationService.addNotification(new ImmediateNotification(
          i18n.get("errorTitle"), i18n.get("chat.sendFailed"), Severity.ERROR, throwable, Arrays.asList(
          new ReportAction(i18n, reportingService, throwable),
          new DismissAction(i18n))
      ));

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
        })
        .exceptionally(throwable -> {
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
      if (lastMessage == null || !lastMessage.getUsername().equals(chatMessage.getUsername())
          || lastMessage.getTime().isBefore(chatMessage.getTime().minus(1, MINUTES))) {
        addChatSection(chatMessage);
      }
      appendMessage(chatMessage);
      lastMessage = chatMessage;
    });
  }

  private void appendMessage(ChatMessage chatMessage) throws IOException {
    try (Reader reader = new InputStreamReader(uiService.getThemeFileUrl(CHAT_TEXT).openStream())) {
      String text = htmlEscaper().escape(chatMessage.getMessage()).replace("\\", "\\\\");
      text = convertUrlsToHyperlinks(text);

      Matcher matcher = mentionPattern.matcher(text);
      if (matcher.find()) {
        text = matcher.replaceAll("<span class='self'>" + matcher.group(1) + "</span>");
        onMention(chatMessage);
      }

      String html = CharStreams.toString(reader).replace("{text}", text);

      Collection<String> cssClasses = new ArrayList<>();
      if (chatMessage.isAction()) {
        cssClasses.add(ACTION_CSS_CLASS);
      } else {
        cssClasses.add(MESSAGE_CSS_CLASS);
      }

      html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses));
      addToMessageContainer(html, "chat-section-" + lastEntryId);
    }
  }

  private void addChatSection(ChatMessage chatMessage) throws IOException {
    Player player = playerService.getPlayerForUsername(chatMessage.getUsername());
    try (Reader reader = new InputStreamReader(uiService.getThemeFileUrl(CHAT_ENTRY).openStream())) {
      String login = chatMessage.getUsername();
      String html = CharStreams.toString(reader);

      String avatarUrl = "";
      String clanTag = "";
      String countryFlagUrl = "";
      if (player != null) {
        avatarUrl = player.getAvatarUrl();
        countryFlagUrl = countryFlagService.getCountryFlagUrl(player.getCountry())
            .map(URL::toString)
            .orElse("");

        if (StringUtils.isNotEmpty(player.getClan())) {
          clanTag = i18n.get("chat.clanTagFormat", player.getClan());
        }
      }

      String timeString = timeService.asShortTime(chatMessage.getTime());
      html = html.replace("{time}", timeString)
          .replace("{avatar}", StringUtils.defaultString(avatarUrl))
          .replace("{username}", login)
          .replace("{clan-tag}", clanTag)
          .replace("{country-flag}", StringUtils.defaultString(countryFlagUrl))
          .replace("{section-id}", String.valueOf(++lastEntryId));

      Collection<String> cssClasses = new ArrayList<>();
      cssClasses.add(String.format("user-%s", chatMessage.getUsername()));

      Optional.ofNullable(getMessageCssClass(login)).ifPresent(cssClasses::add);

      html = html.replace("{css-classes}", Joiner.on(' ').join(cssClasses));
      html = html.replace("{inline-style}", getInlineStyle(login));

      addToMessageContainer(html, MESSAGE_CONTAINER_ID);
    }
  }

  protected void onMention(ChatMessage chatMessage) {
    // Default implementation does nothing
  }

  protected void showNotificationIfNecessary(ChatMessage chatMessage) {
    Stage stage = StageHolder.getStage();
    if (stage.isFocused() && stage.isShowing()) {
      return;
    }

    Player player = playerService.getPlayerForUsername(chatMessage.getUsername());
    String identiconSource = player != null ? String.valueOf(player.getId()) : chatMessage.getUsername();

    notificationService.addNotification(new TransientNotification(
        chatMessage.getUsername(),
        chatMessage.getMessage(),
        IdenticonUtil.createIdenticon(identiconSource),
        event -> {
          eventBus.post(new NavigateEvent(NavigationItem.CHAT));
          stage.toFront();
          getRoot().getTabPane().getSelectionModel().select(getRoot());
        })
    );
  }

  protected String getMessageCssClass(String login) {
    String cssClass;
    Player player = playerService.getPlayerForUsername(login);
    if (player == null) {
      return CSS_CLASS_CHAT_ONLY;
    } else {
      cssClass = player.getSocialStatus().getCssClass();
    }

    if (cssClass.equals("") && player.isChatOnly()) {
      cssClass = CSS_CLASS_CHAT_ONLY;
    }
    return cssClass;
  }

  @VisibleForTesting
  String getInlineStyle(String username) {
    ChatUser chatUser = chatService.getOrCreateChatUser(username);
    Player player = playerService.getPlayerForUsername(username);
    ChatPrefs chatPrefs = preferencesService.getPreferences().getChat();
    String color = "";
    String display = "";

    if (chatPrefs.getHideFoeMessages() && player != null && player.getSocialStatus() == FOE) {
      display = "display: none;";
    } else {
      ChatColorMode chatColorMode = chatPrefs.getChatColorMode();
      if ((chatColorMode == ChatColorMode.CUSTOM || chatColorMode == ChatColorMode.RANDOM)
          && chatUser.getColor() != null) {
        color = createInlineStyleFromColor(chatUser.getColor());
      }
    }

    return String.format("%s%s", color, display);
  }

  @VisibleForTesting
  String createInlineStyleFromColor(Color messageColor) {
    return String.format("color: %s;", JavaFxUtil.toRgbCode(messageColor));
  }

  protected String convertUrlsToHyperlinks(String text) {
    return (String) engine.executeScript("link('" + text.replace("'", "\\'") + "')");
  }

  private void addToMessageContainer(String html, String containerId) {
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
