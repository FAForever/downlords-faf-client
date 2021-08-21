package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardEntryBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class LeaderboardsControllerTest extends UITest {

  private LeaderboardsController instance;

  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private I18n i18n;

  private final LeaderboardBean leaderboardGlobal = LeaderboardBeanBuilder.create().defaultValues().id(1).technicalName("global").get();
  private final List<LeaderboardEntryBean> entriesGlobal = List.of(
      LeaderboardEntryBeanBuilder.create().defaultValues().id(0).player(PlayerBeanBuilder.create().defaultValues().username("MarcSpector").id(0).get()).get(),
      LeaderboardEntryBeanBuilder.create().defaultValues().id(1).player(PlayerBeanBuilder.create().defaultValues().username("Sheikah").id(1).get()).get(),
      LeaderboardEntryBeanBuilder.create().defaultValues().id(2).player(PlayerBeanBuilder.create().defaultValues().username("ZLO").id(2).get()).get()
  );

  @BeforeEach
  public void setUp() throws Exception {
    when(leaderboardService.getLeaderboards())
        .thenReturn(CompletableFuture.completedFuture(List.of(leaderboardGlobal)));
    when(leaderboardService.getEntries(leaderboardGlobal)).thenReturn(CompletableFuture.completedFuture(entriesGlobal));

    instance = new LeaderboardsController(leaderboardService, notificationService, i18n);

    loadFxml("theme/leaderboard/leaderboards.fxml", clazz -> instance);
  }

  @Test
  public void testOnDisplay() {
    showLeaderboard(leaderboardGlobal);
    assertTrue(instance.contentPane.isVisible());
    assertEquals(3, instance.ratingTable.getItems().size());
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testOnDisplayWhenThrowException() {
    Exception exception = new RuntimeException("error of loading leaderboard entries");
    when(leaderboardService.getEntries(leaderboardGlobal))
        .thenReturn(CompletableFuture.failedFuture(exception));
    showLeaderboard(leaderboardGlobal);
    assertFalse(instance.contentPane.isVisible());
    verify(notificationService).addImmediateErrorNotification(any(), any());
  }

  @Test
  public void testFilterByNamePlayerExactMatch() {
    showLeaderboard(leaderboardGlobal);
    assertNull(instance.ratingTable.getSelectionModel().getSelectedItem());
    runOnFxThreadAndWait(() -> setSearchText("Sheikah"));
    assertEquals(3, instance.ratingTable.getItems().size());
    assertEquals("Sheikah", instance.ratingTable.getSelectionModel().getSelectedItem().getPlayer().getUsername());
  }

  @Test
  public void testFilterByNamePlayerPartialMatch() {
    showLeaderboard(leaderboardGlobal);
    assertNull(instance.ratingTable.getSelectionModel().getSelectedItem());
    runOnFxThreadAndWait(() -> setSearchText("z"));
    assertEquals(3, instance.ratingTable.getItems().size());
    assertEquals("ZLO", instance.ratingTable.getSelectionModel().getSelectedItem().getPlayer().getUsername());
  }

  @Test
  public void testAutoCompletionSuggestions() {
    showLeaderboard(leaderboardGlobal);
    settingAutoCompletionForTestEnvironment();
    setSearchText("o");
    assertSearchSuggestions("MarcSpector", "ZLO");
    setSearchText("ei");
    assertSearchSuggestions("Sheikah");
  }

  private void showLeaderboard(LeaderboardBean leaderboard) {
    runOnFxThreadAndWait(() -> {
      instance.leaderboardComboBox.setValue(leaderboard);
      instance.onLeaderboardSelected();
    });
  }

  private void setSearchText(String text) {
    runOnFxThreadAndWait(() -> instance.searchTextField.setText(text));
  }

  private void settingAutoCompletionForTestEnvironment() {
    runOnFxThreadAndWait(() -> {
      getRoot().getChildren().add(instance.searchTextField);
      instance.usernamesAutoCompletion.getAutoCompletionPopup().setOpacity(0.0);
    });
  }

  private void assertSearchSuggestions(String... expectedSuggestions) {
    List<String> actualSuggestions = instance.usernamesAutoCompletion.getAutoCompletionPopup().getSuggestions();
    assertEquals(expectedSuggestions.length,  actualSuggestions.size());
    assertTrue(actualSuggestions.containsAll(Arrays.asList(expectedSuggestions)));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
    assertNull(instance.getRoot().getParent());
  }
}
