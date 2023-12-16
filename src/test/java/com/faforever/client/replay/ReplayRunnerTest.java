package com.faforever.client.replay;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.config.ClientProperties;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fa.ForgedAllianceLaunchService;
import com.faforever.client.featuredmod.FeaturedModService;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.GamePathHandler;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplayRunnerTest extends ServiceTest {

  @InjectMocks
  private ReplayRunner instance;

  @Mock
  private ForgedAllianceLaunchService forgedAllianceLaunchService;
  @Mock
  private MapService mapService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;
  @Mock
  private ModService modService;
  @Mock
  private FeaturedModService featuredModService;
  @Mock
  private GameService gameService;
  @Mock
  private GamePathHandler gamePathHandler;
  @Mock
  private FxApplicationThreadExecutor fxApplicationThreadExecutor;
  @Mock
  private PlayerService playerService;
  @Spy
  private ClientProperties clientProperties;

  @Mock
  private Process process;

  @BeforeEach
  public void setUp() throws Exception {
    clientProperties.getReplay().setRemoteHost("localhost");
    clientProperties.getReplay().setRemotePort(15000);

    lenient().when(playerService.getCurrentPlayer()).thenReturn(PlayerBeanBuilder.create().defaultValues().get());
    lenient().doAnswer(invocation -> {
      invocation.getArgument(0, Runnable.class).run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());

    instance.afterPropertiesSet();
  }

  @Test
  public void testDownloadMapWithErrorIgnore() {
    when(mapService.downloadIfNecessary(any())).thenReturn(failedFuture(new Exception()));

    CompletableFuture<Void> future = instance.downloadMapAskIfError("test");

    verify(mapService).downloadIfNecessary("test");

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());

    ImmediateNotification notification = captor.getValue();
    notification.getActions().getFirst().call(null);

    assertDoesNotThrow(() -> future.get(1, TimeUnit.SECONDS));
  }

  @Test
  public void testDownloadMapWithError() {
    when(mapService.downloadIfNecessary(any())).thenReturn(
        CompletableFuture.runAsync(() -> {throw new RuntimeException();}));

    CompletableFuture<Void> future = instance.downloadMapAskIfError("test");

    verify(mapService).downloadIfNecessary("test");

    ArgumentCaptor<ImmediateNotification> captor = ArgumentCaptor.forClass(ImmediateNotification.class);
    verify(notificationService).addNotification(captor.capture());

    ImmediateNotification notification = captor.getValue();
    notification.getActions().get(1).call(null);

    assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
  }

  private void mockStartReplayProcess() {
    lenient().when(preferencesService.hasValidGamePath()).thenReturn(true);
    lenient().when(featuredModService.updateFeaturedMod(any(), any(), any(), anyBoolean()))
             .thenReturn(completedFuture(null));
    lenient().when(modService.downloadAndEnableMods(any())).thenReturn(completedFuture(null));
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    lenient().when(process.onExit()).thenReturn(new CompletableFuture<>());
    lenient().when(process.isAlive()).thenReturn(true);
    lenient().when(forgedAllianceLaunchService.startReplay(any(Path.class), any())).thenReturn(process);
  }

  @Test
  public void killRunningReplay() {
    mockStartReplayProcess();

    instance.runWithReplay(Path.of(""), null, KnownFeaturedMod.FAF.getTechnicalName(), 3, Map.of(), Set.of("a"),
                           "test");

    instance.killReplay();

    verify(process).destroy();
  }

  @Test
  public void runWithReplay() {
    mockStartReplayProcess();

    instance.runWithReplay(Path.of(""), null, KnownFeaturedMod.FAF.getTechnicalName(), 3, Map.of(), Set.of("a"),
                           "test");

    verify(featuredModService).updateFeaturedMod(KnownFeaturedMod.FAF.getTechnicalName(), Map.of(), 3, true);
    verify(mapService).downloadIfNecessary("test");
    verify(modService).downloadAndEnableMods(Set.of("a"));
    verify(forgedAllianceLaunchService).startReplay(any(Path.class), any());
    assertTrue(instance.isRunning());
  }

  @Test
  public void runWithReplayAlreadyRunning() {
    mockStartReplayProcess();

    Path path = Path.of("");
    instance.runWithReplay(path, null, KnownFeaturedMod.FAF.getTechnicalName(), 3, Map.of(), Set.of("a"), "test");
    instance.runWithReplay(path, null, KnownFeaturedMod.FAF.getTechnicalName(), 3, Map.of(), Set.of("a"), "test");

    verify(forgedAllianceLaunchService).startReplay(any(Path.class), any());
  }

  @Test
  public void runWithReplayIfNoGameSet() {
    mockStartReplayProcess();

    when(preferencesService.hasValidGamePath()).thenReturn(false);
    CompletableFuture<Void> chosenFuture = new CompletableFuture<>();
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(chosenFuture);

    instance.runWithReplay(Path.of(""), null, null, null, null, null, null);

    verify(gamePathHandler).chooseAndValidateGameDirectory();

    when(preferencesService.hasValidGamePath()).thenReturn(true);
    chosenFuture.complete(null);

    verify(forgedAllianceLaunchService).startReplay(any(Path.class), any());
  }

  private void mockStartLiveReplayProcess(GameBean game) {
    lenient().when(preferencesService.hasValidGamePath()).thenReturn(true);
    lenient().when(gameService.getByUid(any())).thenReturn(Optional.of(game));
    lenient().when(featuredModService.updateFeaturedModToLatest(any(), anyBoolean())).thenReturn(completedFuture(null));
    lenient().when(modService.downloadAndEnableMods(any())).thenReturn(completedFuture(null));
    lenient().when(mapService.downloadIfNecessary(any())).thenReturn(completedFuture(null));
    lenient().when(process.onExit()).thenReturn(new CompletableFuture<>());
    lenient().when(process.isAlive()).thenReturn(true);
    lenient().when(forgedAllianceLaunchService.startReplay(any(URI.class), any())).thenReturn(process);
  }

  @Test
  public void runWithLiveReplay() {
    GameBean game = GameBeanBuilder.create().defaultValues().simMods(Map.of("a", "name")).get();
    mockStartLiveReplayProcess(game);

    instance.runWithLiveReplay(game);

    verify(featuredModService).updateFeaturedModToLatest(KnownFeaturedMod.FAF.getTechnicalName(), true);
    verify(mapService).downloadIfNecessary(game.getMapFolderName());
    verify(modService).downloadAndEnableMods(game.getSimMods().keySet());
    verify(forgedAllianceLaunchService).startReplay(any(URI.class), any());
    assertTrue(instance.isRunning());
  }

  @Test
  public void runWithLiveReplayAlreadyRunning() {
    mockStartLiveReplayProcess(GameBeanBuilder.create().defaultValues().get());

    GameBean game = GameBeanBuilder.create().defaultValues().get();
    instance.runWithLiveReplay(game);
    instance.runWithLiveReplay(game);

    verify(forgedAllianceLaunchService).startReplay(any(URI.class), any());
    verify(notificationService).addImmediateWarnNotification(anyString());
  }

  @Test
  public void runWithLiveReplayIfNoGameSet() {
    mockStartLiveReplayProcess(GameBeanBuilder.create().defaultValues().get());

    when(preferencesService.hasValidGamePath()).thenReturn(false);
    CompletableFuture<Void> chosenFuture = new CompletableFuture<>();
    when(gamePathHandler.chooseAndValidateGameDirectory()).thenReturn(chosenFuture);

    instance.runWithLiveReplay(GameBeanBuilder.create().defaultValues().get());

    verify(gamePathHandler).chooseAndValidateGameDirectory();

    when(preferencesService.hasValidGamePath()).thenReturn(true);
    chosenFuture.complete(null);

    verify(forgedAllianceLaunchService).startReplay(any(URI.class), any());
  }
}
