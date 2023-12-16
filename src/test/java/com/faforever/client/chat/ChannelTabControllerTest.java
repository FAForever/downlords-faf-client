package com.faforever.client.chat;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Labeled;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.regex.Pattern;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.theme.ThemeService.CHAT_CONTAINER;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_EXTENDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelTabControllerTest extends PlatformTest {

  private static final String CHANNEL_NAME = "#testChannel";

  @InjectMocks
  private ChannelTabController instance;

  @Mock
  private ChatService chatService;
  @Mock
  private TimeService timeService;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;
  @Mock
  private ThemeService themeService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PlatformService platformService;
  @Mock
  private EmoticonService emoticonService;
  @Mock
  private ChatUserListController chatUserListController;
  @Spy
  private ChatPrefs chatPrefs;
  @Spy
  private NotificationPrefs notificationPrefs;

  @Mock
  private EmoticonsWindowController emoticonsWindowController;

  private ChatChannelUser user;
  private ChatChannelUser other;
  private ChatChannel defaultChatChannel;

  @BeforeEach
  public void setUp() throws Exception {
    user = new ChatChannelUser("junit", CHANNEL_NAME);
    other = new ChatChannelUser("other", CHANNEL_NAME);
    defaultChatChannel = new ChatChannel(CHANNEL_NAME);
    lenient().when(uiService.loadFxml("theme/chat/emoticons/emoticons_window.fxml"))
             .thenReturn(emoticonsWindowController);
    lenient().when(emoticonsWindowController.getRoot()).thenReturn(new VBox());
    lenient().when(chatService.getCurrentUsername()).thenReturn(user.getUsername());
    lenient().when(themeService.getThemeFileUrl(CHAT_CONTAINER))
             .thenReturn(getClass().getResource("/theme/chat/chat_container.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_SECTION_COMPACT))
        .thenReturn(getClass().getResource("/theme/chat/compact/chat_section.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_TEXT_COMPACT))
        .thenReturn(getClass().getResource("/theme/chat/compact/chat_text.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_TEXT_EXTENDED)).thenReturn(
        getClass().getResource("/theme/chat/extended/chat_text.html"));
    lenient().when(timeService.asShortTime(any())).thenReturn("now");
    lenient().when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile("-----"));
    lenient().when(chatService.getOrCreateChatUser(any(String.class), eq(CHANNEL_NAME)))
        .thenReturn(new ChatChannelUser("junit", "test"));
    when(chatUserListController.chatChannelProperty()).thenReturn(new SimpleObjectProperty<>());

    Stage stage = mock(Stage.class);
    lenient().when(stage.focusedProperty()).thenReturn(new SimpleBooleanProperty());

    StageHolder.setStage(stage);

    loadFxml("theme/chat/channel_tab.fxml", clazz -> {
      if (clazz == ChatUserListController.class) {
        return chatUserListController;
      }
      return instance;
    });

    runOnFxThreadAndWait(() -> new TabPane().getTabs().add(instance.getRoot()));
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
    defaultChatChannel.setTopic(new ChannelTopic(user, "topic https://example.com/1"));
    initializeDefaultChatChannel();

    assertEquals(2, instance.topicText.getChildren().size());
    assertEquals("topic ", ((Labeled) instance.topicText.getChildren().getFirst()).getText());
    assertEquals("https://example.com/1", ((Labeled) instance.topicText.getChildren().get(1)).getText());
    assertTrue(instance.topicPane.isVisible());
  }

  @Test
  public void testNoChannelTopic() {
    defaultChatChannel.addUser(user);

    defaultChatChannel.setTopic(new ChannelTopic(null, ""));
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
    defaultChatChannel.setTopic(new ChannelTopic(user, "topic1: https://faforever.com"));
    initializeDefaultChatChannel();

    assertEquals(2, instance.topicText.getChildren().size());

    runOnFxThreadAndWait(() -> defaultChatChannel.setTopic(
        new ChannelTopic(null, "topic2: https://faforever.com topic3: https://faforever.com/example")));
    assertEquals(4, instance.topicText.getChildren().size());
    assertEquals("topic2: ", ((Labeled) instance.topicText.getChildren().getFirst()).getText());
    assertEquals("https://faforever.com", ((Labeled) instance.topicText.getChildren().get(1)).getText());
    assertEquals("topic3: ", ((Labeled) instance.topicText.getChildren().get(2)).getText());
    assertEquals("https://faforever.com/example", ((Labeled) instance.topicText.getChildren().get(3)).getText());
  }

  @Test
  public void testChangeTopicButtonForModerators() {
    user.setModerator(true);
    defaultChatChannel.addUser(user);
    initializeDefaultChatChannel();
    assertTrue(instance.changeTopicTextButton.isVisible());
  }

  @Test
  public void testNoChangeTopicButtonForNonModerators() {
    user.setModerator(false);
    defaultChatChannel.addUser(user);
    initializeDefaultChatChannel();
    assertFalse(instance.changeTopicTextButton.isVisible());
  }

  @Test
  public void testCheckModeratorListener() {
    user.setModerator(true);
    defaultChatChannel.addUser(user);
    initializeDefaultChatChannel();
    assertTrue(instance.changeTopicTextButton.isVisible());
    runOnFxThreadAndWait(() -> user.setModerator(false));
    assertFalse(instance.changeTopicTextButton.isVisible());
  }

  @Test
  public void testOnTopicTextFieldEntered() {
    defaultChatChannel.setTopic(new ChannelTopic(user, "topic1: https://faforever.com"));
    initializeDefaultChatChannel();

    runOnFxThreadAndWait(() -> {
      instance.topicTextField.setText("New Topic");
      instance.onTopicTextFieldEntered();
    });

    verify(chatService).setChannelTopic(defaultChatChannel, "New Topic");
  }

  @Test
  public void testOnChangeTopicTextButtonClicked() {
    defaultChatChannel.setTopic(new ChannelTopic(user, "topic1: https://faforever.com"));
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> instance.onChangeTopicTextButtonClicked());
    assertEquals("topic1: https://faforever.com", instance.topicTextField.getText());
    assertTrue(instance.topicTextField.isVisible());
    assertFalse(instance.topicText.isVisible());
  }

  @Test
  public void testOnCancelChangesTopicTextButtonClicked() {
    defaultChatChannel.setTopic(new ChannelTopic(user, "topic: https://faforever.com"));
    initializeDefaultChatChannel();

    runOnFxThreadAndWait(() -> {
      instance.topicTextField.setText("New Topic");
      instance.onCancelChangesTopicTextButtonClicked();
    });

    assertEquals(2, instance.topicText.getChildren().size());
    assertEquals("topic: ", ((Labeled) instance.topicText.getChildren().getFirst()).getText());
    assertEquals("https://faforever.com", ((Labeled) instance.topicText.getChildren().get(1)).getText());
    assertFalse(instance.topicTextField.isVisible());
    assertTrue(instance.topicText.isVisible());
  }

  @Test
  public void textCheckTextTopicLimitListener() {
    defaultChatChannel.setTopic(new ChannelTopic(user, "topic: https://faforever.com"));
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> instance.onChangeTopicTextButtonClicked());
    int length = "topic: https://faforever.com".length();
    assertTrue(instance.topicCharactersLimitLabel.getText().contains(Integer.toString(length)));

    runOnFxThreadAndWait(() -> instance.topicTextField.appendText("123"));
    assertTrue(instance.topicCharactersLimitLabel.getText().contains(Integer.toString(length + +3)));
  }

  @Test
  public void testSearchChatMessage() throws Exception {
    String highlighted = "<span class=\"highlight\">world</span>";

    initializeDefaultChatChannel();
    sendMessage(user, "Hello, world!");

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.F, false, true, false, false)));
    assertTrue(instance.chatMessageSearchContainer.isVisible());

    runOnFxThreadAndWait(() -> instance.chatMessageSearchTextField.setText("world"));
    assertTrue(instance.getHtmlBodyContent().contains(highlighted));

    runOnFxThreadAndWait(() -> instance.chatMessageSearchTextField.setText(""));
    assertFalse(instance.getHtmlBodyContent().contains(highlighted));

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.F, false, true, false, false)));
    assertFalse(instance.chatMessageSearchContainer.isVisible());
  }

  @Test
  public void testSearchChatMessageAndCloseViaEscape() throws Exception {
    String highlighted = "<span class=\"highlight\">world</span>";

    initializeDefaultChatChannel();
    sendMessage(user, "Hello, world!");

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.F, false, true, false, false)));
    assertTrue(instance.chatMessageSearchContainer.isVisible());

    runOnFxThreadAndWait(() -> instance.chatMessageSearchTextField.setText("world"));
    assertTrue(instance.getHtmlBodyContent().contains(highlighted));

    runOnFxThreadAndWait(() -> instance.onChatChannelKeyReleased(new KeyEvent(null, null, KeyEvent.KEY_PRESSED,
        null, null, KeyCode.ESCAPE, false, false, false, false)));
    assertFalse(instance.chatMessageSearchContainer.isVisible());
    assertFalse(instance.getHtmlBodyContent().contains(highlighted));
  }

  @Test
  public void testShowHideChatUserList() {
    initializeDefaultChatChannel();

    assertTrue(instance.chatUserList.isVisible());

    runOnFxThreadAndWait(() -> instance.userListVisibilityToggleButton.fire());
    assertFalse(instance.chatUserList.isVisible());
    assertFalse(chatPrefs.isPlayerListShown());

    runOnFxThreadAndWait(() -> instance.userListVisibilityToggleButton.fire());
    assertTrue(instance.chatUserList.isVisible());
    assertTrue(chatPrefs.isPlayerListShown());
  }

  @Test
  public void testOnTabClosed() {
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> instance.getRoot().getOnClosed().handle(null));
    verify(chatService).leaveChannel(defaultChatChannel);
  }

  @Test
  public void testAtMentionTriggersNotification() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage(Instant.now(), user, "hello @" + user + "!!"));
    verify(chatService).incrementUnreadMessagesCount(1);
  }

  @Test
  public void testAtMentionTriggersNotificationWhenFlagIsEnabled() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage(Instant.now(), user, "hello @" + user.getUsername() + "!!"));
    verify(chatService).incrementUnreadMessagesCount(1);
  }

  @Test
  public void testNormalMentionTriggersNotification() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage(Instant.now(), user, "hello " + user + "!!"));
    verify(chatService).incrementUnreadMessagesCount(1);
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationWhenFlagIsEnabled() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage(Instant.now(), user, "hello " + user + "!!"));
    verify(chatService, never()).incrementUnreadMessagesCount(anyInt());
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationFromFoe() {
    notificationPrefs.notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    user.setPlayer(PlayerBeanBuilder.create().defaultValues().socialStatus(FOE).get());
    instance.onMention(new ChatMessage(Instant.now(), user, "hello " + user + "!!"));
    verify(chatService, never()).incrementUnreadMessagesCount(anyInt());
  }

  @Test
  public void getInlineStyleChangeToRandom() {
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
      chatPrefs.setHideFoeMessages(false);
    });

    defaultChatChannel.addUser(user);

    Color color = ColorGeneratorUtil.generateRandomColor();
    user.setColor(color);
    WaitForAsyncUtils.waitForFxEvents();

    String expected = String.format("color: %s;", JavaFxUtil.toRgbCode(color));
    String result = instance.getInlineStyle(user);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandom() {
    Color color = ColorGeneratorUtil.generateRandomColor();
    user.setColor(color);

    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
      chatPrefs.setHideFoeMessages(false);
    });

    defaultChatChannel.addUser(user);

    String expected = String.format("color: %s;", JavaFxUtil.toRgbCode(color));
    String result = instance.getInlineStyle(user);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomFoeHide() {
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
      chatPrefs.setHideFoeMessages(true);
    });

    user.setPlayer(PlayerBeanBuilder.create().defaultValues().socialStatus(FOE).get());

    String result = instance.getInlineStyle(user);
    assertEquals("display: none;", result);
  }

  @Test
  public void getInlineStyleRandomFoeShow() {
    initializeDefaultChatChannel();
    runOnFxThreadAndWait(() -> {
      chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
      chatPrefs.setHideFoeMessages(false);
    });

    user.setPlayer(PlayerBeanBuilder.create().defaultValues().socialStatus(FOE).get());

    String result = instance.getInlineStyle(user);
    assertEquals("", result);
  }

  @Test
  public void userColorChangeTest() throws Exception {
    String before = "<span class=\"text user-junit message chat_only\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message chat_only\" style=\"\">Hello man!</span>";
    String after = "<span class=\"text user-junit message chat_only\" style=\"color: rgb(0, 255, 255);\">Hello world!</span>";
    defaultChatChannel.addUser(user);

    initializeDefaultChatChannel();
    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> user.setColor(Color.AQUA));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  @Test
  public void testOnUserMessageVisibility() throws Exception {
    String before = "<span class=\"text user-junit message other\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message other\" style=\"display: none;\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message chat_only\" style=\"\">Hello man!</span>";

    PlayerBean player = PlayerBeanBuilder.create().socialStatus(OTHER).get();
    user.setPlayer(player);
    defaultChatChannel.addUser(user);

    initializeDefaultChatChannel();
    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> player.setSocialStatus(FOE));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  @Test
  public void testOnChatOnlyUserStyleClassUpdate() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message chat_only\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message chat_only\" style=\"\">Hello man!</span>";

    user.setPlayer(PlayerBeanBuilder.create().defaultValues().get());
    defaultChatChannel.addUser(user);

    initializeDefaultChatChannel();
    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> user.setPlayer(null));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  @Test
  public void testOnModeratorUserStyleClassUpdate() throws Exception {
    String before = "<span class=\"text user-junit message chat_only\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message chat_only moderator\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message chat_only\" style=\"\">Hello man!</span>";

    defaultChatChannel.addUser(user);

    initializeDefaultChatChannel();
    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertTrue(content.contains(before));
    assertTrue(content.contains(otherBefore));

    runOnFxThreadAndWait(() -> user.setModerator(true));
    content = instance.getHtmlBodyContent();
    assertTrue(content.contains(after));
    assertTrue(content.contains(otherBefore));
  }

  private void sendMessage(ChatChannelUser sender, String message) {
    runOnFxThreadAndWait(() -> instance.onChatMessage(new ChatMessage(Instant.now(), sender, message)));
  }

  private void initializeDefaultChatChannel() {
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));
  }
}
