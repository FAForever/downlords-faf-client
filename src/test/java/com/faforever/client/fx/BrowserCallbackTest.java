package com.faforever.client.fx;

import com.faforever.client.chat.UrlPreviewResolver;
import com.faforever.client.clan.ClanService;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.config.ClientProperties.Vault;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.ShowReplayEvent;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.replay.Replay;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BrowserCallbackTest extends AbstractPlainJavaFxTest {
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

  @Before
  public void setUp() throws Exception {
    ClientProperties clientProperties = new ClientProperties();
    Vault vault = new Vault();
    vault.setReplayDownloadUrlFormat("replayId=%s");
    clientProperties.setVault(vault);
    instance = new BrowserCallback(platformService, clientProperties, urlPreviewResolver, replayService, eventBus, clanService, uiService, playerService, i18n, notificationService);
  }

  @Test
  public void testOpenReplayUrl() {
    Replay replay = new Replay();
    when(replayService.findById(12)).thenReturn(CompletableFuture.completedFuture(Optional.of(replay)));
    instance.openUrl("replayId=12");
    WaitForAsyncUtils.waitForFxEvents();
    verify(eventBus).post(any(ShowReplayEvent.class));
  }
}