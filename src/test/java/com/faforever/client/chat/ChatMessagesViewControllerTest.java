package com.faforever.client.chat;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.ui.StageHolder;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.theme.ThemeService.CHAT_CONTAINER;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_EXTENDED;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_EXTENDED;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChatMessagesViewControllerTest extends PlatformTest {

  @Mock
  private ChatService chatService;
  @Mock
  private TimeService timeService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ThemeService themeService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private EmoticonService emoticonService;
  @Spy
  private ChatPrefs chatPrefs;

  @Mock
  private EmoticonsWindowController emoticonsWindowController;

  @InjectMocks
  private ChatMessageViewController instance;

  private final ChatChannel chatChannel = new ChatChannel("#testChannel");
  private final ChatChannelUser user = new ChatChannelUser("junit", chatChannel);
  private final ChatChannelUser other = new ChatChannelUser("other", chatChannel);


  @BeforeEach
  public void setup() throws Exception {
    lenient().when(themeService.getThemeFileUrl(CHAT_CONTAINER))
             .thenReturn(getClass().getResource("/theme/chat/chat_container.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_SECTION_COMPACT))
             .thenReturn(getClass().getResource("/theme/chat/compact/chat_section.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_TEXT_COMPACT))
             .thenReturn(getClass().getResource("/theme/chat/compact/chat_text.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_SECTION_EXTENDED))
             .thenReturn(getClass().getResource("/theme/chat/extended/chat_section.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_TEXT_EXTENDED))
             .thenReturn(getClass().getResource("/theme/chat/extended/chat_text.html"));
    lenient().when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile("-----"));
    lenient().when(chatService.getCurrentUsername()).thenReturn("junit");
    lenient().when(timeService.asShortTime(any())).thenReturn("now");
    lenient().when(emoticonsWindowController.getRoot()).thenReturn(new VBox());

    Stage stage = mock(Stage.class);
    lenient().when(stage.focusedProperty()).thenReturn(new SimpleBooleanProperty());

    StageHolder.setStage(stage);


    loadFxml("theme/chat/chat_message_view.fxml", clazz -> {
      if (clazz == EmoticonsWindowController.class) {
        return emoticonsWindowController;
      }
      return instance;
    });

    instance.setChatChannel(chatChannel);
  }

  @Test
  public void testUpdateTypingState() {
    instance.messageTextField.appendText("a");
    verify(chatService).setActiveTypingState(any());

    instance.messageTextField.deleteText(0, 1);
    verify(chatService).setDoneTypingState(any());
  }

  @Test
  public void testUpdateTypingStateOnSend() {
    instance.messageTextField.appendText("a");
    verify(chatService).setActiveTypingState(any());

    when(chatService.sendMessageInBackground(any(), any())).thenReturn(completedFuture(null));

    fxApplicationThreadExecutor.executeAndWait(() -> instance.onSendMessage());
    verify(chatService, never()).setDoneTypingState(any());
  }

  @Test
  public void testOnSendMessageSuccessful() {
    String message = "Some message";
    instance.messageTextField.setText(message);
    when(chatService.sendMessageInBackground(any(), any())).thenReturn(completedFuture(null));

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(eq(chatChannel), eq(message));
    assertThat(instance.messageTextField.getText(), is(emptyString()));
    assertThat(instance.messageTextField.isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageFailed() {
    String message = "Some message";
    instance.messageTextField.setText(message);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(chatService.sendMessageInBackground(any(), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(chatChannel, message);
    assertThat(instance.messageTextField.getText(), is(message));
    assertThat(instance.messageTextField.isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionSuccessful() {
    String message = "/me is happy";
    instance.messageTextField.setText(message);
    when(chatService.sendActionInBackground(any(), any())).thenReturn(completedFuture(null));

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendActionInBackground(eq(chatChannel), eq("is happy"));
    assertThat(instance.messageTextField.getText(), is(emptyString()));
    assertThat(instance.messageTextField.isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionFailed() {
    String message = "/me is happy";
    instance.messageTextField.setText(message);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(chatService.sendActionInBackground(any(), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendActionInBackground(chatChannel, "is happy");
    assertThat(instance.messageTextField.getText(), is(message));
    assertThat(instance.messageTextField.isDisable(), is(false));
  }

  @Test
  public void testChannelNamesTransformedToHyperlinks() {
    String output = instance.replaceChannelNamesWithHyperlinks("Go to #moderation and report a user");
    String expected = String.format("Go to %s and report a user", instance.transformToChannelLinkHtml("#moderation"));
    assertThat(output, is(expected));
  }

  @Test
  public void testDuplicateChannelNamesTransformedToHyperlinks() {
    String output = instance.replaceChannelNamesWithHyperlinks(
        "Go to #moderation #moderation #moderation and report a user");
    String expected = String.format("Go to %1$s %1$s %1$s and report a user",
                                    instance.transformToChannelLinkHtml("#moderation"));
    assertThat(output, is(expected));
  }

  @Test
  public void testSeveralChannelNamesTransformedToHyperlinks() {
    String output = instance.replaceChannelNamesWithHyperlinks("#develop #development #test");
    String expected = String.format("%s %s %s", instance.transformToChannelLinkHtml("#develop"),
                                    instance.transformToChannelLinkHtml("#development"),
                                    instance.transformToChannelLinkHtml("#test"));
    assertThat(output, is(expected));
  }

  @Test
  public void testTransformEmoticonShortcodesToImages() {
    when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile(":uef:|:aeon:"));
    when(emoticonService.getBase64SvgContentByShortcode(":uef:")).thenReturn("uefBase64Content");
    when(emoticonService.getBase64SvgContentByShortcode(":aeon:")).thenReturn("aeonBase64Content");

    String text = ":uef: Hello, world :aeon:";
    assertEquals(
        "<img src=\"data:image/svg+xml;base64,uefBase64Content\" width=\"24\" height=\"24\" /> " + "Hello, world <img src=\"data:image/svg+xml;base64,aeonBase64Content\" width=\"24\" height=\"24\" />",
        instance.transformEmoticonShortcodesToImages(text));
  }

  @Test
  public void testMentionPattern() {
    when(chatService.getCurrentUsername()).thenReturn("-Box-");

    runOnFxThreadAndWait(() -> reinitialize(instance));
    assertTrue(instance.mentionPattern.matcher("-Box-").find());
    assertTrue(instance.mentionPattern.matcher("-Box-!").find());
    assertTrue(instance.mentionPattern.matcher("!-Box-").find());
    assertTrue(instance.mentionPattern.matcher("Goodbye -Box-").find());
    assertFalse(instance.mentionPattern.matcher(" ").find());
    assertFalse(instance.mentionPattern.matcher("").find());
    assertFalse(instance.mentionPattern.matcher("-Box-h").find());
    assertFalse(instance.mentionPattern.matcher("h-Box-").find());
  }

  @Test
  public void getInlineStyleChangeToRandom() {
    runOnFxThreadAndWait(() -> {
      chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
      chatPrefs.setHideFoeMessages(false);
    });

    chatChannel.addUser(user);

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

    runOnFxThreadAndWait(() -> {
      chatPrefs.setChatColorMode(ChatColorMode.RANDOM);
      chatPrefs.setHideFoeMessages(false);
    });

    chatChannel.addUser(user);

    String expected = String.format("color: %s;", JavaFxUtil.toRgbCode(color));
    String result = instance.getInlineStyle(user);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomFoeHide() {
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
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";
    String after = "<span class=\"text user-junit message\" style=\"color: rgb(0, 255, 255);\">Hello world!</span>";
    chatChannel.addUser(user);

    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertThat(content, containsString(before));
    assertThat(content, containsString(otherBefore));

    runOnFxThreadAndWait(() -> user.setColor(Color.AQUA));
    content = instance.getHtmlBodyContent();
    assertThat(content, containsString(after));
    assertThat(content, containsString(otherBefore));
  }

  @Test
  public void testOnUserMessageVisibility() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message\" style=\"display: none;\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";

    PlayerBean player = PlayerBeanBuilder.create().socialStatus(OTHER).get();
    user.setPlayer(player);
    chatChannel.addUser(user);

    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertThat(content, containsString(before));
    assertThat(content, containsString(otherBefore));

    runOnFxThreadAndWait(() -> player.setSocialStatus(FOE));
    content = instance.getHtmlBodyContent();
    assertThat(content, containsString(after));
    assertThat(content, containsString(otherBefore));
  }

  @Test
  public void testOnChatOnlyUserStyleClassUpdate() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";

    user.setPlayer(PlayerBeanBuilder.create().defaultValues().get());
    chatChannel.addUser(user);

    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertThat(content, containsString(before));
    assertThat(content, containsString(otherBefore));

    runOnFxThreadAndWait(() -> user.setPlayer(null));
    content = instance.getHtmlBodyContent();
    assertThat(content, containsString(after));
    assertThat(content, containsString(otherBefore));
  }

  @Test
  public void testOnModeratorUserStyleClassUpdate() throws Exception {
    String before = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String after = "<span class=\"text user-junit message\" style=\"\">Hello world!</span>";
    String otherBefore = "<span class=\"text user-other message\" style=\"\">Hello man!</span>";

    chatChannel.addUser(user);

    sendMessage(user, "Hello world!");
    sendMessage(other, "Hello man!");

    String content = instance.getHtmlBodyContent();
    assertThat(content, containsString(before));
    assertThat(content, containsString(otherBefore));

    runOnFxThreadAndWait(() -> user.setModerator(true));
    content = instance.getHtmlBodyContent();
    assertThat(content, containsString(after));
    assertThat(content, containsString(otherBefore));
  }

  private void sendMessage(ChatChannelUser sender, String message) {
    runOnFxThreadAndWait(() -> instance.onChatMessage(new ChatMessage(Instant.now(), sender, message)));
  }

  private KeyEvent keyEvent(KeyCode keyCode, Collection<KeyCode> modifiers) {
    return new KeyEvent(null, null, KeyEvent.KEY_PRESSED, keyCode.getChar(), keyCode.getName(), keyCode,
                        modifiers.contains(KeyCode.SHIFT), modifiers.contains(KeyCode.CONTROL),
                        modifiers.contains(KeyCode.ALT), modifiers.contains(KeyCode.META));
  }
}
