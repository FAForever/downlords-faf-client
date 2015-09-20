package com.faforever.client.patch;

import com.faforever.client.game.GameType;
import com.faforever.client.preferences.ForgedAlliancePrefs;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.PrioritizedTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.Callback;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.context.ApplicationContext;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class GameUpdateServiceImplTest extends AbstractPlainJavaFxTest {

  private static final long TIMEOUT = 5000;
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  @Rule
  public final TemporaryFolder fafBinDirectory = new TemporaryFolder();

  @Rule
  public final TemporaryFolder faDirectory = new TemporaryFolder();

  private GameUpdateServiceImpl instance;

  @Mock
  private TaskService taskService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private Preferences preferences;
  @Mock
  private ForgedAlliancePrefs forgedAlliancePrefs;

  @Before
  public void setUp() throws Exception {
    instance = new GameUpdateServiceImpl();
    instance.taskService = taskService;
    instance.applicationContext = applicationContext;
    instance.preferencesService = preferencesService;

    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(preferencesService.getFafBinDirectory()).thenReturn(fafBinDirectory.getRoot().toPath());
    when(preferences.getForgedAlliance()).thenReturn(forgedAlliancePrefs);
    when(forgedAlliancePrefs.getPath()).thenReturn(faDirectory.getRoot().toPath());

    mockTaskService();
  }

  @SuppressWarnings("unchecked")
  private void mockTaskService() throws Exception {
    doAnswer((InvocationOnMock invocation) -> {
      PrioritizedTask<Boolean> prioritizedTask = invocation.getArgumentAt(0, PrioritizedTask.class);
      prioritizedTask.run();

      Callback<Boolean> callback = invocation.getArgumentAt(1, Callback.class);

      Future<Throwable> throwableFuture = WaitForAsyncUtils.asyncFx(prioritizedTask::getException);
      Throwable throwable = throwableFuture.get(TIMEOUT, TIMEOUT_UNIT);
      if (throwable != null) {
        callback.error(throwable);
      } else {
        Future<Boolean> result = WaitForAsyncUtils.asyncFx(prioritizedTask::getValue);
        callback.success(result.get(TIMEOUT, TIMEOUT_UNIT));
      }

      return null;
    }).when(instance.taskService).submitTask(any(), any());
  }

  @Test
  public void testPatchInBackground() throws Exception {
    UpdateGameFilesTask updateGameFilesTask = mock(UpdateGameFilesTask.class, withSettings().useConstructor());
    when(applicationContext.getBean(UpdateGameFilesTask.class)).thenReturn(updateGameFilesTask);

    instance.updateInBackground(GameType.DEFAULT.getString(), null, null, null).get(TIMEOUT, TIMEOUT_UNIT);

    verify(taskService).submitTask(eq(updateGameFilesTask), any());
  }

  @Test
  public void testPatchInBackgroundDirectoriesNotSet() throws Exception {
    when(preferencesService.getFafBinDirectory()).thenReturn(null);
    when(forgedAlliancePrefs.getPath()).thenReturn(null);

    instance.updateInBackground(GameType.DEFAULT.getString(), null, null, null).get(TIMEOUT, TIMEOUT_UNIT);

    verifyZeroInteractions(taskService);
  }

  @Test
  public void testPatchInBackgroundAlreadyRunning() throws Exception {
    UpdateGameFilesTask updateGameFilesTask = mock(UpdateGameFilesTask.class, withSettings().useConstructor());
    when(applicationContext.getBean(UpdateGameFilesTask.class)).thenReturn(updateGameFilesTask);

    when(updateGameFilesTask.isDone()).thenReturn(false);

    instance.updateInBackground(GameType.DEFAULT.getString(), null, null, null).get(TIMEOUT, TIMEOUT_UNIT);
    instance.updateInBackground(GameType.DEFAULT.getString(), null, null, null).get(TIMEOUT, TIMEOUT_UNIT);

    verify(taskService, only()).submitTask(eq(updateGameFilesTask), any());
  }

  @Test
  public void testCheckForUpdateInBackground() throws Exception {
    instance.checkForUpdateInBackground();
  }
}
