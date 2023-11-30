package com.faforever.client.fx;

import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.clan.ClanService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.navigation.NavigationHandler;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.testfx.util.WaitForAsyncUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class BrowserCallbackTest extends PlatformTest {

  @InjectMocks
  private BrowserCallback instance;
  @Mock
  private PlatformService platformService;
  @Mock
  private UrlPreviewResolver urlPreviewResolver;
  @Mock
  private ReplayService replayService;
  @Mock
  private ClanService clanService;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private NavigationHandler navigationHandler;
  @Spy
  private ClientProperties clientProperties;

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties.getVault().setReplayDownloadUrlFormat("replayId=%s");
    instance.afterPropertiesSet();
  }

  @Test
  public void testOpenReplayUrl() {
    instance.openUrl("replayId=12");
    WaitForAsyncUtils.waitForFxEvents();
    verify(navigationHandler).navigateTo(any(ShowReplayEvent.class));
  }
}