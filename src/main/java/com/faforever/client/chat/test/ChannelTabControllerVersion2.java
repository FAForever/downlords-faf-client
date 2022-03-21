package com.faforever.client.chat.test;

import com.faforever.client.audio.AudioService;
import com.faforever.client.chat.AbstractChatTabController;
import com.faforever.client.chat.ChatChannel;
import com.faforever.client.chat.ChatChannelUser;
import com.faforever.client.chat.ChatService;
import com.faforever.client.chat.ChatUserService;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static com.faforever.client.fx.PlatformService.URL_REGEX_PATTERN;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ChannelTabControllerVersion2 extends AbstractChatTabController {

  public static final String MODERATOR_STYLE_CLASS = "moderator";
  public static final String USER_STYLE_CLASS = "user-%s";

  private final PlatformService platformService;

  public Tab root;
  public SplitPane splitPane;
  public HBox chatMessageSearchContainer;
  public Button closeChatMessageSearchButton;
  public TextField chatMessageSearchTextField;
  public WebView messagesWebView;
  public TextField messageTextField;
  public VBox topicPane;
  public TextFlow topicText;
  public ToggleButton toggleSidePaneButton;
  public Node chatUserList;
  public ChatUserListController chatUserListController;

  private String channelName;
  private ChatChannel chatChannel;

  private final InvalidationListener topicListener = observable -> JavaFxUtil.runLater(this::updateChannelTopic);
  private final ChangeListener<String> searchChatMessageListener = (observable, oldValue, newValue) -> {
    if (StringUtils.isBlank(newValue)) {
      callJsMethod("removeHighlight");
    } else {
      callJsMethod("highlightText", newValue);
    }
  };

  public ChannelTabControllerVersion2(WebViewConfigurer webViewConfigurer, UserService userService, ChatService chatService, PreferencesService preferencesService, PlayerService playerService, AudioService audioService, TimeService timeService, I18n i18n, ImageUploadService imageUploadService, NotificationService notificationService, ReportingService reportingService, UiService uiService, EventBus eventBus, CountryFlagService countryFlagService, ChatUserService chatUserService, EmoticonService emoticonService, PlatformService platformService) {
    super(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService, timeService, i18n, imageUploadService, notificationService, reportingService, uiService, eventBus, countryFlagService, chatUserService, emoticonService);
    this.platformService = platformService;
  }

  @Override
  public void initialize() {
    super.initialize();

    JavaFxUtil.bindManagedToVisible(topicPane);
    JavaFxUtil.bind(chatMessageSearchTextField.visibleProperty(), chatMessageSearchContainer.visibleProperty());
    JavaFxUtil.bind(closeChatMessageSearchButton.visibleProperty(), chatMessageSearchContainer.visibleProperty());
  }

  public void setChatChannel(ChatChannel chatChannel, BooleanBinding chatTabSelectedProperty) {
    this.chatChannel = chatChannel;
    this.channelName = chatChannel.getName();
    chatUserListController.setChatChannel(chatChannel, getRoot(), chatTabSelectedProperty);
    chatUserListController.getAutoCompletionHelper().bindTo(messageTextField());

    updateTabProperties();
    updateChannelTopic();
    setReceiver(chatChannel.getName());

    initializeListeners();
  }

  private void initializeListeners() {
    JavaFxUtil.addListener(chatMessageSearchTextField.textProperty(), new WeakChangeListener<>(searchChatMessageListener));
    JavaFxUtil.addListener(chatChannel.topicProperty(), new WeakInvalidationListener(topicListener));
  }

  private void updateTabProperties() {
    root.setId(channelName);
    root.setText(channelName);
    getRoot().setOnCloseRequest(event -> {
      chatService.leaveChannel(channelName);
      chatUserListController.onTabClosed();
    });
  }

  private void updateChannelTopic() {
    List<Node> children = topicText.getChildren();
    boolean empty = StringUtils.isBlank(chatChannel.getTopic());
    topicPane.setVisible(!empty);
    children.clear();
    if (!empty) {
      Arrays.stream(chatChannel.getTopic().split("\\s"))
          .forEach(word -> {
            if (URL_REGEX_PATTERN.matcher(word).matches()) {
              Hyperlink link = new Hyperlink(word);
              link.setOnAction(event -> platformService.showDocument(word));
              children.add(link);
            } else {
              children.add(new Label(word + " "));
            }
          });
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

  @Subscribe
  public void onChatUserCategoryChange(ChatUserCategoryChangeEvent event) {
    ChatChannelUser user = event.getChatUser();
    if (chatChannel.containsUser(user)) {
      updateUserMessageVisibility(user, user.getSocialStatus().filter(status -> status == SocialStatus.FOE).isPresent());
      updateStyleClass(user);
      updateUserMessageColor(user);
    }
  }

  @Subscribe
  public void onChatUserColorChange(ChatUserColorChangeEvent event) {
    ChatChannelUser user = event.getChatUser();
    if (chatChannel.containsUser(user)) {
      updateUserMessageColor(user);
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
    user.getPlayer().ifPresentOrElse(player -> removeUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY),
        () -> addUserMessageStyleClass(user, CSS_CLASS_CHAT_ONLY));
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
