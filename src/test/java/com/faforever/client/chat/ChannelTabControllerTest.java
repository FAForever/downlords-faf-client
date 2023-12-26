package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.regex.Pattern;

import static com.faforever.client.theme.ThemeService.CHAT_CONTAINER;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_EXTENDED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelTabControllerTest extends PlatformTest {

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
  private ChatMessageViewController chatMessageViewController;
  @Mock
  private EmoticonsWindowController emoticonsWindowController;

  private final ChatChannel defaultChatChannel = new ChatChannel("#testChannel");
  private final ChatChannelUser user = new ChatChannelUser("junit", defaultChatChannel);

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(chatMessageViewController.chatChannelProperty()).thenReturn(new SimpleObjectProperty<>());
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
    lenient().when(chatService.getOrCreateChatUser(any(String.class), eq(defaultChatChannel.getName())))
             .thenReturn(new ChatChannelUser("junit", defaultChatChannel));
    when(chatUserListController.chatChannelProperty()).thenReturn(new SimpleObjectProperty<>());

    Stage stage = mock(Stage.class);
    lenient().when(stage.focusedProperty()).thenReturn(new SimpleBooleanProperty());

    StageHolder.setStage(stage);

    loadFxml("theme/chat/channel_tab.fxml", clazz -> {
      if (clazz == ChatUserListController.class) {
        return chatUserListController;
      }
      if (clazz == ChatMessageViewController.class) {
        return chatMessageViewController;
      }
      if (clazz == EmoticonsWindowController.class) {
        return emoticonsWindowController;
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
    assertThat(instance.topicCharactersLimitLabel.getText(), containsString(Integer.toString(length)));

    runOnFxThreadAndWait(() -> instance.topicTextField.appendText("123"));
    assertThat(instance.topicCharactersLimitLabel.getText(), containsString(Integer.toString(length + 3)));
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

  private void initializeDefaultChatChannel() {
    runOnFxThreadAndWait(() -> instance.setChatChannel(defaultChatChannel));
  }
}
