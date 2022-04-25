package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.event.ChatUserCategoryChangeEvent;
import com.faforever.client.chat.event.ChatUserColorChangeEvent;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.beans.binding.BooleanBinding;
import javafx.scene.control.Labeled;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.scheduling.TaskScheduler;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static com.faforever.client.theme.UiService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.UiService.CHAT_TEXT_COMPACT;
import static com.faforever.client.theme.UiService.CHAT_TEXT_EXTENDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatChannelTabControllerTest extends UITest {

  private static final String USER_NAME = "junit";
  private static final String CHANNEL_NAME = "#testChannel";

  @InjectMocks
  private ChannelTabController instance;

  @Mock
  private ChatService chatService;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TimeService timeService;
  @Mock
  private ImageUploadService imageUploadService;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private AudioService audioService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private EventBus eventBus;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ChatUserService chatUserService;
  @Mock
  private EmoticonService emoticonService;
  @Mock
  private TaskScheduler taskScheduler;
  @Mock
  private ChatUserListController chatUserListController;

  private Preferences preferences;
  private ChatChannel defaultChatChannel;

  @BeforeEach
  public void setUp() throws Exception {
    defaultChatChannel = new ChatChannel(CHANNEL_NAME);
    preferences = PreferencesBuilder.create().defaultValues().chatPrefs().then().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(userService.getUsername()).thenReturn(USER_NAME);
    when(uiService.getThemeFileUrl(CHAT_CONTAINER)).thenReturn(getClass().getResource("/theme/chat/chat_container.html"));
    when(uiService.getThemeFileUrl(CHAT_SECTION_COMPACT)).thenReturn(getClass().getResource("/theme/chat/compact/chat_section.html"));
    when(uiService.getThemeFileUrl(CHAT_TEXT_EXTENDED)).thenReturn(getClass().getResource("/theme/chat/extended/chat_text.html"));
    when(uiService.getThemeFileUrl(CHAT_TEXT_COMPACT)).thenReturn(getClass().getResource("/theme/chat/compact/chat_text.html"));
    when(timeService.asShortTime(any())).thenReturn("now");
    when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile("-----"));
    when(chatService.getOrCreateChatUser(any(String.class), eq(CHANNEL_NAME))).thenReturn(mock(ChatChannelUser.class));
    when(chatUserListController.getAutoCompletionHelper()).thenReturn(mock(AutoCompletionHelper.class));

    loadFxml("theme/chat/channel_tab.fxml", clazz -> {
      if (clazz == ChatUserListController.class) {
        return chatUserListController;
      }
      return instance;
    });
    instance.afterPropertiesSet();
  }

  @Test
  public void testGetRoot() {
    assertNotNull(instance.getRoot());
  }

  @Test
  public void testGetMessagesWebView() {
    assertNotNull(instance.getMessagesWebView());
  }

  @Test
  public void testGetMessageTextField() {
    assertNotNull(instance.messageTextField());
  }

  @Test
  public void testSetChannelTopic() {
    defaultChatChannel.setTopic(new ChannelTopic(USER_NAME, "topic https://example.com/1"));
    initializeDefaultChatChannel();

    assertEquals(2, instance.topicText.getChildren().size());
    assertEquals("topic ", ((Labeled) instance.topicText.getChildren().get(0)).getText());
    assertEquals("https://example.com/1", ((Labeled) instance.topicText.getChildren().get(1)).getText());
    assertTrue(instance.topicPane.isVisible());
  }

  @Test
  public void testNoChannelTopic() {
    ChatChannelUser user = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();
    defaultChatChannel.addUser(user);
    when(userService.getUsername()).thenReturn(USER_NAME);

    defaultChatChannel.setTopic(new ChannelTopic("",""));
    initializeDefaultChatChannel();
    assertFalse(instance.topicPane.isVisible());
  }

  @Test
  public void testSetTabName() {
    initializeDefaultChatChannel();
    assertEquals("testChannel", instance.root.getText());
  }

  @Test
  public void testChannelTopicUpdate() {
    defaultChatChannel.setTopic(new ChannelTopic(USER_NAME, "topic1: https://faforever.com"));
    initializeDefaultChatChannel();

    assertEquals(2, instance.topicText.getChildren().size());

    runOnFxThreadAndWait(() -> defaultChatChannel.setTopic(new ChannelTopic("junit2", "topic2: https://faforever.com topic3: https://faforever.com/example")));
    assertEquals(4, instance.topicText.getChildren().size());
    assertEquals("topic2: ", ((Labeled) instance.topicText.getChildren().get(0)).getText());
    assertEquals("https://faforever.com", ((Labeled) instance.topicText.getChildren().get(1)).getText());
    assertEquals("topic3: ", ((Labeled) instance.topicText.getChildren().get(2)).getText());
    assertEquals("https://faforever.com/example", ((Labeled) instance.topicText.getChildren().get(3)).getText());
  }

  @Test
  public void testChangeTopicButtonForModerators() {
    doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return invocation;
    }).when(instance.chatUserListController).setOnListInitialized(any(Runnable.class));

    ChatChannelUser user = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().moderator(true).get();
    defaultChatChannel.addUser(user);
    when(userService.getUsername()).thenReturn(USER_NAME);
    initializeDefaultChatChannel();
    assertTrue(instance.changeTopicTextButton.isVisible());
  }

  @Test
  public void testNoChangeTopicButtonForNonModerators() {
    doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return invocation;
    }).when(instance.chatUserListController).setOnListInitialized(any(Runnable.class));

    ChatChannelUser user = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().moderator(false).get();
    defaultChatChannel.addUser(user);
    when(userService.getUsername()).thenReturn(USER_NAME);
    initializeDefaultChatChannel();
    assertFalse(instance.changeTopicTextButton.isVisible());
  }

  @Test
  public void testCheckModeratorListener() {
    doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return invocation;
    }).when(instance.chatUserListController).setOnListInitialized(any(Runnable.class));

    ChatChannelUser user = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().moderator(true).get();
    defaultChatChannel.addUser(user);
    when(userService.getUsername()).thenReturn(USER_NAME);
    initializeDefaultChatChannel();
    assertTrue(instance.changeTopicTextButton.isVisible());
    runOnFxThreadAndWait(() -> user.setModerator(false));
    assertFalse(instance.changeTopicTextButton.isVisible());
  }

  @Test
  public void testOnTopicTextFieldEntered() {
    defaultChatChannel.setTopic(new ChannelTopic(USER_NAME,"topic1: https://faforever.com"));
    initializeDefaultChatChannel();

    runOnFxThreadAndWait(() -> {
      instance.topicTextField.setText("New Topic");
      instance.onTopicTextFieldEntered();
    });

    verify(chatService).setChannelTopic(CHANNEL_NAME, "New Topic");
  }

  @Test
  public void testOnChangeTopicTextButtonClicked() {
    defaultChatChannel.setTopic(new ChannelTopic(USER_NAME,"topic1: https://faforever.com"));
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> instance.onChangeTopicTextButtonClicked());
    assertEquals("topic1: https://faforever.com", instance.topicTextField.getText());
    assertTrue(instance.topicTextField.isVisible());
    assertFalse(instance.topicText.isVisible());
  }

  @Test
  public void testOnCancelChangesTopicTextButtonClicked() {
    defaultChatChannel.setTopic(new ChannelTopic(USER_NAME, "topic: https://faforever.com"));
    initializeDefaultChatChannel();

    runOnFxThreadAndWait(() -> {
      instance.topicTextField.setText("New Topic");
      instance.onCancelChangesTopicTextButtonClicked();
    });

    assertEquals(2, instance.topicText.getChildren().size());
    assertEquals("topic: ", ((Labeled) instance.topicText.getChildren().get(0)).getText());
    assertEquals("https://faforever.com", ((Labeled) instance.topicText.getChildren().get(1)).getText());
    assertFalse(instance.topicTextField.isVisible());
    assertTrue(instance.topicText.isVisible());
  }

  @Test
  public void textCheckTextTopicLimitListener() {
    defaultChatChannel.setTopic(new ChannelTopic(USER_NAME, "topic: https://faforever.com"));
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> instance.onChangeTopicTextButtonClicked());
    int length = "topic: https://faforever.com".length();
    assertTrue(instance.topicCharactersLimitLabel.getText().contains(Integer.toString(length)));

    runOnFxThreadAndWait(() -> instance.topicTextField.appendText("123"));
    assertTrue(instance.topicCharactersLimitLabel.getText().contains(Integer.toString(length + + 3)));
  }

  @Test
  public void testSearchChatMessage() throws Exception {
    String highlighted = "<span class=\"highlight\">world</span>";

    initializeDefaultChatChannel();
    sendMessage(USER_NAME, "Hello, world!");

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.F, false, true,false, false)));
    assertTrue(instance.chatMessageSearchContainer.isVisible());

    runOnFxThreadAndWait(() -> instance.chatMessageSearchTextField.setText("world"));
    assertTrue(instance.getHtmlBodyContent().contains(highlighted));

    runOnFxThreadAndWait(() -> instance.chatMessageSearchTextField.setText(""));
    assertFalse(instance.getHtmlBodyContent().contains(highlighted));

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.F, false, true,false, false)));
    assertFalse(instance.chatMessageSearchContainer.isVisible());
  }

  @Test
  public void testSearchChatMessageAndCloseViaEscape() throws Exception {
    String highlighted = "<span class=\"highlight\">world</span>";

    initializeDefaultChatChannel();
    sendMessage(USER_NAME, "Hello, world!");

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.F, false, true,false, false)));
    assertTrue(instance.chatMessageSearchContainer.isVisible());

    runOnFxThreadAndWait(() -> instance.chatMessageSearchTextField.setText("world"));
    assertTrue(instance.getHtmlBodyContent().contains(highlighted));

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.ESCAPE, false, false,false, false)));
    assertFalse(instance.chatMessageSearchContainer.isVisible());
    assertFalse(instance.getHtmlBodyContent().contains(highlighted));
  }

  @Test
  public void testShowHideChatUserList() {
    initializeDefaultChatChannel();

    assertTrue(instance.chatUserList.isVisible());

    runOnFxThreadAndWait(() -> instance.userListVisibilityToggleButton.fire());
    assertFalse(instance.chatUserList.isVisible());
    assertFalse(preferences.getChat().isPlayerListShown());

    runOnFxThreadAndWait(() -> instance.userListVisibilityToggleButton.fire());
    assertTrue(instance.chatUserList.isVisible());
    assertTrue(preferences.getChat().isPlayerListShown());
    verify(preferencesService, times(2)).storeInBackground();
  }

  @Test
  public void testOnTabClosed() {
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> instance.getRoot().getOnCloseRequest().handle(null));
    verify(chatService).leaveChannel(CHANNEL_NAME);
    verify(chatUserListController).onTabClosed();
  }

  @Test
  public void testGetMessageCssClassModerator() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().moderator(true).get();

    when(chatService.getOrCreateChatUser(USER_NAME, defaultChatChannel.getName())).thenReturn(chatUser);

    initializeDefaultChatChannel();

    assertEquals(ChannelTabController.MODERATOR_STYLE_CLASS, instance.getMessageCssClass(USER_NAME));
  }

  @Test
  public void testAtMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage(CHANNEL_NAME, Instant.now(), USER_NAME, "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testAtMentionTriggersNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage(CHANNEL_NAME, Instant.now(), USER_NAME, "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage(CHANNEL_NAME, Instant.now(), USER_NAME, "hello " + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage(CHANNEL_NAME, Instant.now(), USER_NAME, "hello " + USER_NAME + "!!"));
    verify(audioService, never()).playChatMentionSound();
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationFromFoe() {
    this.getRoot().setVisible(false);
    preferences.getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    when(playerService.getPlayerByNameIfOnline(USER_NAME)).thenReturn(Optional.ofNullable(PlayerBeanBuilder.create().defaultValues().username("junit").socialStatus(FOE).get()));
    instance.onMention(new ChatMessage(CHANNEL_NAME, Instant.now(), USER_NAME, "hello " + USER_NAME + "!!"));
    verify(audioService, never()).playChatMentionSound();
  }

  @Test
  public void getInlineStyleChangeToRandom() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();

    when(chatService.getOrCreateChatUser(USER_NAME, CHANNEL_NAME)).thenReturn(chatUser);
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    Color color = ColorGeneratorUtil.generateRandomColor();
    chatUser.setColor(color);
    WaitForAsyncUtils.waitForFxEvents();

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(USER_NAME);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandom() {
    Color color = ColorGeneratorUtil.generateRandomColor();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();
    chatUser.setColor(color);

    when(chatService.getOrCreateChatUser(USER_NAME, CHANNEL_NAME)).thenReturn(chatUser);
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(USER_NAME);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomFoeHide() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();

    when(playerService.getPlayerByNameIfOnline(USER_NAME))
        .thenReturn(Optional.of(PlayerBeanBuilder.create().defaultValues().username(USER_NAME).socialStatus(FOE).get()));
    when(chatService.getOrCreateChatUser(USER_NAME, CHANNEL_NAME)).thenReturn(chatUser);
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(true);
    });

    String result = instance.getInlineStyle(USER_NAME);
    assertEquals("display: none;", result);
  }

  @Test
  public void getInlineStyleRandomFoeShow() {
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();
    when(playerService.getPlayerByNameIfOnline(USER_NAME))
        .thenReturn(Optional.of(PlayerBeanBuilder.create().defaultValues().username(USER_NAME).socialStatus(FOE).get()));

    when(chatService.getOrCreateChatUser(USER_NAME, CHANNEL_NAME)).thenReturn(chatUser);
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    String result = instance.getInlineStyle(USER_NAME);
    assertEquals("", result);
  }

  @Test
  public void userColorChangeTest() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";
    String after = "<span class=\"text user-junit message\" style=\"color: rgb(0, 255, 255);\">Hello world!</span>";
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();
    ChatChannelUser updatedChatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().color(Color.AQUA).get();
    defaultChatChannel.addUser(chatUser);

    initializeDefaultChatChannel();
    sendMessage(USER_NAME, "Hello world!");
    sendMessage("other", "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> instance.onChatUserColorChange(new ChatUserColorChangeEvent(updatedChatUser)));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  @Test
  public void testOnUserMessageVisibility() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message chat_only\" style=\"display: none;\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";

    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().socialStatus(OTHER).get();
    ChatChannelUser updatedChatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().socialStatus(FOE).get();
    defaultChatChannel.addUser(chatUser);

    initializeDefaultChatChannel();
    sendMessage(USER_NAME, "Hello world!");
    sendMessage("other", "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(updatedChatUser)));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  @Test
  public void testOnChatOnlyUserStyleClassUpdate() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message chat_only\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";

    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().get();
    ChatChannelUser updatedChatOnlyChatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().socialStatus(null).get();
    defaultChatChannel.addUser(chatUser);

    initializeDefaultChatChannel();
    sendMessage(USER_NAME, "Hello world!");
    sendMessage("other", "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(updatedChatOnlyChatUser)));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  @Test
  public void testOnModeratorUserStyleClassUpdate() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message chat_only moderator\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";

    ChatChannelUser chatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().socialStatus(OTHER).moderator(false).get();
    ChatChannelUser updatedChatOnlyChatUser = ChatChannelUserBuilder.create(USER_NAME, CHANNEL_NAME).defaultValues().moderator(true).socialStatus(OTHER).get();
    defaultChatChannel.addUser(chatUser);

    initializeDefaultChatChannel();
    sendMessage(USER_NAME, "Hello world!");
    sendMessage("other", "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> instance.onChatUserCategoryChange(new ChatUserCategoryChangeEvent(updatedChatOnlyChatUser)));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  private void sendMessage(String username, String message) {
    runOnFxThreadAndWait(() -> instance.onChatMessage(new ChatMessage(CHANNEL_NAME, Instant.now(), username, message)));
  }

  private void initializeDefaultChatChannel() {
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel, mock(BooleanBinding.class)));
  }
}
