package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.theme.UiService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.WeakListChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.faforever.client.fx.PlatformService.URL_REGEX_PATTERN;
import static com.faforever.client.player.SocialStatus.FOE;
import static java.util.Locale.US;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabController extends AbstractChatTabController implements InitializingBean {

  public static final String MODERATOR_STYLE_CLASS = "moderator";
  public static final String USER_STYLE_CLASS = "user-%s";

  private static final int TOPIC_CHARACTERS_LIMIT = 350;

  private final PlatformService platformService;

  public Tab root;
  public SplitPane splitPane;
  public HBox chatMessageSearchContainer;
  public Button closeChatMessageSearchButton;
  public TextField chatMessageSearchTextField;
  public WebView messagesWebView;
  public TextField messageTextField;
  public HBox topicPane;
  public Label topicCharactersLimitLabel;
  public TextField topicTextField;
  public TextFlow topicText;
  public Button changeTopicTextButton;
  public Button cancelChangesTopicTextButton;
  public ToggleButton userListVisibilityToggleButton;
  public Node chatUserList;
  public ChatUserListController chatUserListController;

  private ChatPrefs chatPrefs;
  private ChatChannel chatChannel;
  private String topicContent = "";

  /* Listeners */
  private final InvalidationListener topicListener = observable -> JavaFxUtil.runLater(this::updateChannelTopic);
  @SuppressWarnings("FieldCanBeLocal")
  private final InvalidationListener hideFoeMessagesListener = observable -> hideFoeMessages();
  private final InvalidationListener chatColorModeListener = observable -> chatChannel.getUsers()
      .forEach(this::updateUserMessageColor);
  private final ListChangeListener<ChatChannelUser> channelUserListChangeListener = this::updateChangedUsersStyles;

  private AutoCompletionHelper autoCompletionHelper;

  public ChannelTabController(WebViewConfigurer webViewConfigurer, UserService userService, ChatService chatService, PreferencesService preferencesService, PlayerService playerService, AudioService audioService, TimeService timeService, I18n i18n, NotificationService notificationService, UiService uiService, EventBus eventBus, CountryFlagService countryFlagService, EmoticonService emoticonService, PlatformService platformService) {
    super(userService, chatService, preferencesService, playerService, audioService, timeService, i18n, notificationService, uiService, eventBus, webViewConfigurer, emoticonService, countryFlagService);
    this.platformService = platformService;
  }

  private void highlightText(String newValue) {
    if (StringUtils.isBlank(newValue)) {
      callJsMethod("removeHighlight");
    } else {
      callJsMethod("highlightText", newValue);
    }
  }

  private void hideFoeMessages() {
    boolean visible = chatPrefs.getHideFoeMessages();
    chatChannel.getUsers()
        .stream()
        .filter(user -> user.getCategories().stream().anyMatch(status -> status == ChatUserCategory.FOE))
        .forEach(user -> updateUserMessageVisibility(user, visible));
  }

  private void updateChangedUsersStyles(Change<? extends ChatChannelUser> change) {
    while (change.next()) {
      if (change.wasUpdated()) {
        List<ChatChannelUser> changedUsers = List.copyOf(change.getList().subList(change.getFrom(), change.getTo()));
        changedUsers.forEach(user -> {
          updateUserMessageVisibility(user, user.getCategories()
              .stream()
              .anyMatch(status -> status == ChatUserCategory.FOE));
          updateStyleClass(user);
          updateUserMessageColor(user);
        });
      }
    }
  }

  private void updateDividerPosition() {
    boolean selected = userListVisibilityToggleButton.isSelected();
    splitPane.setDividerPositions(selected ? 0.8 : 1);
    preferencesService.storeInBackground();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    this.chatPrefs = preferencesService.getPreferences().getChat();
  }

  @Override
  public void initialize() {
    super.initialize();
    JavaFxUtil.bindManagedToVisible(topicPane, chatUserList, changeTopicTextButton, topicTextField, cancelChangesTopicTextButton, topicText, topicCharactersLimitLabel, chatMessageSearchContainer);
    JavaFxUtil.bind(topicCharactersLimitLabel.visibleProperty(), topicTextField.visibleProperty());
    JavaFxUtil.bind(cancelChangesTopicTextButton.visibleProperty(), topicTextField.visibleProperty());
    JavaFxUtil.bind(chatUserList.visibleProperty(), userListVisibilityToggleButton.selectedProperty());

    userListVisibilityToggleButton.selectedProperty()
        .bindBidirectional(preferencesService.getPreferences().getChat().playerListShownProperty());
    topicTextField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText()
        .length() <= TOPIC_CHARACTERS_LIMIT ? change : null));

    topicCharactersLimitLabel.textProperty()
        .bind(topicTextField.textProperty()
            .length()
            .map(length -> String.format("%d / %d", length.intValue(), TOPIC_CHARACTERS_LIMIT)));

    JavaFxUtil.addListener(chatPrefs.hideFoeMessagesProperty(), new WeakInvalidationListener(hideFoeMessagesListener));
    JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), new WeakInvalidationListener(chatColorModeListener));

    userListVisibilityToggleButton.selectedProperty().addListener(new WeakInvalidationListener(observable -> updateDividerPosition()));

    autoCompletionHelper = getAutoCompletionHelper();
    autoCompletionHelper.bindTo(messageTextField());
  }

  @Override
  protected void onClosed(Event event) {
    super.onClosed(event);
    chatUserListController.dispose();
    autoCompletionHelper.unbind();
  }

  public void setChatChannel(ChatChannel chatChannel) {
    Assert.checkNotNullIllegalState(this.chatChannel, "Chat channel already set");

    this.chatChannel = chatChannel;
    JavaFxUtil.bind(topicPane.visibleProperty(), topicText.visibleProperty()
        .or(changeTopicTextButton.visibleProperty())
        .or(topicTextField.visibleProperty()));

    chatChannel.getUser(userService.getUsername())
        .ifPresentOrElse(ownUser -> changeTopicTextButton.visibleProperty().bind(ownUser.moderatorProperty()),
            () -> {
              log.warn("Cannot get own chat user of `{}` channel", chatChannel.getName());
              changeTopicTextButton.visibleProperty().unbind();
              changeTopicTextButton.setVisible(false);
            });
    chatUserListController.setChatChannel(chatChannel);
    chatChannel.getUsers().addListener(new WeakListChangeListener<>(channelUserListChangeListener));

    updateTabProperties();
    setChannelTopic(chatChannel.getTopic().getContent());
    setReceiver(chatChannel.getName());

    initializeListeners();
  }

  public AutoCompletionHelper getAutoCompletionHelper() {
    return new AutoCompletionHelper(currentWord -> chatChannel.getUsers()
        .stream()
        .map(ChatChannelUser::getUsername)
        .filter(username -> username.toLowerCase(US).startsWith(currentWord.toLowerCase()))
        .sorted()
        .collect(Collectors.toList()));
  }

  private void initializeListeners() {
    JavaFxUtil.addListener(chatMessageSearchTextField.textProperty(), (observable, oldValue, newValue) -> highlightText(newValue));
    JavaFxUtil.addListener(chatChannel.topicProperty(), new WeakInvalidationListener(topicListener));
  }

  private void updateTabProperties() {
    String channelName = chatChannel.getName();
    root.setId(channelName);
    root.setText(channelName.replaceFirst("^#", ""));
    getRoot().setOnCloseRequest(event -> chatService.leaveChannel(channelName));
  }

  private void setChannelTopic(String content) {
    List<Node> children = topicText.getChildren();
    children.clear();
    boolean notBlank = StringUtils.isNotBlank(content);
    if (notBlank) {
      topicContent = content;
      Arrays.stream(content.split("\\s")).forEach(word -> {
        if (URL_REGEX_PATTERN.matcher(word).matches()) {
          Hyperlink link = new Hyperlink(word);
          link.setOnAction(event -> platformService.showDocument(word));
          children.add(link);
        } else {
          children.add(new Label(word + " "));
        }
      });
    }
    topicText.setVisible(notBlank);
  }

  private void updateChannelTopic() {
    String oldTopicContent = topicContent;
    String newTopicContent = chatChannel.getTopic().getContent();
    if (StringUtils.equals(oldTopicContent, newTopicContent)) {
      return;
    }

    setChannelTopic(newTopicContent);
    onChatMessage(new ChatMessage(chatChannel.getName(), Instant.now(), chatChannel.getTopic()
        .getAuthor(), i18n.get("chat.topicUpdated", oldTopicContent, newTopicContent)));

    if (topicPane.isDisable()) {
      topicTextField.setVisible(false);
      changeTopicTextButton.setVisible(true);
      topicPane.setDisable(false);
    }
  }

  public void onChatChannelKeyReleased(KeyEvent keyEvent) {
    KeyCode code = keyEvent.getCode();
    if (code == KeyCode.ESCAPE) {
      onChatMessageSearchButtonClicked();
    } else if (keyEvent.isControlDown() && code == KeyCode.F) {
      showOrHideChatMessageSearchContainer();
    }
  }

  public void onChatMessageSearchButtonClicked() {
    chatMessageSearchContainer.setVisible(false);
    chatMessageSearchTextField.clear();
  }

  public void showOrHideChatMessageSearchContainer() {
    chatMessageSearchContainer.setVisible(!chatMessageSearchContainer.isVisible());
    chatMessageSearchTextField.clear();
    if (chatMessageSearchContainer.isVisible()) {
      chatMessageSearchTextField.requestFocus();
    } else {
      messageTextField().requestFocus();
    }
  }

  private void updateUserMessageColor(ChatChannelUser user) {
    String color = user.getColor().map(JavaFxUtil::toRgbCode).orElse("");
    JavaFxUtil.runLater(() -> callJsMethod("updateUserMessageColor", user.getUsername(), color));
  }

  private void updateUserMessageVisibility(ChatChannelUser user, boolean visible) {
    String displayPropertyValue = visible ? "none" : "";
    JavaFxUtil.runLater(() -> callJsMethod("updateUserMessageDisplay", user.getUsername(), displayPropertyValue));
  }

  private void updateStyleClass(ChatChannelUser user) {
    user.getPlayer()
        .ifPresentOrElse(player -> removeUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY), () -> addUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY));
    if (user.isModerator()) {
      addUserMessageStyleClass(user, MODERATOR_STYLE_CLASS);
    } else {
      removeUserMessageStyleClass(user, MODERATOR_STYLE_CLASS);
    }
  }

  private void addUserMessageStyleClass(ChatChannelUser user, String styleClass) {
    JavaFxUtil.runLater(() -> callJsMethod("addUserMessageClass", String.format(USER_STYLE_CLASS, user.getUsername()), styleClass));
  }

  private void removeUserMessageStyleClass(ChatChannelUser user, String styleClass) {
    if (StringUtils.isNotBlank(styleClass)) {
      JavaFxUtil.runLater(() -> callJsMethod("removeUserMessageClass", String.format(USER_STYLE_CLASS, user.getUsername()), styleClass));
    }
  }

  @Override
  protected String getMessageCssClass(String login) {
    return chatService.getOrCreateChatUser(login, chatChannel.getName())
        .isModerator() ? MODERATOR_STYLE_CLASS : super.getMessageCssClass(login);
  }

  @Override
  protected void onMention(ChatMessage chatMessage) {
    if (preferencesService.getPreferences()
        .getNotification()
        .getNotifyOnAtMentionOnlyEnabled() && !chatMessage.getMessage().contains("@" + userService.getUsername())) {
      return;
    }

    if (playerService.getPlayerByNameIfOnline(chatMessage.getUsername())
        .filter(player -> player.getSocialStatus() == FOE)
        .isPresent()) {
      log.debug("Ignored ping from {}", chatMessage.getUsername());
    } else if (!hasFocus()) {
      audioService.playChatMentionSound();
      showNotificationIfNecessary(chatMessage);
      incrementUnreadMessagesCount();
      setUnread(true);
    }
  }

  @Override
  protected String getInlineStyle(String username) {
    ChatChannelUser user = chatService.getOrCreateChatUser(username, chatChannel.getName());
    Optional<PlayerBean> playerOptional = playerService.getPlayerByNameIfOnline(username);

    if (chatPrefs.getHideFoeMessages() && playerOptional.map(PlayerBean::getSocialStatus)
        .stream()
        .anyMatch(status -> status == FOE)) {
      return "display: none;";
    } else {
      return user.getColor().map(color -> String.format("color: %s;", JavaFxUtil.toRgbCode(color))).orElse("");
    }
  }

  public void onChangeTopicTextButtonClicked() {
    changeTopicTextButton.setVisible(false);
    topicText.setVisible(false);
    topicTextField.setText(topicContent);
    topicTextField.setVisible(true);
    topicTextField.requestFocus();
    topicTextField.selectEnd();
  }

  public void onTopicTextFieldEntered() {
    String normalizedText = StringUtils.normalizeSpace(topicTextField.getText());
    if (!normalizedText.equals(topicContent)) {
      topicPane.setDisable(true);
      chatService.setChannelTopic(chatChannel.getName(), normalizedText);
    } else {
      onCancelChangesTopicTextButtonClicked();
    }
  }

  public void onCancelChangesTopicTextButtonClicked() {
    topicTextField.setText(topicContent);
    topicTextField.setVisible(false);
    topicText.setVisible(true);
    changeTopicTextButton.setVisible(true);
  }

  @Override
  public Tab getRoot() {
    return root;
  }

  @Override
  protected TextInputControl messageTextField() {
    return messageTextField;
  }

  @Override
  protected WebView getMessagesWebView() {
    return messagesWebView;
  }
}
