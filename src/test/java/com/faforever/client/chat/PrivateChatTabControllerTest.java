package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.ChatChannelUserBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.domain.AvatarBean;
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
import javafx.collections.FXCollections;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.net.URL;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
  @Mock
  private AvatarService avatarService;

  @InjectMocks
  private PrivateChatTabController instance;

  private String playerName;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().notificationsPrefs().privateMessageToastEnabled(true).then().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);

    player = PlayerBeanBuilder.create().defaultValues().get();
    playerName = player.getUsername();

    when(playerService.getPlayerByNameIfOnline(playerName)).thenReturn(Optional.of(player));
    when(userService.getUsername()).thenReturn(playerName);
    when(timeService.asShortTime(any())).thenReturn("");
    when(i18n.get(any(), any())).then(invocation -> invocation.getArgument(0));
    when(uiService.getThemeFileUrl(any())).then(invocation -> getThemeFileUrl(invocation.getArgument(0)));
    when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile(".*"));
    when(chatService.getOrCreateChatUser(playerName, playerName, false)).thenReturn(ChatChannelUserBuilder.create(playerName, playerName).get());
    when(chatService.getMutedUserIds()).thenReturn(FXCollections.observableSet());

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

    runOnFxThreadAndWait(() -> {
      TabPane tabPane = new TabPane();
      tabPane.setSkin(new TabPaneSkin(tabPane));
      getRoot().getChildren().setAll(tabPane);
      tabPane.getTabs().add(instance.getRoot());
      instance.setReceiver(playerName);
    });
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
  public void testIgnoreMessageWhenUserIsMuted() {
    when(chatService.isUserMuted(playerName)).thenReturn(true);
    instance.onChatMessage(new ChatMessage(playerName, Instant.now(), playerName, "Test message"));
    verifyNoInteractions(notificationService);
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

  @Test
  public void checkSetAvatarToTabIfPlayerHasAvatar() {
    when(avatarService.loadAvatar(player.getAvatar())).thenReturn(mock(Image.class));
    runOnFxThreadAndWait(() -> instance.setReceiver(playerName));
    assertTrue(instance.avatarImageView.isVisible());
    assertNotNull(instance.avatarImageView.getImage());
    assertFalse(instance.defaultIconImageView.isVisible());
  }

  @Test
  public void checkSetDefaultIconForTabIfPlayerHasNoAvatar() {
    player.setAvatar(null);
    runOnFxThreadAndWait(() -> instance.setReceiver(playerName));
    assertFalse(instance.avatarImageView.isVisible());
    assertNull(instance.avatarImageView.getImage());
    assertTrue(instance.defaultIconImageView.isVisible());
  }

  @Test
  public void checkPlayerAvatarListener() throws Exception {
    Image oldAvatar = mock(Image.class);
    when(avatarService.loadAvatar(player.getAvatar())).thenReturn(oldAvatar);

    runOnFxThreadAndWait(() -> instance.setReceiver(playerName));
    assertEquals(oldAvatar, instance.avatarImageView.getImage());

    Image newAvatar = mock(Image.class);
    AvatarBean avatarBean = AvatarBeanBuilder.create().defaultValues().url(new URL("https://test11.com")).get();
    when(avatarService.loadAvatar(avatarBean)).thenReturn(newAvatar);
    runOnFxThreadAndWait(() -> player.setAvatar(avatarBean));
    assertEquals(newAvatar, instance.avatarImageView.getImage());
  }

  @Test
  public void testOnMuteButtonClickedWhenUserIsNotMuted() {
    when(chatService.isUserMuted(playerName)).thenReturn(false);
    runOnFxThreadAndWait(() -> instance.onMuteButtonClicked());
    verify(chatService).muteUser(playerName);
  }

  @Test
  public void testOnMuteButtonClickedWhenUserIsMuted() {
    when(chatService.isUserMuted(playerName)).thenReturn(true);
    runOnFxThreadAndWait(() -> instance.onMuteButtonClicked());
    verify(chatService).unmuteUser(playerName);
  }
}
