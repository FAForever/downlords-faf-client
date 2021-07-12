package com.faforever.client.fx;

import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.clan.ClanService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Vault;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class BrowserCallbackTest extends UITest {
  private BrowserCallback instance;
  @Mock
  private PlatformService platformService;
  @Mock
  private UrlPreviewResolver urlPreviewResolver;
  @Mock
  private ReplayService replayService;
  @Mock
  private EventBus eventBus;
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

  @BeforeEach
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    Vault vault = new Vault();
    vault.setReplayDownloadUrlFormat("replayId=%s");
    clientProperties.setVault(vault);
    instance = new BrowserCallback(platformService, clientProperties, urlPreviewResolver, replayService, eventBus, clanService, uiService, playerService, i18n, notificationService);
  }

  @Test
  public void testOpenReplayUrl() {
    instance.openUrl("replayId=12");
    WaitForAsyncUtils.waitForFxEvents();
    verify(eventBus).post(any(ShowReplayEvent.class));
  }
}