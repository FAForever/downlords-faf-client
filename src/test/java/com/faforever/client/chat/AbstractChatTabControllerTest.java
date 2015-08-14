package com.faforever.client.chat;

import com.faforever.client.audio.AudioController;
import com.faforever.client.fx.HostService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Callback;
import com.faforever.client.util.TimeService;
import javafx.collections.FXCollections;
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
import javafx.scene.layout.Pane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractChatTabControllerTest extends AbstractPlainJavaFxTest {

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Mock
  ChatService chatService;

  @Mock
  UserService userService;

  @Mock
  PreferencesService preferencesService;

  @Mock
  Preferences preferences;

  @Mock
  ChatPrefs chatPrefs;

  @Mock
  PlayerService playerService;

  @Mock
  PlayerInfoTooltipController playerInfoTooltipController;

  @Mock
  HostService hostService;

  @Mock
  UrlPreviewResolver urlPreviewResolver;

  @Mock
  TimeService timeService;

  @Mock
  AudioController audioController;

  @Mock
  ImageUploadService imageUploadService;

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
      protected WebView getMessagesWebView() {
        return webView;
      }

      @Override
      public Tab getRoot() {
        return root;
      }

      @Override
      protected TextInputControl getMessageTextField() {
        return messageTextField;
      }
    };
    instance.chatService = chatService;
    instance.userService = userService;
    instance.preferencesService = preferencesService;
    instance.playerService = playerService;
    instance.playerInfoTooltipController = playerInfoTooltipController;
    instance.hostService = hostService;
    instance.urlPreviewResolver = urlPreviewResolver;
    instance.timeService = timeService;
    instance.audioController = audioController;
    instance.imageUploadService = imageUploadService;

    TabPane tabPane = new TabPane(instance.getRoot());
    getRoot().getChildren().setAll(tabPane);

    when(timeService.asShortTime(any())).thenReturn("123");
    when(userService.getUsername()).thenReturn("junit");
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getCacheDirectory()).thenReturn(tempDir.getRoot().toPath());
    when(preferences.getTheme()).thenReturn("default");
    when(preferences.getChat()).thenReturn(chatPrefs);

    chatReadyLatch = new CountDownLatch(1);
    instance.getMessagesWebView().getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
      if (Worker.State.SUCCEEDED.equals(newValue)) {
        chatReadyLatch.countDown();
      }
    });

    instance.postConstruct();
  }

  @Test
  public void testOnSendMessageSuccessful() throws Exception {
    String receiver = "receiver";
    String message = "Some message";
    instance.getMessageTextField().setText(message);
    instance.setReceiver(receiver);

    instance.onSendMessage();

    ArgumentCaptor<Callback<String>> argumentCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(chatService).sendMessageInBackground(eq(receiver), any(), argumentCaptor.capture());

    argumentCaptor.getValue().success(message);
    assertThat(instance.getMessageTextField().getText(), isEmptyString());
    assertThat(instance.getMessageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageFailed() throws Exception {
    String receiver = "receiver";
    String message = "Some message";
    instance.getMessageTextField().setText(message);
    instance.setReceiver(receiver);

    instance.onSendMessage();

    ArgumentCaptor<Callback<String>> argumentCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(chatService).sendMessageInBackground(eq(receiver), any(), argumentCaptor.capture());

    argumentCaptor.getValue().error(new Exception("junit fake exception"));
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

    instance.onSendMessage();

    ArgumentCaptor<Callback<String>> argumentCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(chatService).sendActionInBackground(eq(receiver), any(), argumentCaptor.capture());

    argumentCaptor.getValue().success(message);
    assertThat(instance.getMessageTextField().getText(), isEmptyString());
    assertThat(instance.getMessageTextField().isDisable(), is(false));
  }

  @Test
  public void testOnSendMessageSendActionFailed() throws Exception {
    String receiver = "receiver";
    String message = "/me is happy";
    instance.getMessageTextField().setText(message);
    instance.setReceiver(receiver);

    instance.onSendMessage();

    ArgumentCaptor<Callback<String>> argumentCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(chatService).sendActionInBackground(eq(receiver), any(), argumentCaptor.capture());

    argumentCaptor.getValue().error(new Exception("junit test exception"));
    assertThat(instance.getMessageTextField().getText(), is(message));
    assertThat(instance.getMessageTextField().isDisable(), is(false));
  }

  @Test
  public void testPlayerInfo() throws Exception {
    String playerName = "somePlayer";
    PlayerInfoBean playerInfoBean = new PlayerInfoBean(playerName);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(playerInfoBean);
    when(playerInfoTooltipController.getRoot()).thenReturn(new Pane());

    WaitForAsyncUtils.waitForAsyncFx(100, () -> instance.playerInfo(playerName));

    verify(playerService).getPlayerForUsername(playerName);
    verify(playerInfoTooltipController).setPlayerInfoBean(eq(playerInfoBean));
  }

  @Test
  public void testHidePlayerInfoDoesNotThrowExceptionWhenNoTooltipDisplayed() throws Exception {
    instance.hidePlayerInfo();
  }

  @Test
  public void testHidePlayerInfo() throws Exception {
    String playerName = "somePlayer";
    PlayerInfoBean playerInfoBean = new PlayerInfoBean(playerName);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(playerInfoBean);
    when(playerInfoTooltipController.getRoot()).thenReturn(new Pane());

    WaitForAsyncUtils.waitForAsyncFx(100, () -> {
      instance.playerInfo(playerName);
      instance.hidePlayerInfo();
    });
    // I don't see what could be verified here
  }

  @Test
  public void testOpenUrl() throws Exception {
    String url = "http://www.example.com";

    instance.openUrl(url);

    verify(hostService).showDocument(url);
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

    WaitForAsyncUtils.waitForAsyncFx(100, () -> {
      instance.previewUrl(url);
      instance.hideUrlPreview();
    });
    // I don't see what could be verified here
  }

  @Test
  public void testAutoCompleteWithEmptyText() throws Exception {
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    instance.onKeyPressed(keyEvent);

    assertThat(keyEvent.isConsumed(), is(true));
    assertThat(instance.getMessageTextField().getText(), isEmptyString());
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
  public void testAutoCompleteDoesntCompleteWhenTheresNoWordBeforeCaret() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("DummyUser", "Junit"));
    instance.getMessageTextField().setText("j");
    instance.getMessageTextField().positionCaret(0);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    instance.onKeyPressed(keyEvent);

    assertThat(keyEvent.isConsumed(), is(true));
    assertThat(instance.getMessageTextField().getText(), is("j"));
  }

  @Test
  public void testAutoCompleteCompletesToFirstMatchCaseInsensitive() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("DummyUser", "Junit"));
    instance.getMessageTextField().setText("j");
    instance.getMessageTextField().positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    instance.onKeyPressed(keyEvent);

    assertThat(keyEvent.isConsumed(), is(true));
    assertThat(instance.getMessageTextField().getText(), is("Junit"));
  }

  @Test
  public void testAutoCompleteCompletesToFirstMatchCaseInsensitiveRepeated() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("DummyUser", "Junit"));
    instance.getMessageTextField().setText("j");
    instance.getMessageTextField().positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    instance.onKeyPressed(keyEvent);
    instance.onKeyPressed(keyEvent);

    assertThat(keyEvent.isConsumed(), is(true));
    assertThat(instance.getMessageTextField().getText(), is("Junit"));
  }

  @Test
  public void testAutoCompleteCycles() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("JayUnit", "Junit"));
    instance.getMessageTextField().setText("j");
    instance.getMessageTextField().positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    instance.onKeyPressed(keyEvent);
    assertThat(instance.getMessageTextField().getText(), is("JayUnit"));

    instance.onKeyPressed(keyEvent);
    assertThat(instance.getMessageTextField().getText(), is("Junit"));

    instance.onKeyPressed(keyEvent);
    assertThat(instance.getMessageTextField().getText(), is("JayUnit"));
  }

  @Test
  public void testAutoCompleteSortedByName() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("JBunit", "JAyUnit"));
    instance.getMessageTextField().setText("j");
    instance.getMessageTextField().positionCaret(1);
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    instance.onKeyPressed(keyEvent);

    assertThat(keyEvent.isConsumed(), is(true));
    assertThat(instance.getMessageTextField().getText(), is("JAyUnit"));
  }

  @Test
  public void testAutoCompleteCaretMovedAway() throws Exception {
    when(playerService.getPlayerNames()).thenReturn(FXCollections.observableSet("JUnit", "Downlord"));
    KeyEvent keyEvent = keyEvent(KeyCode.TAB);

    // Start auto completion on "JB"
    instance.getMessageTextField().setText("JU Do");
    instance.getMessageTextField().positionCaret(2);
    instance.onKeyPressed(keyEvent);

    // Then auto complete on "Do"
    instance.getMessageTextField().positionCaret(instance.getMessageTextField().getText().length());
    instance.onKeyPressed(keyEvent);

    assertThat(keyEvent.isConsumed(), is(true));
    assertThat(instance.getMessageTextField().getText(), is("JUnit Downlord"));
  }

  @Test
  public void testOnChatMessage() throws Exception {
    instance.onChatMessage(new ChatMessage(Instant.now(), "junit", "Test message"));
  }

  @Test
  public void testOnChatMessageAction() throws Exception {
    instance.onChatMessage(new ChatMessage(Instant.now(), "junit", "Test action", true));
  }

  @Test
  public void testHasFocus() throws Exception {
    assertThat(instance.hasFocus(), is(true));
  }

  @Test
  public void testPasteImageCtrlV() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> {
      Image image = new Image(getClass().getResourceAsStream("/images/tray_icon.png"));

      ClipboardContent clipboardContent = new ClipboardContent();
      clipboardContent.putImage(image);
      Clipboard.getSystemClipboard().setContent(clipboardContent);

      instance.getMessageTextField().getOnKeyReleased().handle(
          keyEvent(KeyCode.V, singletonList(KeyCode.CONTROL))
      );
    });

    ArgumentCaptor<Callback<String>> argumentCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(imageUploadService).uploadImageInBackground(any(), argumentCaptor.capture());

    String url = "http://www.example.com/fake.png";
    argumentCaptor.getValue().success(url);

    assertThat(instance.getMessageTextField().getText(), is(url));
  }

  @Test
  public void testPasteImageShiftInsert() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(1000, () -> {
      Image image = new Image(getClass().getResourceAsStream("/images/tray_icon.png"));

      ClipboardContent clipboardContent = new ClipboardContent();
      clipboardContent.putImage(image);
      Clipboard.getSystemClipboard().setContent(clipboardContent);

      instance.getMessageTextField().getOnKeyReleased().handle(
          keyEvent(KeyCode.INSERT, singletonList(KeyCode.SHIFT))
      );
    });

    ArgumentCaptor<Callback<String>> argumentCaptor = ArgumentCaptor.forClass(Callback.class);
    verify(imageUploadService).uploadImageInBackground(any(), argumentCaptor.capture());

    String url = "http://www.example.com/fake.png";
    argumentCaptor.getValue().success(url);

    assertThat(instance.getMessageTextField().getText(), is(url));
  }
}
