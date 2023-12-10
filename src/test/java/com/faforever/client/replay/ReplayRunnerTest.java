package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.discord.DiscordRichPresenceService;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.fa.relay.ice.CoturnService;
import com.faforever.client.fa.relay.ice.IceAdapter;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.logging.LoggingService;
import com.faforever.client.map.MapService;
import com.faforever.client.mapstruct.GameMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.patch.GameUpdater;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.LastGamePrefs;
import com.faforever.client.preferences.NotificationPrefs;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.test.ServiceTest;
import com.faforever.client.ui.preferences.GameDirectoryRequiredHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplayRunnerTest extends ServiceTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;
  private static final Integer GPG_PORT = 1234;
  private static final int LOCAL_REPLAY_PORT = 15111;
  private static final String LADDER_1v1_RATING_TYPE = "ladder_1v1";

  @InjectMocks
  private ReplayRunner instance;

  @Mock
  private GameService gameService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private FafServerAccessor fafServerAccessor;
  @Mock
  private MapService mapService;
  @Mock
  private ForgedAllianceLaunchService forgedAllianceLaunchService;
  @Mock
  private GameUpdater gameUpdater;
  @Mock
  private PlayerService playerService;
  @Mock
  private ExecutorService executorService;
  @Mock
  private IceAdapter iceAdapter;
  @Mock
  private ModService modService;
  @Mock
  private FeaturedModService featuredModService;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private PlatformService platformService;
  @Mock
  private DiscordRichPresenceService discordRichPresenceService;
  @Mock
  private LoggingService loggingService;
  @Mock
  private Process process;
  @Mock
  private CoturnService coturnService;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Mock
  private GameDirectoryRequiredHandler gameDirectoryRequiredHandler;
  @Spy
  private GameMapper gameMapper = Mappers.getMapper(GameMapper.class);
  @Spy
  private ClientProperties clientProperties;
  @Spy
  private LastGamePrefs lastGamePrefs;
  @Spy
  private NotificationPrefs notificationPrefs;

  @Captor
  private ArgumentCaptor<Set<String>> simModsCaptor;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(gameMapper);

    lenient().when(preferencesService.isValidGamePath()).thenReturn(true);

    lenient().doAnswer(invocation -> {
      try {
        ((Runnable) invocation.getArgument(0)).run();
      } catch (Exception e) {
        e.printStackTrace();
      }
      return null;
    }).when(executorService).execute(any());
  }

  private void mockStartReplayProcess(Path path, int id) throws IOException {
    when(forgedAllianceLaunchService.startReplay(path, id)).thenReturn(process);
  }

  private void mockStartLiveReplayProcess(URI replayUrl, int gameId) throws IOException {
    when(forgedAllianceLaunchService.startReplay(replayUrl, gameId)).thenReturn(process);
    when(gameService.getByUid(any())).thenReturn(Optional.of(GameBeanBuilder.create().defaultValues().get()));
  }

  @Test
  public void runWithLiveReplayIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.runWithLiveReplay(null, null, null, null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
  }

  @Test
  public void runWithReplayIfNoGameSet() {
    when(preferencesService.isValidGamePath()).thenReturn(false);
    instance.runWithReplay(null, null, null, null, null, null, null);
    verify(gameDirectoryRequiredHandler).onChooseGameDirectory();
  }
}
