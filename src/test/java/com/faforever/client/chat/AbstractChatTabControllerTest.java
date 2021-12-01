package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.SocialStatus;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.concurrent.Worker;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.bridj.Platform;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import static com.faforever.client.chat.AbstractChatTabController.CSS_CLASS_CHAT_ONLY;
import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.SELF;
import static java.util.Collections.singletonList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractChatTabControllerTest extends UITest {

  private static final long TIMEOUT = 5000;

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
  private AudioService audioService;
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
  private ReportingService reportingService;
  @Mock
  private EventBus eventBus;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private ChatUserService chatUserService;
  @Mock
  private EmoticonService emoticonService;

  private AbstractChatTabController instance;
  private CountDownLatch chatReadyLatch;


  @Override
  public void start(Stage stage) throws Exception {
    super.start(stage);

    Preferences preferences = PreferencesBuilder.create().defaultValues().get();

    when(uiService.getThemeFileUrl(any())).thenReturn(getClass().getResource("/" + UiService.CHAT_SECTION_EXTENDED));
    when(timeService.asShortTime(any())).thenReturn("123");
    when(userService.getUsername()).thenReturn("junit");
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile(":uef:|:aeon:"));
    when(emoticonService.getBase64SvgContentByShortcode(":uef:")).thenReturn("uefBase64Content");
    when(emoticonService.getBase64SvgContentByShortcode(":aeon:")).thenReturn("aeonBase64Content");


    instance = new AbstractChatTabController(webViewConfigurer, userService, chatService, preferencesService,
        playerService, audioService, timeService, i18n, imageUploadService, notificationService, reportingService,
        uiService, eventBus, countryFlagService, chatUserService, emoticonService) {
      private final Tab root = new Tab();
      private final WebView webView = new WebView();
      private final TextInputControl messageTextField = new TextField();
      private final Button emoticonsButton = new Button();

      @Override
      public Tab getRoot() {
        return root;
      }

      @Override
      protected TextInputControl messageTextField() {
        return messageTextField;
      }

      @Override
      public Button getEmoticonsButton() {
        return emoticonsButton;
      }

      @Override
      protected WebView getMessagesWebView() {
        return webView;
      }
    };

    TabPane tabPane = new TabPane(instance.getRoot());
    getRoot().getChildren().setAll(tabPane);

    chatReadyLatch = new CountDownLatch(1);
    JavaFxUtil.addListener(instance.getMessagesWebView().getEngine().getLoadWorker().stateProperty(), (observable, oldValue, newValue) -> {
      if (Worker.State.SUCCEEDED.equals(newValue)) {
        chatReadyLatch.countDown();
      }
    });

    instance.initialize();
  }

  @Test
  public void testOnSendMessageSuccessful() {
    String receiver = "receiver";
    String message = "Some message";
    instance.messageTextField().setText(message);
    instance.setReceiver(receiver);
    when(chatService.sendMessageInBackground(eq(receiver), any())).thenReturn(completedFuture(message));

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(eq(receiver), eq(message));
    assertThat(instance.messageTextField().getText(), is(isEmptyString()));
    assertThat(instance.messageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageFailed() {
    String receiver = "receiver";
    String message = "Some message";
    instance.messageTextField().setText(message);
    instance.setReceiver(receiver);

    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(chatService.sendMessageInBackground(eq(receiver), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendMessageInBackground(receiver, message);
    assertThat(instance.messageTextField().getText(), is(message));
    assertThat(instance.messageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionSuccessful() {
    String receiver = "receiver";
    String message = "/me is happy";
    instance.messageTextField().setText(message);
    instance.setReceiver(receiver);
    when(chatService.sendActionInBackground(eq(receiver), any())).thenReturn(completedFuture(message));

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendActionInBackground(eq(receiver), eq("is happy"));
    assertThat(instance.messageTextField().getText(), is(isEmptyString()));
    assertThat(instance.messageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionFailed() {
    String receiver = "receiver";
    String message = "/me is happy";
    instance.messageTextField().setText(message);
    instance.setReceiver(receiver);

    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(new FakeTestException());
    when(chatService.sendActionInBackground(eq(receiver), any())).thenReturn(future);

    runOnFxThreadAndWait(() -> instance.onSendMessage());

    verify(chatService).sendActionInBackground(receiver, "is happy");
    assertThat(instance.messageTextField().getText(), is(message));
    assertThat(instance.messageTextField().isDisable(), is(false));
  }

  @NotNull
  private KeyEvent keyEvent(KeyCode keyCode, Collection<KeyCode> modifiers) {
    return new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "\u0000", "", keyCode,
        modifiers.contains(KeyCode.SHIFT),
        modifiers.contains(KeyCode.CONTROL), modifiers.contains(KeyCode.ALT),
        modifiers.contains(KeyCode.META));
  }


  @Test
  public void testOnChatMessage() {
    // TODO assert something, maybe we can spy on engine
    instance.onChatMessage(new ChatMessage("", Instant.now(), "junit", "Test message"));
  }

  @Test
  public void testOnChatMessageAction() {
    // TODO assert something, maybe we can spy on engine
    instance.onChatMessage(new ChatMessage("", Instant.now(), "junit", "Test action", true));
  }

  @Test
  public void testHasFocus() {
    assertThat(instance.hasFocus(), is(true));
  }

  @Test
  public void testPasteImageCtrlV() {
    KeyCode modifier;
    if (Platform.isMacOSX()) {
      modifier = KeyCode.META;
    } else {
      modifier = KeyCode.CONTROL;
    }

    Image image = new Image(getClass().getResourceAsStream("/theme/images/default_achievement.png"));

    String url = "http://www.example.com/fake.png";
    when(imageUploadService.uploadImageInBackground(any())).thenReturn(completedFuture(url));

    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> {
      ClipboardContent clipboardContent = new ClipboardContent();
      clipboardContent.putImage(image);
      Clipboard.getSystemClipboard().setContent(clipboardContent);

      instance.messageTextField().getOnKeyReleased().handle(
          keyEvent(KeyCode.V, singletonList(modifier))
      );
    });

    assertThat(instance.messageTextField().getText(), is(url));
  }

  @Test
  public void testPasteImageShiftInsert() {
    Image image = new Image(getClass().getResourceAsStream("/theme/images/default_achievement.png"));

    String url = "http://www.example.com/fake.png";
    when(imageUploadService.uploadImageInBackground(any())).thenReturn(completedFuture(url));

    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> {
      ClipboardContent clipboardContent = new ClipboardContent();
      clipboardContent.putImage(image);
      Clipboard.getSystemClipboard().setContent(clipboardContent);

      instance.messageTextField().getOnKeyReleased().handle(
          keyEvent(KeyCode.INSERT, singletonList(KeyCode.SHIFT))
      );
    });

    assertThat(instance.messageTextField().getText(), is(url));
  }

  @Test
  public void getMessageCssClassFriend() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    player.setSocialStatus(FRIEND);
    when(playerService.getPlayerByNameIfOnline(player.getUsername())).thenReturn(Optional.of(player));
    assertEquals(instance.getMessageCssClass(player.getUsername()), SocialStatus.FRIEND.getCssClass());
  }

  @Test
  public void getMessageCssClassFoe() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    player.setSocialStatus(FOE);
    when(playerService.getPlayerByNameIfOnline(player.getUsername())).thenReturn(Optional.of(player));
    assertEquals(instance.getMessageCssClass(player.getUsername()), SocialStatus.FOE.getCssClass());
  }

  @Test
  public void getMessageCssClassChatOnly() {
    String playerName = "somePlayer";
    when(playerService.getPlayerByNameIfOnline(playerName)).thenReturn(Optional.empty());
    assertEquals(instance.getMessageCssClass(playerName), CSS_CLASS_CHAT_ONLY);
  }

  @Test
  public void getMessageCssClassSelf() {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    player.setSocialStatus(SELF);
    when(playerService.getPlayerByNameIfOnline(player.getUsername())).thenReturn(Optional.of(player));
    assertEquals(instance.getMessageCssClass(player.getUsername()), SocialStatus.SELF.getCssClass());
  }

  @Test
  public void getMessageCssClassChatOnlyNullPlayerInfoBean() {
    String playerName = "somePlayer";
    assertEquals(instance.getMessageCssClass(playerName), CSS_CLASS_CHAT_ONLY);
  }

  @Test
  public void testChannelNamesTransformedToHyperlinks() {
    String output = instance.replaceChannelNamesWithHyperlinks("Go to #moderation and report a user");
    String expected = String.format("Go to %s and report a user", instance.transformToChannelLinkHtml("#moderation"));
    assertThat(output, is(expected));
  }

  @Test
  public void testDuplicateChannelNamesTransformedToHyperlinks() {
    String output = instance.replaceChannelNamesWithHyperlinks("Go to #moderation #moderation #moderation and report a user");
    String expected = String.format("Go to %1$s %1$s %1$s and report a user", instance.transformToChannelLinkHtml("#moderation"));
    assertThat(output, is(expected));
  }

  @Test
  public void testSeveralChannelNamesTransformedToHyperlinks() {
    String output = instance.replaceChannelNamesWithHyperlinks("#develop #development #test");
    String expected = String.format("%s %s %s", instance.transformToChannelLinkHtml("#develop"),
        instance.transformToChannelLinkHtml("#development"), instance.transformToChannelLinkHtml("#test"));
    assertThat(output, is(expected));
  }

  @Test
  public void testTransformEmoticonShortcodesToImages() {
    String text = ":uef: Hello, world :aeon:";
    assertEquals("<img src=\"data:image/svg+xml;base64,uefBase64Content\" width=\"24\" height=\"24\" /> " +
        "Hello, world <img src=\"data:image/svg+xml;base64,aeonBase64Content\" width=\"24\" height=\"24\" />",
        instance.transformEmoticonShortcodesToImages(text));
  }

  @Test
  public void testMentionPattern() {
    when(userService.getUsername()).thenReturn("-Box-");
    runOnFxThreadAndWait(() -> instance.initialize());
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
  public void testOpenEmoticonsPopupWindow() {
    EmoticonsWindowController controller = mock(EmoticonsWindowController.class);
    when(uiService.loadFxml("theme/chat/emoticons/emoticons_window.fxml")).thenReturn(controller);
    when(controller.getRoot()).thenReturn(new VBox());

    runOnFxThreadAndWait(() -> {
      instance.getRoot().setContent(instance.getEmoticonsButton());
      instance.openEmoticonsPopupWindow();
    });
    assertTrue(instance.emoticonsPopupWindowWeakReference != null && instance.emoticonsPopupWindowWeakReference.get() != null);
    assertTrue(instance.emoticonsPopupWindowWeakReference.get().isShowing());
  }
}
