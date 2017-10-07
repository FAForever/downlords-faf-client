package com.faforever.client.patch;

import com.faforever.client.game.FeaturedModBeanBuilder;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.mod.FeaturedMod;
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
  private ApplicationContext applicationContext;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private TaskService taskService;

  private GitRepositoryFeaturedModUpdater instance;

  @Before
  public void setUp() throws Exception {
    instance = new GitRepositoryFeaturedModUpdater(taskService, applicationContext, preferencesService);

    when(preferencesService.getPatchReposDirectory()).thenReturn(reposDirectory.getRoot().toPath());
    doAnswer(invocation -> invocation.getArgument(0)).when(taskService).submitTask(any());
  }

  @Test
  public void testUpdateInBackgroundThrowsException() throws Exception {
    GitFeaturedModUpdateTask featuredModTask = mock(GitFeaturedModUpdateTask.class, withSettings());
    when(applicationContext.getBean(GitFeaturedModUpdateTask.class)).thenReturn(featuredModTask);
    GameBinariesUpdateTask binariesTask = mock(GameBinariesUpdateTask.class, withSettings());

    CompletableFuture<PatchResult> future = new CompletableFuture<>();
    future.completeExceptionally(new Exception("This exception mimicks that something went wrong"));
    when(featuredModTask.getFuture()).thenReturn(future);

    expectedException.expect(Exception.class);
    expectedException.expectMessage("This exception mimicks that something went wrong");

    instance.updateMod(featuredMod(KnownFeaturedMod.FAF), null).toCompletableFuture().get(TIMEOUT, TIMEOUT_UNIT);
  }

  private FeaturedMod featuredMod(KnownFeaturedMod knownFeaturedMod) {
    return FeaturedModBeanBuilder.create().defaultValues().technicalName(knownFeaturedMod.getTechnicalName()).get();
  }
}
