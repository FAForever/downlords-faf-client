package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import javafx.concurrent.Worker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.bridj.Platform;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static com.faforever.client.chat.AbstractChatTabController.CSS_CLASS_CHAT_ONLY;
import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static com.faforever.client.chat.SocialStatus.OTHER;
import static com.faforever.client.chat.SocialStatus.SELF;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractChatTabControllerTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();
  @Mock
  private ChatService chatService;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private PlatformService platformService;
  @Mock
  private UrlPreviewResolver urlPreviewResolver;
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
  private AutoCompletionHelper autoCompletionHelper;
  @Mock
  private UiService uiService;
  @Mock
  private WebViewConfigurer webViewConfigurer;

  private Preferences preferences;
  private AbstractChatTabController instance;
  private CountDownLatch chatReadyLatch;

  @Override
  public void start(Stage stage) throws Exception {
    super.start(stage);

    instance = new AbstractChatTabController() {
      private final Tab root = new Tab();
      private final WebView webView = new WebView();
      private final TextInputControl messageTextField = new TextField();

      @Override
      public Tab getRoot() {
        return root;
      }

      @Override
      protected TextInputControl getMessageTextField() {
        return messageTextField;
      }

      @Override
      protected WebView getMessagesWebView() {
        return webView;
      }
    };
    instance.chatService = chatService;
    instance.userService = userService;
    instance.preferencesService = preferencesService;
    instance.playerService = playerService;
    instance.platformService = platformService;
    instance.urlPreviewResolver = urlPreviewResolver;
    instance.timeService = timeService;
    instance.audioService = audioService;
    instance.imageUploadService = imageUploadService;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.stage = stage;
    instance.autoCompletionHelper = autoCompletionHelper;
    instance.webViewConfigurer = webViewConfigurer;
    instance.uiService = uiService;

    TabPane tabPane = new TabPane(instance.getRoot());
    getRoot().getChildren().setAll(tabPane);

    preferences = new Preferences();

    when(uiService.getThemeFileUrl(any())).thenReturn(getClass().getResource("/theme/chat/chat_section.html"));
    when(timeService.asShortTime(any())).thenReturn("123");
    when(userService.getUsername()).thenReturn("junit");
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getCacheDirectory()).thenReturn(tempDir.getRoot().toPath());

    chatReadyLatch = new CountDownLatch(1);
    instance.getMessagesWebView().getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
      if (Worker.State.SUCCEEDED.equals(newValue)) {
        chatReadyLatch.countDown();
      }
    });

    instance.initialize();
  }

  @Test
  public void testOnSendMessageSuccessful() throws Exception {
    String receiver = "receiver";
    String message = "Some message";
    instance.getMessageTextField().setText(message);
    instance.setReceiver(receiver);
    when(chatService.sendMessageInBackground(eq(receiver), any())).thenReturn(CompletableFuture.completedFuture(message));

    instance.onSendMessage();

    verify(chatService).sendMessageInBackground(eq(receiver), eq(message));
    assertThat(instance.getMessageTextField().getText(), isEmptyString());
    assertThat(instance.getMessageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageFailed() throws Exception {
    String receiver = "receiver";
    String message = "Some message";
    instance.getMessageTextField().setText(message);
    instance.setReceiver(receiver);

    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(new Exception("junit fake exception"));
    when(chatService.sendMessageInBackground(eq(receiver), any())).thenReturn(future);

    instance.onSendMessage();

    verify(chatService).sendMessageInBackground(receiver, message);
    assertThat(instance.getMessageTextField().getText(), is(message));
    assertThat(instance.getMessageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionSuccessful() throws Exception {
    String receiver = "receiver";
    String message = "/me is happy";
    instance.getMessageTextField().setText(message);
    instance.setReceiver(receiver);
    when(timeService.asShortTime(any())).thenReturn("123");
    when(chatService.sendActionInBackground(eq(receiver), any())).thenReturn(CompletableFuture.completedFuture(message));

    instance.onSendMessage();

    verify(chatService).sendActionInBackground(eq(receiver), eq("is happy"));
    assertThat(instance.getMessageTextField().getText(), isEmptyString());
    assertThat(instance.getMessageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionFailed() throws Exception {
    String receiver = "receiver";
    String message = "/me is happy";
    instance.getMessageTextField().setText(message);
    instance.setReceiver(receiver);

    CompletableFuture<String> future = new CompletableFuture<>();
    future.completeExceptionally(new Exception("junit fake exception"));
    when(chatService.sendActionInBackground(eq(receiver), any())).thenReturn(future);

    instance.onSendMessage();

    verify(chatService).sendActionInBackground(receiver, "is happy");
    assertThat(instance.getMessageTextField().getText(), is(message));
    assertThat(instance.getMessageTextField().isDisable(), is(false));
  }

  @Test
  public void testPlayerInfo() throws Exception {
    String playerName = "somePlayer";
    Player player = new Player(playerName);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);

    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> instance.playerInfo(playerName));

    verify(playerService).getPlayerForUsername(playerName);
  }

  @Test
  public void testHidePlayerInfoDoesNotThrowExceptionWhenNoTooltipDisplayed() throws Exception {
    instance.hidePlayerInfo();
  }

  @Test
  public void testHidePlayerInfo() throws Exception {
    String playerName = "somePlayer";
    Player player = new Player(playerName);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);

    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> {
      instance.playerInfo(playerName);
      instance.hidePlayerInfo();
    });
    // I don't see what could be verified here
  }

  @Test
  public void testOpenUrl() throws Exception {
    String url = "http://www.example.com";

    instance.openUrl(url);

    verify(platformService).showDocument(url);
  }

  @Test
  public void testPreviewUrlReturnsNull() throws Exception {
    String url = "http://www.example.com";

    instance.previewUrl(url);

    verify(urlPreviewResolver).resolvePreview(url);
  }

  @Test
  public void testPreviewUrlReturnsPreview() throws Exception {
    String url = "http://www.example.com";
    UrlPreviewResolver.Preview preview = mock(UrlPreviewResolver.Preview.class);
    when(urlPreviewResolver.resolvePreview(url)).thenReturn(preview);

    WaitForAsyncUtils.waitForAsyncFx(1000, () -> instance.previewUrl(url));

    verify(urlPreviewResolver).resolvePreview(url);
    verify(preview).getNode();
  }

  @Test
  public void testHideUrlPreviewNullDoesntThrowException() throws Exception {
    instance.hideUrlPreview();
  }

  @Test
  public void testHideUrlPreview() throws Exception {
    String url = "http://www.example.com";
    UrlPreviewResolver.Preview preview = mock(UrlPreviewResolver.Preview.class);
    when(urlPreviewResolver.resolvePreview(url)).thenReturn(preview);

    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> {
      instance.previewUrl(url);
      instance.hideUrlPreview();
    });
    // I don't see what could be verified here
  }


  @NotNull
  private KeyEvent keyEvent(KeyCode keyCode) {
    return keyEvent(keyCode, emptyList());
  }

  @NotNull
  private KeyEvent keyEvent(KeyCode keyCode, Collection<KeyCode> modifiers) {
    return new KeyEvent(null, null, KeyEvent.KEY_PRESSED, "\u0000", "", keyCode,
        modifiers.contains(KeyCode.SHIFT),
        modifiers.contains(KeyCode.CONTROL), modifiers.contains(KeyCode.ALT),
        modifiers.contains(KeyCode.META));
  }


  @Test
  public void testOnChatMessage() throws Exception {
    // TODO assert something, maybe we can spy on engine
    instance.onChatMessage(new ChatMessage("", Instant.now(), "junit", "Test message"));
  }

  @Test
  public void testOnChatMessageAction() throws Exception {
    // TODO assert something, maybe we can spy on engine
    instance.onChatMessage(new ChatMessage("", Instant.now(), "junit", "Test action", true));
  }

  @Test
  public void testHasFocus() throws Exception {
    assertThat(instance.hasFocus(), is(true));
  }

  @Test
  public void testPasteImageCtrlV() throws Exception {
    KeyCode modifier;
    if (Platform.isMacOSX()) {
      modifier = KeyCode.META;
    } else {
      modifier = KeyCode.CONTROL;
    }

    Image image = new Image(getClass().getResourceAsStream("/theme/images/close.png"));

    String url = "http://www.example.com/fake.png";
    when(imageUploadService.uploadImageInBackground(any())).thenReturn(CompletableFuture.completedFuture(url));

    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> {
      ClipboardContent clipboardContent = new ClipboardContent();
      clipboardContent.putImage(image);
      Clipboard.getSystemClipboard().setContent(clipboardContent);

      instance.getMessageTextField().getOnKeyReleased().handle(
          keyEvent(KeyCode.V, singletonList(modifier))
      );
    });

    assertThat(instance.getMessageTextField().getText(), is(url));
  }

  @Test
  public void testPasteImageShiftInsert() throws Exception {
    Image image = new Image(getClass().getResourceAsStream("/theme/images/close.png"));

    String url = "http://www.example.com/fake.png";
    when(imageUploadService.uploadImageInBackground(any())).thenReturn(CompletableFuture.completedFuture(url));

    WaitForAsyncUtils.waitForAsyncFx(TIMEOUT, () -> {
      ClipboardContent clipboardContent = new ClipboardContent();
      clipboardContent.putImage(image);
      Clipboard.getSystemClipboard().setContent(clipboardContent);

      instance.getMessageTextField().getOnKeyReleased().handle(
          keyEvent(KeyCode.INSERT, singletonList(KeyCode.SHIFT))
      );
    });

    assertThat(instance.getMessageTextField().getText(), is(url));
  }

  @Test
  public void getMessageCssClassFriend() throws Exception {
    String playerName = "somePlayer";
    Player player = new Player(playerName);
    player.setSocialStatus(FRIEND);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);
    assertEquals(instance.getMessageCssClass(playerName), SocialStatus.FRIEND.getCssClass());
  }

  @Test
  public void getMessageCssClassFoe() throws Exception {
    String playerName = "somePlayer";
    Player player = new Player(playerName);
    player.setSocialStatus(FOE);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);
    assertEquals(instance.getMessageCssClass(playerName), SocialStatus.FOE.getCssClass());
  }

  @Test
  public void getMessageCssClassChatOnly() throws Exception {
    String playerName = "somePlayer";
    Player player = new Player(playerName);
    player.setSocialStatus(OTHER);
    player.setChatOnly(true);
    assertEquals(instance.getMessageCssClass(playerName), CSS_CLASS_CHAT_ONLY);
  }

  @Test
  public void getMessageCssClassSelf() throws Exception {
    String playerName = "junit";
    Player player = new Player(playerName);
    player.setSocialStatus(SELF);
    player.setChatOnly(false);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);
    assertEquals(instance.getMessageCssClass(playerName), SocialStatus.SELF.getCssClass());
  }

  @Test
  public void getMessageCssClassChatOnlyNullPlayerInfoBean() throws Exception {
    String playerName = "somePlayer";
    assertEquals(instance.getMessageCssClass(playerName), CSS_CLASS_CHAT_ONLY);
  }

  @Test
  public void getInlineStyleCustom() throws Exception {
    Color color = ColorGeneratorUtil.generateRandomColor();
    String colorStyle = instance.createInlineStyleFromColor(color);
    ChatUser chatUser = new ChatUser("somePlayer", color);

    preferences.getChat().setChatColorMode(ChatColorMode.CUSTOM);
    when(chatService.getOrCreateChatUser("somePlayer")).thenReturn(chatUser);
    preferences.getChat().setHideFoeMessages(false);

    String expected = String.format("%s%s", colorStyle, "");
    String result = instance.getInlineStyle("somePlayer");
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomOther() throws Exception {
    String somePlayer = "somePlayer";
    Color color = ColorGeneratorUtil.generateRandomColor();

    ChatUser chatUser = new ChatUser(somePlayer, color);
    when(playerService.getPlayerForUsername(somePlayer)).thenReturn(PlayerBuilder.create(somePlayer).chatOnly(true).get());

    preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
    when(chatService.getOrCreateChatUser(somePlayer)).thenReturn(chatUser);
    preferences.getChat().setHideFoeMessages(false);

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(somePlayer);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomChatOnly() throws Exception {
    Color color = ColorGeneratorUtil.generateRandomColor();
    String somePlayer = "somePlayer";

    ChatUser chatUser = new ChatUser(somePlayer, color);
    when(playerService.getPlayerForUsername(somePlayer)).thenReturn(PlayerBuilder.create(somePlayer).chatOnly(true).get());

    preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
    when(chatService.getOrCreateChatUser(somePlayer)).thenReturn(chatUser);
    preferences.getChat().setHideFoeMessages(false);

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(somePlayer);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomFoeHide() throws Exception {
    String playerName = "playerName";
    ChatUser chatUser = new ChatUser(playerName, null);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(PlayerBuilder.create(playerName).socialStatus(FOE).get());

    preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
    when(chatService.getOrCreateChatUser(playerName)).thenReturn(chatUser);
    preferences.getChat().setHideFoeMessages(true);

    String result = instance.getInlineStyle(playerName);
    assertEquals("display: none;", result);
  }

  @Test
  public void getInlineStyleRandomFoeShow() throws Exception {
    String playerName = "somePlayer";
    ChatUser chatUser = new ChatUser(playerName, null);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(PlayerBuilder.create(playerName).socialStatus(FOE).get());

    preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
    when(chatService.getOrCreateChatUser(playerName)).thenReturn(chatUser);
    preferences.getChat().setHideFoeMessages(false);

    String result = instance.getInlineStyle(playerName);
    assertEquals("", result);
  }
}
