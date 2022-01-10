package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.TransientNotification;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.PrivatePlayerInfoController;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.google.common.eventbus.EventBus;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PrivateChatTabControllerTest extends UITest {

  @Mock
  private ChatService chatService;
  @Mock
  private UserService userService;
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
  private PrivatePlayerInfoController privatePlayerInfoController;
  @Mock
  private GameDetailController gameDetailController;
  @Mock
  private WatchButtonController watchButtonController;
  @Mock
  private ChatUserService chatUserService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private EmoticonService emoticonService;

  @InjectMocks
  private PrivateChatTabController instance;
  private String playerName;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().notificationsPrefs().privateMessageToastEnabled(true).then().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();
    playerName = player.getUsername();

    when(playerService.getPlayerByNameIfOnline(playerName)).thenReturn(Optional.of(player));
    when(userService.getUsername()).thenReturn(playerName);
    when(timeService.asShortTime(any())).thenReturn("");
    when(i18n.get(any(), any())).then(invocation -> invocation.getArgument(0));
    when(uiService.getThemeFileUrl(any())).then(invocation -> getThemeFileUrl(invocation.getArgument(0)));

    TabPane tabPane = new TabPane();
    tabPane.setSkin(new TabPaneSkin(tabPane));

    loadFxml("theme/chat/private_chat_tab.fxml", clazz -> {
      if (clazz == PrivatePlayerInfoController.class) {
        return privatePlayerInfoController;
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
    }).get();

    verify(webViewConfigurer).configureWebView(eq(instance.messagesWebView));
  }

  @Test
  public void testOnChatMessageUnfocusedTriggersNotification() {
    // TODO this test throws exceptions if another test runs before it or after it, but not if run alone
    // In that case AbstractChatTabController.hasFocus throws NPE because tabPane.getScene().getWindow() is null
    WaitForAsyncUtils.waitForAsyncFx(5000, () -> getRoot().getScene().getWindow().hide());
    instance.onChatMessage(new ChatMessage(playerName, Instant.now(), playerName, "Test message"));
    verify(notificationService).addNotification(any(TransientNotification.class));
  }

  @Test
  public void testOnChatMessageFocusedDoesntTriggersNotification() {
    instance.onChatMessage(new ChatMessage(playerName, Instant.now(), playerName, "Test message"));
    verifyNoInteractions(notificationService);
  }

  @Test
  public void onPlayerConnectedTest() {
    assertFalse(instance.isUserOffline());

    instance.onPlayerDisconnected(playerName);
    instance.onPlayerConnected(playerName);

    assertFalse(instance.isUserOffline());
  }

  @Test
  public void onPlayerDisconnected() {
    assertFalse(instance.isUserOffline());

    instance.onPlayerDisconnected(playerName);

    assertTrue(instance.isUserOffline());
  }
}
