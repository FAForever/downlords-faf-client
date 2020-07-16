package com.faforever.client.leaderboard;

import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.main.event.OpenLadder1v1LeaderboardEvent;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.remote.FafServerAccessor;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LeaderboardControllerTest extends AbstractPlainJavaFxTest {

  private LeaderboardController instance;

  private LeaderboardService leaderboardService;

  @Mock
  private FafService fafService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private I18n i18n;

  public LeaderboardControllerTest() {
  }

  @Before
  public void setUp() throws Exception {
    leaderboardService = new LeaderboardService(fafService);
    instance = new LeaderboardController(leaderboardService, notificationService, i18n, reportingService);

    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.leaderboardRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
