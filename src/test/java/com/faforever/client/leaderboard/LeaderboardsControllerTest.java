package com.faforever.client.leaderboard;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LeaderboardsControllerTest extends AbstractPlainJavaFxTest {

  private LeaderboardsController instance;

  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private I18n i18n;

  private Leaderboard leaderboard;

  @Before
  public void setUp() throws Exception {
    leaderboard = LeaderboardBuilder.create().defaultValues().get();

    when(leaderboardService.getLeaderboards()).thenReturn(CompletableFuture.completedFuture(List.of(leaderboard)));

    instance = new LeaderboardsController(leaderboardService, notificationService, i18n, reportingService);

    loadFxml("theme/leaderboard/leaderboards.fxml", clazz -> instance);
  }

  @Test
  public void testOnDisplay() {
    when(leaderboardService.getEntries(leaderboard)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        new LeaderboardEntry(), new LeaderboardEntry()
    )));

    instance.leaderboardComboBox.setValue(leaderboard);
    instance.onLeaderboardSelected();
    WaitForAsyncUtils.waitForFxEvents();

    assertEquals(2, instance.ratingTable.getItems().size());
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testFilterByNamePlayerExactMatch() {
    LeaderboardEntry entry1 = LeaderboardEntryBuilder.create().defaultValues().username("Aa").get();
    LeaderboardEntry entry2 = LeaderboardEntryBuilder.create().defaultValues().username("Ab").get();

    when(leaderboardService.getEntries(leaderboard)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        entry1, entry2
    )));

    instance.leaderboardComboBox.setValue(leaderboard);
    instance.onLeaderboardSelected();
    WaitForAsyncUtils.waitForFxEvents();

    assertNull(instance.ratingTable.getSelectionModel().getSelectedItem());

    instance.searchTextField.setText("aa");
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(2, instance.ratingTable.getItems().size());
    assertEquals("Aa", instance.ratingTable.getSelectionModel().getSelectedItem().getUsername());
  }

  @Test
  public void testFilterByNamePlayerPartialMatch() {
    LeaderboardEntry entry1 = new LeaderboardEntry();
    entry1.setUsername("Aa");
    LeaderboardEntry entry2 = new LeaderboardEntry();
    entry2.setUsername("Ab");

    when(leaderboardService.getEntries(leaderboard)).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
        entry1, entry2
    )));

    instance.leaderboardComboBox.setValue(leaderboard);
    instance.onLeaderboardSelected();
    WaitForAsyncUtils.waitForFxEvents();

    assertNull(instance.ratingTable.getSelectionModel().getSelectedItem());

    instance.searchTextField.setText("b");
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(2, instance.ratingTable.getItems().size());
    assertEquals("Ab", instance.ratingTable.getSelectionModel().getSelectedItem().getUsername());
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
    assertNull(instance.getRoot().getParent());
  }
}
