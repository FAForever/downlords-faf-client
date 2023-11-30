package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.avatar.AvatarService;
import com.faforever.client.builders.AvatarBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.domain.AvatarBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.game.GameDetailController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.player.PrivatePlayerInfoController;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.replay.WatchButtonController;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TabPane;
import javafx.scene.control.skin.TabPaneSkin;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PrivateChatTabControllerTest extends PlatformTest {

  @Mock
  private LoginService loginService;
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
  private ThemeService themeService;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private ReportingService reportingService;
  @Mock
  private NavigationHandler navigationHandler;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PrivatePlayerInfoController privatePlayerInfoController;
  @Mock
  private GameDetailController gameDetailController;
  @Mock
  private WatchButtonController watchButtonController;
  @Mock
  private EmoticonService emoticonService;
  @Mock
  private AvatarService avatarService;
  @Mock
  private ChatService chatService;
  @Spy
  private ChatPrefs chatPrefs;
  @Spy
  private NotificationPrefs notificationPrefs;

  private PrivateChatTabController instance;

  private String playerName;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new PrivateChatTabController(loginService, playerService, timeService, i18n, notificationService,
                                            uiService, themeService, navigationHandler, chatService, webViewConfigurer,
                                            countryFlagService,
                                            emoticonService, avatarService, chatPrefs, notificationPrefs,
                                            fxApplicationThreadExecutor);

    player = PlayerBeanBuilder.create().defaultValues().get();
    playerName = player.getUsername();

    when(playerService.getPlayerByNameIfOnline(playerName)).thenReturn(Optional.of(player));
    when(loginService.getUsername()).thenReturn(playerName);
    when(timeService.asShortTime(any())).thenReturn("");
    when(i18n.get(any(), any())).then(invocation -> invocation.getArgument(0));
    when(themeService.getThemeFileUrl(any())).then(invocation -> getThemeFileUrl(invocation.getArgument(0)));
    when(emoticonService.getEmoticonShortcodeDetectorPattern()).thenReturn(Pattern.compile(".*"));
    when(privatePlayerInfoController.chatUserProperty()).thenReturn(new SimpleObjectProperty<>());
    when(avatarService.loadAvatar(player.getAvatar())).thenReturn(new Image(InputStream.nullInputStream()));

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
      tabPane.getTabs().add(instance.getRoot());
      instance.setChatChannel(new ChatChannel(playerName));
    });
    verify(webViewConfigurer).configureWebView(eq(instance.messagesWebView));
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
    assertTrue(instance.avatarImageView.isVisible());
    assertNotNull(instance.avatarImageView.getImage());
    assertFalse(instance.defaultIconImageView.isVisible());
  }

  @Test
  public void checkSetDefaultIconForTabIfPlayerHasNoAvatar() {
    player.setAvatar(null);
    assertFalse(instance.avatarImageView.isVisible());
    assertNull(instance.avatarImageView.getImage());
    assertTrue(instance.defaultIconImageView.isVisible());
  }

  @Test
  public void checkPlayerAvatarListener() throws Exception {
    assertNotNull(instance.avatarImageView.getImage());

    Image newAvatar = new Image(InputStream.nullInputStream());
    AvatarBean avatarBean = AvatarBeanBuilder.create().defaultValues().url(new URL("https://test11.com")).get();
    when(avatarService.loadAvatar(avatarBean)).thenReturn(newAvatar);
    runOnFxThreadAndWait(() -> player.setAvatar(avatarBean));
    assertEquals(newAvatar, instance.avatarImageView.getImage());
  }

  @Test
  public void testOnClosedTab() {
  }
}
