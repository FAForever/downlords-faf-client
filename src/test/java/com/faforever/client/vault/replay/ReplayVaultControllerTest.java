package com.faforever.client.vault.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.LoadLocalReplaysTask;
import com.faforever.client.replay.ReplayInfoBeanBuilder;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.beans.InvalidationListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReplayVaultControllerTest extends AbstractPlainJavaFxTest {

  private ReplayVaultController instance;
  @Mock
  private I18n i18n;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private TaskService taskService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReplayService replayService;

  @Before
  public void setUp() throws Exception {
    instance = new ReplayVaultController();
    instance.i18n = i18n;
    instance.applicationContext = applicationContext;
    instance.taskService = taskService;
    instance.notificationService = notificationService;
    instance.replayService = replayService;

    doAnswer(invocation -> invocation.getArgumentAt(0, Object.class)).when(taskService).submitTask(any());

    loadFxml("theme/vault/replay/replay_vault.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.replayVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testLoadLocalReplaysInBackground() throws Exception {
    LoadLocalReplaysTask task = mock(LoadLocalReplaysTask.class);
    when(task.getFuture()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        ReplayInfoBeanBuilder.create().get(),
        ReplayInfoBeanBuilder.create().get(),
        ReplayInfoBeanBuilder.create().get()
    )));

    when(applicationContext.getBean(LoadLocalReplaysTask.class)).thenReturn(task);

    CountDownLatch loadedLatch = new CountDownLatch(1);
    instance.localReplaysRoot.getChildren().addListener((InvalidationListener) observable -> loadedLatch.countDown());

    instance.loadLocalReplaysInBackground();

    assertTrue(loadedLatch.await(5000, TimeUnit.MILLISECONDS));
    assertThat(instance.localReplaysRoot.getChildren(), hasSize(3));

    verify(taskService).submitTask(task);
    verifyZeroInteractions(notificationService);
  }
//
//  @Test
//  public void testLoadOnlineReplaysInBackground() throws Exception {
//    LoadLocalReplaysTask task = mock(LoadLocalReplaysTask.class);
//    when(applicationContext.getBean(LoadLocalReplaysTask.class)).thenReturn(task);
//    when(replayService.getOnlineReplays()).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
//        ReplayInfoBeanBuilder.create().get(),
//        ReplayInfoBeanBuilder.create().get(),
//        ReplayInfoBeanBuilder.create().get()
//    )));
//
//    CountDownLatch loadedLatch = new CountDownLatch(1);
//    instance.onlineReplaysRoot.getChildren().addListener((InvalidationListener) observable -> loadedLatch.countDown());
//
//    instance.loadOnlineReplaysInBackground();
//
//    assertTrue(loadedLatch.await(5000, TimeUnit.MILLISECONDS));
//    assertThat(instance.onlineReplaysRoot.getChildren(), hasSize(3));
//
//    verifyZeroInteractions(notificationService);
//  }
}
