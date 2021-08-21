package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.chat.event.UnreadPartyMessageEvent;
import com.faforever.client.discord.JoinDiscordEvent;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;

import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static com.faforever.client.theme.UiService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.UiService.CHAT_TEXT_COMPACT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MatchmakingChatControllerTest extends UITest {

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
  private AudioService audioService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private EventBus eventBus;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private ChatUserService chatUserService;
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;

  private MatchmakingChatController instance;

  @BeforeEach
  public void setUp() throws Exception {
    Preferences preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(i18n.get(anyString())).thenReturn("");
    when(chatService.getOrCreateChannel("partyName")).thenReturn(new ChatChannel("partyName"));
    when(userService.getUsername()).thenReturn("junit");
    when(uiService.getThemeFileUrl(CHAT_CONTAINER)).thenReturn(getClass().getResource("/theme/chat/chat_container.html"));
    when(uiService.getThemeFileUrl(CHAT_SECTION_COMPACT)).thenReturn(getClass().getResource("/theme/chat/compact/chat_section.html"));
    when(uiService.getThemeFileUrl(CHAT_TEXT_COMPACT)).thenReturn(getClass().getResource("/theme/chat/compact/chat_text.html"));
    when(timeService.asShortTime(any())).thenReturn("");

    instance = new MatchmakingChatController(userService, preferencesService,
        playerService, timeService,
        i18n, imageUploadService, notificationService, reportingService,
        uiService, eventBus,
        audioService, chatService, webViewConfigurer, countryFlagService,
        chatUserService, applicationEventPublisher);
    loadFxml("theme/play/teammatchmaking/matchmaking_chat.fxml", clazz -> instance);

    instance = spy(instance);
  }

  @Test
  public void testSetChannel() {
    instance.setChannel("partyName");

    verify(chatService).getOrCreateChannel("partyName");
    verify(chatService).joinChannel("partyName");
  }

  @Test
  public void testOnChatMessage() {
    doReturn(false).when(instance).hasFocus();

    instance.onChatMessage(new ChatMessage("junit", Instant.now(), "mock", "test", true));

    verify(eventBus).post(any(UnreadPartyMessageEvent.class));
  }

  @Test
  public void testOnJoinDiscordButtonClicked() {
    instance.onDiscordButtonClicked();
    verify(applicationEventPublisher).publishEvent(any(JoinDiscordEvent.class));
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
