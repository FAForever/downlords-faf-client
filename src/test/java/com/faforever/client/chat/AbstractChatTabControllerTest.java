package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractChatTabControllerTest extends PlatformTest {

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
  private EmoticonService emoticonService;
  @Spy
  private ChatPrefs chatPrefs;

  @Mock
  private EmoticonsWindowController emoticonsWindowController;

  private AbstractChatTabController instance;

  @BeforeEach
  public void setup() throws Exception {
    when(themeService.getThemeFileUrl(any())).thenReturn(
        getClass().getResource("/" + ThemeService.CHAT_SECTION_EXTENDED));
    when(chatService.getCurrentUsername()).thenReturn("junit");
    lenient().when(uiService.loadFxml("theme/chat/emoticons/emoticons_window.fxml"))
             .thenReturn(emoticonsWindowController);
    lenient().when(emoticonsWindowController.getRoot()).thenReturn(new VBox());

    fxApplicationThreadExecutor.executeAndWait(() -> {
      instance = new AbstractChatTabController(chatService, timeService, i18n,
                                               notificationService, uiService, themeService, webViewConfigurer,
                                               emoticonService, countryFlagService, chatPrefs,
                                               fxApplicationThreadExecutor) {
        private final Tab root = new Tab();
        private final WebView webView = new WebView();
        private final TextInputControl messageTextField = new TextField();

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
          return webView;
        }
      };
    });

    instance.emoticonsButton = new Button();
    fxApplicationThreadExecutor.executeAndWait(() -> reinitialize(instance));
  }

  @Test
  public void testOnSendMessageSuccessful() {
    String message = "Some message";
    ChatChannel chatChannel = new ChatChannel("#Test");
    instance.messageTextField().setText(message);
    instance.setChatChannel(chatChannel);
    when(chatService.sendMessageInBackground(any(), any())).thenReturn(completedFuture(null));

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(eq(chatChannel), eq(message));
    assertThat(instance.messageTextField().getText(), is(emptyString()));
    assertThat(instance.messageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageFailed() {
    ChatChannel chatChannel = new ChatChannel("#Test");
    String message = "Some message";
    instance.messageTextField().setText(message);
    instance.setChatChannel(chatChannel);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(chatService.sendMessageInBackground(any(), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(chatChannel, message);
    assertThat(instance.messageTextField().getText(), is(message));
    assertThat(instance.messageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionSuccessful() {
    ChatChannel chatChannel = new ChatChannel("#Test");
    String message = "/me is happy";
    instance.messageTextField().setText(message);
    instance.setChatChannel(chatChannel);
    when(chatService.sendActionInBackground(any(), any())).thenReturn(completedFuture(null));

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendActionInBackground(eq(chatChannel), eq("is happy"));
    assertThat(instance.messageTextField().getText(), is(emptyString()));
    assertThat(instance.messageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionFailed() {
    ChatChannel chatChannel = new ChatChannel("#Test");
    String message = "/me is happy";
    instance.messageTextField().setText(message);
    instance.setChatChannel(chatChannel);

    CompletableFuture<Void> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(chatService.sendActionInBackground(any(), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendActionInBackground(chatChannel, "is happy");
    assertThat(instance.messageTextField().getText(), is(message));
    assertThat(instance.messageTextField().isDisable(), is(false));
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
}
