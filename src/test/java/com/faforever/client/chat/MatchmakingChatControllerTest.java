package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.discord.JoinDiscordEventHandler;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.LoginService;
import com.faforever.client.util.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;

import static com.faforever.client.theme.ThemeService.CHAT_CONTAINER;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_COMPACT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchmakingChatControllerTest extends PlatformTest {

  @Mock
  private ChatService chatService;
  @Mock
  private LoginService loginService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TimeService timeService;
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
  private CountryFlagService countryFlagService;
  @Mock
  private EmoticonService emoticonService;
  @Mock
  private JoinDiscordEventHandler joinDiscordEventHandler;
  @Spy
  private ChatPrefs chatPrefs;

  @InjectMocks
  private MatchmakingChatController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(i18n.get(anyString())).thenReturn("");
    when(chatService.getOrCreateChannel("partyName")).thenReturn(new ChatChannel("partyName"));
    when(loginService.getUsername()).thenReturn("junit");
    when(themeService.getThemeFileUrl(CHAT_CONTAINER)).thenReturn(
        getClass().getResource("/theme/chat/chat_container.html"));
    when(themeService.getThemeFileUrl(CHAT_SECTION_COMPACT)).thenReturn(
        getClass().getResource("/theme/chat/compact/chat_section.html"));
    when(themeService.getThemeFileUrl(CHAT_TEXT_COMPACT)).thenReturn(
        getClass().getResource("/theme/chat/compact/chat_text.html"));
    when(timeService.asShortTime(any())).thenReturn("");

    loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml", clazz -> instance);

    instance = spy(instance);
  }

  @Test
  public void testOnJoinDiscordButtonClicked() throws Exception {
    instance.onDiscordButtonClicked();
    verify(joinDiscordEventHandler).onJoin(any());
  }

  @Test
  public void onPlayerConnectedTest() {
    instance.onPlayerConnected("mock");
    WaitForAsyncUtils.waitForFxEvents();

    verify(instance).onChatMessage(any(ChatMessage.class));
  }

  @Test
  public void onPlayerDisconnected() {
    instance.onPlayerDisconnected("mock");
    WaitForAsyncUtils.waitForFxEvents();

    verify(instance).onChatMessage(any(ChatMessage.class));
  }
}
