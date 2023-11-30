package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
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
import com.faforever.client.user.LoginService;
import com.faforever.client.util.TimeService;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
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
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.faforever.client.fx.PlatformService.URL_REGEX_PATTERN;
import static com.faforever.client.player.SocialStatus.FOE;
import static java.util.Locale.US;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabController extends AbstractChatTabController {

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

  private final ObservableValue<ChannelTopic> channelTopic = chatChannel.flatMap(ChatChannel::topicProperty);
  private final ObservableValue<ObservableList<ChatChannelUser>> users = chatChannel.map(ChatChannel::getUsers)
                                                                                    .orElse(
                                                                                        FXCollections.emptyObservableList());
  private final ListChangeListener<ChatChannelUser> channelUserListChangeListener = this::updateChangedUsersStyles;


  public ChannelTabController(WebViewConfigurer webViewConfigurer, LoginService loginService, ChatService chatService,
                              PlayerService playerService,
                              TimeService timeService, I18n i18n,
                              NotificationService notificationService, UiService uiService, ThemeService themeService,
                              NavigationHandler navigationHandler,
                              CountryFlagService countryFlagService, EmoticonService emoticonService,
                              PlatformService platformService, ChatPrefs chatPrefs,
                              NotificationPrefs notificationPrefs,
                              FxApplicationThreadExecutor fxApplicationThreadExecutor) {
    super(loginService, chatService, playerService, timeService, i18n, notificationService, uiService, themeService,
          webViewConfigurer, emoticonService, countryFlagService, chatPrefs, notificationPrefs,
          fxApplicationThreadExecutor, navigationHandler);
    this.platformService = platformService;
  }

  @Override
  protected void onInitialize() {
    super.onInitialize();
    JavaFxUtil.bindManagedToVisible(topicPane, chatUserList, changeTopicTextButton, topicTextField,
                                    cancelChangesTopicTextButton, topicText, topicCharactersLimitLabel,
                                    chatMessageSearchContainer);

    topicCharactersLimitLabel.visibleProperty().bind(topicTextField.visibleProperty());
    cancelChangesTopicTextButton.visibleProperty().bind(topicTextField.visibleProperty());
    chatUserList.visibleProperty().bind(userListVisibilityToggleButton.selectedProperty());
    userListVisibilityToggleButton.selectedProperty().bindBidirectional(chatPrefs.playerListShownProperty());

    topicTextField.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText()
                                                                        .length() <= TOPIC_CHARACTERS_LIMIT ? change : null));

    topicCharactersLimitLabel.textProperty()
                             .bind(topicTextField.textProperty()
                                                 .length()
                                                 .map(length -> String.format("%d / %d", length.intValue(),
                                                                              TOPIC_CHARACTERS_LIMIT))
                                                 .when(showing));

    topicPane.visibleProperty()
             .bind(topicText.visibleProperty()
                            .or(changeTopicTextButton.visibleProperty())
                            .or(topicTextField.visibleProperty())
                            .when(showing));

    root.textProperty().bind(channelName.map(name -> name.replaceFirst("^#", "")).when(attached));

    chatUserListController.chatChannelProperty().bind(chatChannel.when(showing));

    ObservableValue<Boolean> isModerator = chatChannel.map(channel -> channel.getUser(loginService.getUsername())
                                                                             .orElse(null))
                                                      .flatMap(ChatChannelUser::moderatorProperty)
                                                      .orElse(false)
                                                      .when(showing);
    changeTopicTextButton.visibleProperty()
                         .bind(BooleanExpression.booleanExpression(isModerator)
                                                .and(topicTextField.visibleProperty().not())
                                                .when(showing));

    chatMessageSearchTextField.textProperty().when(showing).subscribe(this::highlightText);
    chatPrefs.hideFoeMessagesProperty()
             .when(showing)
             .subscribe(this::hideFoeMessages);
    chatPrefs.chatColorModeProperty()
             .when(showing)
             .subscribe(() -> users.getValue().forEach(this::updateUserMessageColor));
    channelTopic.when(showing).subscribe(this::updateChannelTopic);
    userListVisibilityToggleButton.selectedProperty().when(showing).subscribe(this::updateDividerPosition);

    users.when(attached).subscribe((oldValue, newValue) -> {
      if (oldValue != null) {
        oldValue.removeListener(channelUserListChangeListener);
      }
      if (newValue != null) {
        newValue.addListener(channelUserListChangeListener);
      }
    });

    AutoCompletionHelper autoCompletionHelper = getAutoCompletionHelper();
    autoCompletionHelper.bindTo(messageTextField());
  }

  @Override
  public void onDetached() {
    super.onDetached();
    ObservableList<ChatChannelUser> users = this.users.getValue();
    if (users != null) {
      users.removeListener(channelUserListChangeListener);
    }
  }

  public AutoCompletionHelper getAutoCompletionHelper() {
    return new AutoCompletionHelper(currentWord -> users.getValue()
                                                        .stream()
                                                        .map(ChatChannelUser::getUsername)
                                                        .filter(username -> username.toLowerCase(US)
                                                                                    .startsWith(
                                                                                        currentWord.toLowerCase()))
                                                        .sorted()
                                                        .collect(Collectors.toList()));
  }

  private void highlightText(String newValue) {
    if (StringUtils.isBlank(newValue)) {
      callJsMethod("removeHighlight");
    } else {
      callJsMethod("highlightText", newValue);
    }
  }

  private void hideFoeMessages(boolean shouldHide) {
    users.getValue()
         .stream()
         .filter(user -> user.getCategories().stream().anyMatch(status -> status == ChatUserCategory.FOE))
         .forEach(user -> updateUserMessageVisibility(user, shouldHide));
  }

  private void updateChangedUsersStyles(Change<? extends ChatChannelUser> change) {
    while (change.next()) {
      if (change.wasUpdated()) {
        List<ChatChannelUser> changedUsers = List.copyOf(change.getList().subList(change.getFrom(), change.getTo()));
        for (ChatChannelUser user : changedUsers) {
          boolean shouldHide = user.getCategories().stream().anyMatch(status -> status == ChatUserCategory.FOE);
          updateUserMessageVisibility(user, shouldHide);
          updateStyleClass(user);
          updateUserMessageColor(user);
        }
      }
    }
  }

  private void updateDividerPosition() {
    boolean selected = userListVisibilityToggleButton.isSelected();
    splitPane.setDividerPositions(selected ? 0.8 : 1);
  }

  private void setChannelTopic(String content) {
    List<Node> children = topicText.getChildren();
    children.clear();
    boolean notBlank = StringUtils.isNotBlank(content);
    if (notBlank) {
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

  private void updateChannelTopic(ChannelTopic oldTopic, ChannelTopic newTopic) {
    String newTopicContent = newTopic.content();

    fxApplicationThreadExecutor.execute(() -> {
      setChannelTopic(newTopicContent);

      if (topicPane.isDisable()) {
        topicTextField.setVisible(false);
        topicPane.setDisable(false);
      }
    });


    if (oldTopic != null) {
      String oldTopicContent = oldTopic.content();
      onChatMessage(new ChatMessage(Instant.now(), newTopic.author(),
                                    i18n.get("chat.topicUpdated", oldTopicContent, newTopicContent)));
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
    fxApplicationThreadExecutor.execute(() -> callJsMethod("updateUserMessageColor", user.getUsername(), color));
  }

  private void updateUserMessageVisibility(ChatChannelUser user, boolean shouldHide) {
    String displayPropertyValue = shouldHide ? "none" : "";
    fxApplicationThreadExecutor.execute(
        () -> callJsMethod("updateUserMessageDisplay", user.getUsername(), displayPropertyValue));
  }

  private void updateStyleClass(ChatChannelUser user) {
    user.getPlayer()
        .ifPresentOrElse(player -> removeUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY),
                         () -> addUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY));
    if (user.isModerator()) {
      addUserMessageStyleClass(user, MODERATOR_STYLE_CLASS);
    } else {
      removeUserMessageStyleClass(user, MODERATOR_STYLE_CLASS);
    }
  }

  private void addUserMessageStyleClass(ChatChannelUser user, String styleClass) {
    fxApplicationThreadExecutor.execute(
        () -> callJsMethod("addUserMessageClass", String.format(USER_STYLE_CLASS, user.getUsername()), styleClass));
  }

  private void removeUserMessageStyleClass(ChatChannelUser user, String styleClass) {
    if (StringUtils.isNotBlank(styleClass)) {
      fxApplicationThreadExecutor.execute(
          () -> callJsMethod("removeUserMessageClass", String.format(USER_STYLE_CLASS, user.getUsername()),
                             styleClass));
    }
  }

  @Override
  protected String getMessageCssClass(String login) {
    return chatService.getOrCreateChatUser(login, channelName.getValue())
                      .isModerator() ? MODERATOR_STYLE_CLASS : super.getMessageCssClass(login);
  }

  @Override
  protected void onMention(ChatMessage chatMessage) {
    if (notificationPrefs.getNotifyOnAtMentionOnlyEnabled() && !chatMessage.message()
                                                                           .contains(
                                                                               "@" + loginService.getUsername())) {
      return;
    }

    if (playerService.getPlayerByNameIfOnline(chatMessage.username())
                     .map(PlayerBean::getSocialStatus)
                     .map(FOE::equals)
                     .orElse(false)) {
      log.debug("Ignored ping from {}", chatMessage.username());
    } else if (!hasFocus()) {
      incrementUnreadMessagesCount();
      setUnread(true);
    }
  }

  @Override
  protected String getInlineStyle(String username) {
    ChatChannelUser user = chatChannel.map(channel -> channel.getUser(username).orElse(null)).getValue();
    if (user == null) {
      return "";
    }

    if (chatPrefs.isHideFoeMessages() && user.getCategories()
                                             .stream()
                                             .anyMatch(category -> category == ChatUserCategory.FOE)) {
      return "display: none;";
    } else {
      return user.getColor().map(color -> String.format("color: %s;", JavaFxUtil.toRgbCode(color))).orElse("");
    }
  }

  public void onChangeTopicTextButtonClicked() {
    topicText.setVisible(false);
    topicTextField.setText(channelTopic.getValue().content());
    topicTextField.setVisible(true);
    topicTextField.requestFocus();
    topicTextField.selectEnd();
  }

  public void onTopicTextFieldEntered() {
    String normalizedText = StringUtils.normalizeSpace(topicTextField.getText());
    if (!normalizedText.equals(channelTopic.getValue().content())) {
      topicPane.setDisable(true);
      chatService.setChannelTopic(chatChannel.getValue(), normalizedText);
    } else {
      onCancelChangesTopicTextButtonClicked();
    }
  }

  public void onCancelChangesTopicTextButtonClicked() {
    topicTextField.setText(channelTopic.getValue().content());
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
