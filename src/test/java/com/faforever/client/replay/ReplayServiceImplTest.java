package com.faforever.client.replay;

import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReplayServiceImplTest {

  /**
   * First 64 bytes of a SCFAReplay file with version 3599. ASCII representation:
   * <pre>
   * Supreme Commande
   * r v1.50.3599....
   * Replay v1.9../ma
   * ps/forbidden pas
   * s.v0001/forbidde
   * n pass.scmap....
   * </pre>
   */
  private static final byte[] REPLAY_FIRST_BYTES = new byte[]{
      0x53, 0x75, 0x70, 0x72, 0x65, 0x6D, 0x65, 0x20, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, 0x65,
      0x72, 0x20, 0x76, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x33, 0x35, 0x39, 0x39, 0x00, 0x0D, 0x0A, 0x00,
      0x52, 0x65, 0x70, 0x6C, 0x61, 0x79, 0x20, 0x76, 0x31, 0x2E, 0x39, 0x0D, 0x0A, 0x2F, 0x6D, 0x61,
      0x70, 0x73, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65, 0x6E, 0x20, 0x70, 0x61, 0x73,
      0x73, 0x2E, 0x76, 0x30, 0x30, 0x30, 0x31, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65,
      0x6E, 0x20, 0x70, 0x61, 0x73, 0x73, 0x2E, 0x73, 0x63, 0x6D, 0x61, 0x70, 0x00, 0x0D, 0x0A, 0x1A
  };
  private static final String TEST_MAP_NAME = "forbidden pass.v0001";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder replayDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  private ReplayServiceImpl instance;
  @Mock
  private I18n i18n;
  @Mock
  private Environment environment;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ReplayFileReader replayFileReader;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private TaskService taskService;
  @Mock
  private GameService gameService;
  @Mock
  private FafService fafService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ReplayServer replayServer;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new ReplayServiceImpl(environment, preferencesService, replayFileReader, notificationService, gameService,
        taskService, i18n, reportingService, applicationContext, platformService, replayServer, fafService);

    when(preferencesService.getReplaysDirectory()).thenReturn(replayDirectory.getRoot().toPath());
    when(preferencesService.getCorruptedReplaysDirectory()).thenReturn(replayDirectory.getRoot().toPath().resolve("corrupt"));
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(environment.getProperty("replayFileGlob")).thenReturn("*.fafreplay");
    doAnswer(invocation -> invocation.getArgumentAt(0, Object.class)).when(taskService).submitTask(any());
  }

  @Test
  public void testParseSupComVersion() throws Exception {
    Integer version = ReplayServiceImpl.parseSupComVersion(REPLAY_FIRST_BYTES);

    assertEquals((Integer) 3599, version);
  }

  @Test
  public void testParseMapName() throws Exception {
    String mapName = ReplayServiceImpl.parseMapName(REPLAY_FIRST_BYTES);

    assertEquals(TEST_MAP_NAME, mapName);
  }

  @Test
  public void testGuessModByFileNameModIsMissing() throws Exception {
    String mod = ReplayServiceImpl.guessModByFileName("110621-2128 Saltrock Colony.SCFAReplay");

    assertEquals(KnownFeaturedMod.DEFAULT.getString(), mod);
  }

  @Test
  public void testGuessModByFileNameModIsBlackops() throws Exception {
    String mod = ReplayServiceImpl.guessModByFileName("110621-2128 Saltrock Colony.blackops.SCFAReplay");

    assertEquals("blackops", mod);
  }

  @Test
  public void testGetLocalReplaysMovesCorruptFiles() throws Exception {
    Path file1 = replayDirectory.newFile("replay.fafreplay").toPath();
    Path file2 = replayDirectory.newFile("replay2.fafreplay").toPath();

    doThrow(new IOException("Junit test exception")).when(replayFileReader).readReplayInfo(file1);
    doThrow(new IOException("Junit test exception")).when(replayFileReader).readReplayInfo(file2);

    Collection<Replay> localReplays = instance.getLocalReplays();

    assertThat(localReplays, empty());

    verify(replayFileReader).readReplayInfo(file1);
    verify(replayFileReader).readReplayInfo(file2);
    verify(notificationService, times(2)).addNotification(any(PersistentNotification.class));

    assertThat(Files.exists(file1), is(false));
    assertThat(Files.exists(file2), is(false));
  }

  @Test
  public void testGetLocalReplays() throws Exception {
    Path file1 = replayDirectory.newFile("replay.fafreplay").toPath();

    LocalReplayInfo localReplayInfo = new LocalReplayInfo();
    localReplayInfo.setUid(123);
    localReplayInfo.setTitle("title");

    when(replayFileReader.readReplayInfo(file1)).thenReturn(localReplayInfo);

    Collection<Replay> localReplays = instance.getLocalReplays();

    assertThat(localReplays, hasSize(1));
    assertThat(localReplays.iterator().next().getId(), is(123));
    assertThat(localReplays.iterator().next().getTitle(), is("title"));
  }

  @Test
  public void testRunFafReplayFile() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    LocalReplayInfo replayInfo = new LocalReplayInfo();
    replayInfo.setUid(123);
    replayInfo.setSimMods(Collections.emptyMap());
    replayInfo.setFeaturedModVersions(emptyMap());
    replayInfo.setFeaturedMod("faf");
    replayInfo.setMapname(TEST_MAP_NAME);

    when(replayFileReader.readReplayInfo(replayFile)).thenReturn(replayInfo);
    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaReplayFile() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(null), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    doThrow(new RuntimeException("Junit test exception")).when(replayFileReader).readReplayData(replayFile);

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Junit test exception");

    instance.runReplay(replay);
  }

  @Test
  public void testRunFafReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    doThrow(new IOException("Junit test exception")).when(replayFileReader).readReplayInfo(replayFile);
    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    instance.runReplay(replay);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
    verifyNoMoreInteractions(gameService);
  }

  @Test
  public void testRunFafOnlineReplay() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    Replay replay = new Replay();

    LocalReplayInfo replayInfo = new LocalReplayInfo();
    replayInfo.setUid(123);
    replayInfo.setSimMods(Collections.emptyMap());
    replayInfo.setFeaturedModVersions(emptyMap());
    replayInfo.setFeaturedMod("faf");
    replayInfo.setMapname(TEST_MAP_NAME);

    when(replayFileReader.readReplayInfo(replayFile)).thenReturn(replayInfo);
    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    instance.runReplay(replay);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplay() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));

    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    Replay replay = new Replay();

    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    instance.runReplay(replay);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(replayFile, null, "faf", 3599, emptyMap(), emptySet(), TEST_MAP_NAME);
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplayExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();
    doThrow(new IOException("Junit test exception")).when(replayFileReader).readReplayInfo(replayFile);

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    Replay replay = new Replay();

    instance.runReplay(replay);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
    verifyNoMoreInteractions(gameService);
  }

  @Test
  public void testRunLiveReplay() throws Exception {
    when(gameService.runWithLiveReplay(any(URI.class), anyInt(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    instance.runLiveReplay(new URI("faflive://example.com/123/456.scfareplay?mod=faf&map=map%20name"));

    verify(gameService).runWithLiveReplay(new URI("gpgnet://example.com/123/456.scfareplay"), 123, "faf", "map name");
  }
}
