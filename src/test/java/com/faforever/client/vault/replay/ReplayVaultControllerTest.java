package com.faforever.client.vault.replay;

import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.LocalReplaysChangedEvent;
import com.faforever.client.map.MapService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.replay.Replay;
import com.faforever.client.replay.ReplayInfoBeanBuilder;
import com.faforever.client.replay.ReplayService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.beans.InvalidationListener;
import javafx.scene.control.TableView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;

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
  @Mock
  private MapService mapService;
  @Mock
  private TimeService timeService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private UiService uiService;
  @Mock
  private ExecutorService executorService;

  @Before
  public void setUp() throws Exception {
    instance = new ReplayVaultController(notificationService, replayService, mapService, taskService, i18n, timeService,
        reportingService, applicationContext, uiService);

    loadFxml("theme/vault/replay/replay_vault.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.replayVaultRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }

  @Test
  public void testLoadLocalReplaysInBackground() throws Exception {

    List<Replay> replays = Arrays.asList(
        ReplayInfoBeanBuilder.create().get(),
        ReplayInfoBeanBuilder.create().get(),
        ReplayInfoBeanBuilder.create().get()
    );

    CountDownLatch loadedLatch = new CountDownLatch(1);
    instance.replayTableView.getItems().addListener((InvalidationListener) observable -> loadedLatch.countDown());

    instance.loadLocalReplaysInBackground();
    instance.onLocalReplaysChanged(new LocalReplaysChangedEvent(this, replays, new ArrayList<Replay>()));

    assertTrue(loadedLatch.await(5000, TimeUnit.MILLISECONDS));
    assertThat(instance.replayTableView.getItems().size(), is(3));

    verifyZeroInteractions(notificationService);
  }
}
