package com.faforever.client.patch;

import com.faforever.client.game.FeaturedModBeanBuilder;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.mod.FeaturedModBean;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class GitRepositoryFeaturedModUpdaterTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  @Rule
  public final TemporaryFolder reposDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder faDirectory = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private Environment environment;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private TaskService taskService;
  @Mock
  private I18n i18n;
  @Mock
  private GitWrapper gitWrapper;
  @Mock
  private NotificationService notificationService;
  @Mock
  private Preferences preferences;

  private GitRepositoryFeaturedModUpdater instance;

  @Before
  public void setUp() throws Exception {
    instance = new GitRepositoryFeaturedModUpdater();
    instance.preferencesService = preferencesService;
    instance.taskService = taskService;
    instance.i18n = i18n;
    instance.gitWrapper = gitWrapper;
    instance.notificationService = notificationService;
    instance.applicationContext = applicationContext;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getGitReposDirectory()).thenReturn(reposDirectory.getRoot().toPath());
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPath()).thenReturn(faDirectory.getRoot().toPath());
    doAnswer(invocation -> invocation.getArgumentAt(0, Object.class)).when(taskService).submitTask(any());
  }

  @Test
  public void testUpdateInBackgroundThrowsException() throws Exception {
    GitFeaturedModUpdateTask featuredModTask = mock(GitFeaturedModUpdateTask.class, withSettings().useConstructor());
    when(applicationContext.getBean(GitFeaturedModUpdateTask.class)).thenReturn(featuredModTask);
    GameBinariesUpdateTask binariesTask = mock(GameBinariesUpdateTask.class, withSettings().useConstructor());
    when(applicationContext.getBean(GameBinariesUpdateTask.class)).thenReturn(binariesTask);

    CompletableFuture<PatchResult> future = new CompletableFuture<>();
    future.completeExceptionally(new Exception("This exception mimicks that something went wrong"));
    when(featuredModTask.getFuture()).thenReturn(future);
    when(binariesTask.getFuture()).thenReturn(CompletableFuture.completedFuture(null));

    expectedException.expect(Exception.class);
    expectedException.expectMessage("This exception mimicks that something went wrong");

    instance.updateMod(featuredMod(KnownFeaturedMod.FAF), null).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);
  }

  private FeaturedModBean featuredMod(KnownFeaturedMod knownFeaturedMod) {
    return FeaturedModBeanBuilder.create().defaultValues().technicalName(knownFeaturedMod.getString()).get();
  }
}
