package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.clan.ClanService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.google.common.eventbus.EventBus;
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

import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PrivateChatTabControllerTest extends AbstractPlainJavaFxTest {

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
  @Mock
  private ClanService clanService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private EventBus eventBus;
  @Mock
  private Stage stage;
  @Mock
  private Preferences preferences;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private MapService mapService;
  @Mock
  private PrivateUserInfoController privateUserInfoController;
  @Mock
  private GameDetailController gameDetailController;
  @Mock
  private WatchButtonController watchButtonController;
  @Mock
  private ChatPrefs chatPrefs;

  private PrivateChatTabController instance;
  private String playerName;

  @Before
  public void setUp() throws IOException {
    instance = new PrivateChatTabController(clanService,
        userService, platformService, preferencesService, playerService,
        timeService, i18n, imageUploadService, urlPreviewResolver, notificationService,
        reportingService, uiService, autoCompletionHelper, eventBus, audioService,
        chatService, mapService, webViewConfigurer, countryFlagService
    );

    playerName = "testUser";
    Player player = new Player(playerName);

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferences.getChat()).thenReturn(chatPrefs);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(player);
    when(userService.getUsername()).thenReturn(playerName);
    when(uiService.getThemeFileUrl(CHAT_CONTAINER)).then(invocation -> getThemeFileUrl(invocation.getArgument(0)));

    TabPane tabPane = new TabPane();
    tabPane.setSkin(new TabPaneSkin(tabPane));

    loadFxml("theme/chat/private_chat_tab.fxml", clazz -> {
      if (clazz == PrivateUserInfoController.class) {
        return privateUserInfoController;
      }
      if (clazz == GameDetailController.class) {
        return gameDetailController;
      }
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      return instance;
    });

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
