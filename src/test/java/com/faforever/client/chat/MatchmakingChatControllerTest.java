package com.faforever.client.chat;

import com.faforever.client.chat.emoticons.EmoticonService;
import com.faforever.client.chat.emoticons.EmoticonsWindowController;
import com.faforever.client.discord.JoinDiscordEventHandler;
import com.faforever.client.domain.PartyBean;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.CountryFlagService;
import com.faforever.client.preferences.ChatPrefs;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.teammatchmaking.TeamMatchmakingService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.ThemeService;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import javafx.beans.property.SimpleObjectProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import static com.faforever.client.theme.ThemeService.CHAT_CONTAINER;
import static com.faforever.client.theme.ThemeService.CHAT_SECTION_COMPACT;
import static com.faforever.client.theme.ThemeService.CHAT_TEXT_COMPACT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

public class MatchmakingChatControllerTest extends PlatformTest {

  @Mock
  private ChatService chatService;
  @Mock
  private TimeService timeService;
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
  @Mock
  private TeamMatchmakingService teamMatchmakingService;
  @Spy
  private ChatPrefs chatPrefs;

  @Mock
  private ChatMessageViewController chatMessageViewController;
  @Mock
  private EmoticonsWindowController emoticonsWindowController;

  @InjectMocks
  private MatchmakingChatController instance;

  @BeforeEach
  public void setUp() throws Exception {
    lenient().when(teamMatchmakingService.getParty()).thenReturn(new PartyBean());
    lenient().when(chatMessageViewController.chatChannelProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(chatService.getCurrentUsername()).thenReturn("junit");
    lenient().when(i18n.get(anyString())).thenReturn("");
    lenient().when(chatService.getOrCreateChannel("partyName")).thenReturn(new ChatChannel("partyName"));
    lenient().when(themeService.getThemeFileUrl(CHAT_CONTAINER)).thenReturn(
        getClass().getResource("/theme/chat/chat_container.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_SECTION_COMPACT)).thenReturn(
        getClass().getResource("/theme/chat/compact/chat_section.html"));
    lenient().when(themeService.getThemeFileUrl(CHAT_TEXT_COMPACT)).thenReturn(
        getClass().getResource("/theme/chat/compact/chat_text.html"));
    lenient().when(timeService.asShortTime(any())).thenReturn("");

    loadFxml("theme/chat/matchmaking_chat.fxml", clazz -> {
      if (clazz == ChatMessageViewController.class) {
        return chatMessageViewController;
      }
      if (clazz == EmoticonsWindowController.class) {
        return emoticonsWindowController;
      }

      return instance;
    });
  }

  @Test
  public void testOnJoinDiscordButtonClicked() throws Exception {
    instance.onDiscordButtonClicked();
    verify(joinDiscordEventHandler).onJoin(any());
  }
}
