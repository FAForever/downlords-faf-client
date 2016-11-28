package com.faforever.client.leaderboard;

import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LeaderboardControllerTest extends AbstractPlainJavaFxTest {

  private LeaderboardController instance;

  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;

  @Before
  public void setUp() throws Exception {
    instance = new LeaderboardController();
    instance.leaderboardService = leaderboardService;
    instance.notificationService = notificationService;
    instance.reportingService = reportingService;

    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> instance);
  }

  @Test
  public void testOnDisplay() throws Exception {
    when(leaderboardService.getEntries(KnownFeaturedMod.LADDER_1V1)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        new Ranked1v1EntryBean(), new Ranked1v1EntryBean()
    )));

    CountDownLatch loadedLatch = new CountDownLatch(1);
    instance.ratingTable.itemsProperty().addListener(observable -> loadedLatch.countDown());
    instance.setRatingType(KnownFeaturedMod.LADDER_1V1);

    instance.onDisplay();

    assertTrue(loadedLatch.await(3, TimeUnit.SECONDS));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testFilterByNamePlayerExactMatch() throws Exception {
    Ranked1v1EntryBean entry1 = new Ranked1v1EntryBean();
    entry1.setUsername("Aa");
    Ranked1v1EntryBean entry2 = new Ranked1v1EntryBean();
    entry2.setUsername("Ab");

    when(leaderboardService.getEntries(KnownFeaturedMod.LADDER_1V1)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        entry1, entry2
    )));
    instance.setRatingType(KnownFeaturedMod.LADDER_1V1);
    instance.onDisplay();

    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem(), nullValue());

    instance.searchTextField.setText("aa");
    assertThat(instance.ratingTable.getItems(), hasSize(2));
    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem().getUsername(), is("Aa"));
  }

  @Test
  public void testFilterByNamePlayerPartialMatch() throws Exception {
    Ranked1v1EntryBean entry1 = new Ranked1v1EntryBean();
    entry1.setUsername("Aa");
    Ranked1v1EntryBean entry2 = new Ranked1v1EntryBean();
    entry2.setUsername("Ab");

    when(leaderboardService.getEntries(KnownFeaturedMod.LADDER_1V1)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        entry1, entry2
    )));
    instance.setRatingType(KnownFeaturedMod.LADDER_1V1);
    instance.onDisplay();

    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem(), nullValue());

    instance.searchTextField.setText("b");
    assertThat(instance.ratingTable.getItems(), hasSize(2));
    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem().getUsername(), is("Ab"));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), is(instance.leaderboardRoot));
    assertThat(instance.getRoot().getParent(), is(nullValue()));
  }
}
