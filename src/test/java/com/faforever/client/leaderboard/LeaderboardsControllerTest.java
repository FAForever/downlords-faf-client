package com.faforever.client.leaderboard;

import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.test.UITest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.client.leaderboard.LeaderboardEntryBuilder.create;
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

  private final Leaderboard leaderboardGlobal = LeaderboardBuilder.create().defaultValues().id(1).technicalName("global").get();
  private final List<LeaderboardEntry> entriesGlobal = List.of(
      create().defaultValues().username("MarcSpector").get(),
      create().defaultValues().username("Sheikah").get(),
      create().defaultValues().username("ZLO").get()
  );

  private final Leaderboard leaderboard1v1 = LeaderboardBuilder.create().defaultValues().id(2).technicalName("1v1").get();
  private final List<LeaderboardEntry> entries1v1 = List.of(
      create().defaultValues().username("Lenkin").get(),
      create().defaultValues().username("Nexus").get(),
      create().defaultValues().username("FtXCommando").get(),
      create().defaultValues().username("Tex").get()
  );

  @BeforeEach
  public void setUp() throws Exception {
    when(leaderboardService.getLeaderboards())
        .thenReturn(CompletableFuture.completedFuture(List.of(leaderboardGlobal, leaderboard1v1)));
    when(leaderboardService.getEntries(leaderboardGlobal)).thenReturn(CompletableFuture.completedFuture(entriesGlobal));
    when(leaderboardService.getEntries(leaderboard1v1)).thenReturn(CompletableFuture.completedFuture(entries1v1));

    instance = new LeaderboardsController(leaderboardService, notificationService, i18n);

    loadFxml("theme/leaderboard/leaderboards.fxml", clazz -> instance);
  }

  @Test
  public void testOnDisplay() {
    showLeaderboard(leaderboardGlobal);
    assertTrue(instance.contentPane.isVisible());
    assertEquals(3, instance.ratingTable.getItems().size());
    verifyNoInteractions(notificationService);

    showLeaderboard(leaderboard1v1);
    assertTrue(instance.contentPane.isVisible());
    assertEquals(4, instance.ratingTable.getItems().size());
    verifyNoInteractions(notificationService);
  }

  @Test
  public void testOnDisplayWhenThrowException() {
    Exception exception = new RuntimeException("error of loading leaderboard entries");
    when(leaderboardService.getEntries(leaderboard1v1))
        .thenReturn(CompletableFuture.failedFuture(exception));
    showLeaderboard(leaderboard1v1);
    assertFalse(instance.contentPane.isVisible());
    verify(notificationService).addImmediateErrorNotification(any(), any());
  }

  @Test
  public void testFilterByNamePlayerExactMatch() {
    showLeaderboard(leaderboardGlobal);
    assertNull(instance.ratingTable.getSelectionModel().getSelectedItem());
    setSearchText("Sheikah");
    assertEquals(3, instance.ratingTable.getItems().size());
    assertEquals("Sheikah", instance.ratingTable.getSelectionModel().getSelectedItem().getUsername());
  }

  @Disabled("Randomly failing for some reason")
  @Test
  public void testFilterByNamePlayerPartialMatch() {
    showLeaderboard(leaderboardGlobal);
    assertNull(instance.ratingTable.getSelectionModel().getSelectedItem());
    setSearchText("z");
    assertEquals(3, instance.ratingTable.getItems().size());
    assertEquals("ZLO", instance.ratingTable.getSelectionModel().getSelectedItem().getUsername());
  }

  @Test
  public void testAutoCompletionSuggestionsForGlobal() {
    showLeaderboard(leaderboardGlobal);
    settingAutoCompletionForTestEnvironment();
    setSearchText("o");
    assertSearchSuggestions("MarcSpector", "ZLO");
    setSearchText("ei");
    assertSearchSuggestions("Sheikah");
  }

  @Test
  public void testAutoCompletionSuggestionsFor1v1() {
    showLeaderboard(leaderboard1v1);
    settingAutoCompletionForTestEnvironment();
    setSearchText("ex");
    assertSearchSuggestions("Nexus", "Tex");
  }

  private void showLeaderboard(Leaderboard leaderboard) {
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
