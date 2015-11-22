package com.faforever.client.replay;

import com.faforever.client.game.GameService;
import com.faforever.client.game.GameType;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.preferences.PreferencesService;
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
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReplayServiceImplTest {

  /**
   * First 64 bytes of a SCFAReplay file with version 3632. ASCII representation:
   * <pre>
   * Supreme Commande
   * r v1.50.3632....
   * Replay v1.9../ma
   * ps/SCMP_012/SCMP
   * </pre>
   */
  private static final byte[] REPLAY_FIRST_BYTES = new byte[]{
      0x53, 0x75, 0x70, 0x72, 0x65, 0x6D, 0x65, 0x20, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, 0x65,
      0x72, 0x20, 0x76, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x33, 0x36, 0x33, 0x32, 0x00, 0x0D, 0x0A, 0x00,
      0x52, 0x65, 0x70, 0x6C, 0x61, 0x79, 0x20, 0x76, 0x31, 0x2E, 0x39, 0x0D, 0x0A, 0x2F, 0x6D, 0x61,
      0x70, 0x73, 0x2F, 0x53, 0x43, 0x4D, 0x50, 0x5F, 0x30, 0x31, 0x32, 0x2F, 0x53, 0x43, 0x4D, 0x50
  };
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
  private ReplayServerAccessor replayServerAccessor;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private TaskService taskService;
  @Mock
  private GameService gameService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new ReplayServiceImpl();
    instance.i18n = i18n;
    instance.environment = environment;
    instance.preferencesService = preferencesService;
    instance.replayFileReader = replayFileReader;
    instance.notificationService = notificationService;
    instance.replayServerAccessor = replayServerAccessor;
    instance.applicationContext = applicationContext;
    instance.taskService = taskService;
    instance.gameService = gameService;

    when(preferencesService.getReplaysDirectory()).thenReturn(replayDirectory.getRoot().toPath());
    when(preferencesService.getCorruptedReplaysDirectory()).thenReturn(replayDirectory.getRoot().toPath().resolve("corrupt"));
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    when(environment.getProperty("replayFileGlob")).thenReturn("*.fafreplay");
  }

  @Test
  public void testParseSupComVersion() throws Exception {
    Integer version = ReplayServiceImpl.parseSupComVersion(REPLAY_FIRST_BYTES);

    assertEquals((Integer) 3632, version);
  }

  @Test
  public void testGuessModByFileNameModIsMissing() throws Exception {
    String mod = ReplayServiceImpl.guessModByFileName("110621-2128 Saltrock Colony.SCFAReplay");

    assertEquals(GameType.DEFAULT.getString(), mod);
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

    Collection<ReplayInfoBean> localReplays = instance.getLocalReplays();

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

    Collection<ReplayInfoBean> localReplays = instance.getLocalReplays();

    assertThat(localReplays, hasSize(1));
    assertThat(localReplays.iterator().next().getId(), is(123));
    assertThat(localReplays.iterator().next().getTitle(), is("title"));
  }

  @Test
  public void testGetOnlineReplays() throws Exception {
    instance.getOnlineReplays();
    verify(replayServerAccessor).requestOnlineReplays();
  }

  @Test
  public void testRunFafReplayFile() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    ReplayInfoBean replayInfoBean = new ReplayInfoBean();
    replayInfoBean.setReplayFile(replayFile);

    LocalReplayInfo replayInfo = new LocalReplayInfo();
    replayInfo.setUid(123);
    replayInfo.setSimMods(Collections.emptyMap());
    replayInfo.setFeaturedModVersions(emptyMap());
    replayInfo.setFeaturedMod("faf");

    when(replayFileReader.readReplayInfo(replayFile)).thenReturn(replayInfo);
    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    instance.runReplay(replayInfoBean);

    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3632), eq(emptyMap()), eq(emptySet()));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaReplayFile() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    ReplayInfoBean replayInfoBean = new ReplayInfoBean();
    replayInfoBean.setReplayFile(replayFile);

    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    instance.runReplay(replayInfoBean);

    verify(gameService).runWithReplay(any(), eq(null), eq("faf"), eq(3632), eq(emptyMap()), eq(emptySet()));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    doThrow(new RuntimeException("Junit test exception")).when(replayFileReader).readReplayData(replayFile);

    ReplayInfoBean replayInfoBean = new ReplayInfoBean();
    replayInfoBean.setReplayFile(replayFile);

    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Junit test exception");

    instance.runReplay(replayInfoBean);
  }

  @Test
  public void testRunFafReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    doThrow(new IOException("Junit test exception")).when(replayFileReader).readReplayInfo(replayFile);
    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);

    ReplayInfoBean replayInfoBean = new ReplayInfoBean();
    replayInfoBean.setReplayFile(replayFile);

    instance.runReplay(replayInfoBean);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
    verifyNoMoreInteractions(gameService);
  }

  @Test
  public void testRunFafOnlineReplay() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    ReplayInfoBean replayInfoBean = new ReplayInfoBean();

    LocalReplayInfo replayInfo = new LocalReplayInfo();
    replayInfo.setUid(123);
    replayInfo.setSimMods(Collections.emptyMap());
    replayInfo.setFeaturedModVersions(emptyMap());
    replayInfo.setFeaturedMod("faf");

    when(replayFileReader.readReplayInfo(replayFile)).thenReturn(replayInfo);
    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);
    when(taskService.submitTask(replayDownloadTask)).thenReturn(CompletableFuture.completedFuture(replayFile));

    instance.runReplay(replayInfoBean);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3632), eq(emptyMap()), eq(emptySet()));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplay() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    ReplayInfoBean replayInfoBean = new ReplayInfoBean();

    when(replayFileReader.readReplayData(replayFile)).thenReturn(REPLAY_FIRST_BYTES);
    when(taskService.submitTask(replayDownloadTask)).thenReturn(CompletableFuture.completedFuture(replayFile));

    instance.runReplay(replayInfoBean);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(replayFile, null, "faf", 3632, emptyMap(), emptySet());
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplayExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();
    doThrow(new IOException("Junit test exception")).when(replayFileReader).readReplayInfo(replayFile);

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    ReplayInfoBean replayInfoBean = new ReplayInfoBean();

    when(taskService.submitTask(replayDownloadTask)).thenReturn(CompletableFuture.completedFuture(replayFile));

    instance.runReplay(replayInfoBean);

    verify(notificationService).addNotification(any(ImmediateNotification.class));
    verifyNoMoreInteractions(gameService);
  }

  @Test
  public void testRunLiveReplay() throws Exception {
    instance.runLiveReplay(new URI("faflive://example.com/123?mod=faf&map=mapname"));

    verify(gameService).runWithReplay(new URI("gpgnet://example.com/123"), 123);
  }
}
