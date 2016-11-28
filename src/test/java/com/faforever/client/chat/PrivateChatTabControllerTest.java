package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.time.Instant;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PrivateChatTabControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ChatPrefs chatPrefs;
  @Mock
  private PlayerService playerService;
  @Mock
  private AudioService audioService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private ChatService chatService;

  private PrivateChatTabController instance;
  private String playerName;

  @Before
  public void setUp() throws IOException {
    instance = new PrivateChatTabController();
    instance.preferencesService = preferencesService;
    instance.playerService = playerService;
    instance.audioService = audioService;
    instance.notificationService = notificationService;
    instance.i18n = i18n;
    instance.webViewConfigurer = webViewConfigurer;
    instance.chatService = chatService;

    WaitForAsyncUtils.asyncFx(() -> instance.stage = new Stage());
    WaitForAsyncUtils.waitForFxEvents();

    playerName = "testUser";
    Player player = new Player(playerName);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getChat()).thenReturn(chatPrefs);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);

    TabPane tabPane = new TabPane();
    tabPane.setSkin(new TabPaneSkin(tabPane));

    loadFxml("theme/chat/private_chat_tab.fxml", clazz -> instance);

    instance.setReceiver(playerName);
    WaitForAsyncUtils.asyncFx(() -> {
      getRoot().getChildren().setAll(tabPane);
      tabPane.getTabs().add(instance.getRoot());
    });
    WaitForAsyncUtils.waitForFxEvents();

    verify(webViewConfigurer).configureWebView(instance.messagesWebView);
  }

  @Test
  public void testOnChatMessageUnfocusedTriggersNotification() throws Exception {
    WaitForAsyncUtils.waitForAsyncFx(5000, () -> getRoot().getScene().getWindow().hide());
    instance.onChatMessage(new ChatMessage(playerName, Instant.now(), playerName, "Test message"));
    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testOnChatMessageFocusedDoesntTriggersNotification() throws Exception {
    instance.onChatMessage(new ChatMessage(playerName, Instant.now(), playerName, "Test message"));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void onChatMessageTestNotFoeShowFoe() {
    when(chatPrefs.getHideFoeMessages()).thenReturn(false);
    instance.onChatMessage(new ChatMessage(playerName, Instant.now(), playerName, "Test message"));
  }

  @Ignore("Not yet implemented")
  @Test
  public void onChatMessageTestNotFoeHideFoe() {

  }

  @Ignore("Not yet implemented")
  @Test
  public void onChatMessageTestIsFoeShowFoe() {

  }

  @Ignore("Not yet implemented")
  @Test
  public void onChatMessageTestIsFoeHideFoe() {

  }

  @Test
  public void onPlayerConnectedTest() {
    assertFalse(instance.isUserOffline());

    instance.onPlayerDisconnected(playerName, null);
    instance.onPlayerConnected(playerName, null);

    assertFalse(instance.isUserOffline());
  }

  @Test
  public void onPlayerDisconnected() {
    assertFalse(instance.isUserOffline());

    instance.onPlayerDisconnected(playerName, null);

    assertTrue(instance.isUserOffline());
  }
}
