package com.faforever.client.leaderboard;

import com.faforever.client.builders.DivisionBeanBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.LeagueSeasonBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class LeaderboardControllerTest extends UITest {

  private LeaderboardController instance;

  @Mock
  private AssetService assetService;
  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PlayerService playerService;
  @Mock
  private UiService uiService;

  private SubDivisionTabController subDivisionTabController;
  private PlayerBean player;
  private SubdivisionBean subdivisionBean1;
  private SubdivisionBean subdivisionBean2;
  private LeagueEntryBean leagueEntryBean1;
  private LeagueEntryBean leagueEntryBean2;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBeanBuilder.create().defaultValues().id(3).username("junit").get();
    PlayerBean sheikah = PlayerBeanBuilder.create().defaultValues().id(1).username("Sheikah").get();
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.getOrDefault(anyString(), anyString())).thenReturn("seasonName");

    subdivisionBean1 = SubdivisionBeanBuilder.create().defaultValues().index(1).get();
    subdivisionBean2 = SubdivisionBeanBuilder.create().defaultValues().index(1).division(DivisionBeanBuilder.create().defaultValues().index(2).get()).get();
    leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).id(0).username("MarcSpector").get();
    leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).id(1).username("Sheikah").get();
    LeagueEntryBean leagueEntryBean3 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean2).id(2).username("ZLO").get();

    when(leaderboardService.getAllSubdivisions(1)).thenReturn(
        CompletableFuture.completedFuture(List.of(subdivisionBean1, subdivisionBean2)));
    when(leaderboardService.getEntries(subdivisionBean1)).thenReturn(
        CompletableFuture.completedFuture(List.of(leagueEntryBean1, leagueEntryBean2)));
    when(leaderboardService.getEntries(subdivisionBean2)).thenReturn(
        CompletableFuture.completedFuture(List.of(leagueEntryBean3)));
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(playerService.getPlayerByName("Sheikah")).thenReturn(CompletableFuture.completedFuture(
        Optional.of(sheikah)));
    when(leaderboardService.getSizeOfDivision(subdivisionBean1)).thenReturn(CompletableFuture.completedFuture(2));
    when(leaderboardService.getSizeOfDivision(subdivisionBean2)).thenReturn(CompletableFuture.completedFuture(1));
    when(leaderboardService.getTotalPlayers(1)).thenReturn(CompletableFuture.completedFuture(3));
    when(leaderboardService.getLeagueEntryForPlayer(player, 1)).thenReturn(
        CompletableFuture.completedFuture(null));
    when(leaderboardService.getLeagueEntryForPlayer(sheikah, 1)).thenReturn(
        CompletableFuture.completedFuture(leagueEntryBean2));

    subDivisionTabController = new SubDivisionTabController(leaderboardService, notificationService, i18n);
    loadFxml("theme/leaderboard/subDivisionTab.fxml", clazz -> subDivisionTabController);
    subDivisionTabController.initialize();
    when(uiService.loadFxml("theme/leaderboard/subDivisionTab.fxml")).thenReturn(subDivisionTabController);

    instance = new LeaderboardController(assetService, i18n, leaderboardService, notificationService, playerService, uiService);
    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> instance);
    instance.initialize();
    instance.setSeason(LeagueSeasonBeanBuilder.create().defaultValues().id(1).get());
  }

  @Test
  public void testSetSeason() {
    waitForFxEvents();

    assertEquals("SEASONNAME", instance.seasonLabel.getText());
    assertEquals(2, instance.majorDivisionPicker.getItems().size());
    assertEquals(subdivisionBean1, instance.majorDivisionPicker.getItems().get(0));
    assertTrue(instance.contentPane.isVisible());
    verifyNoInteractions(notificationService);
    assertNull(subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
  }

  @Test
  public void testFilterByNamePlayerExactMatch() {
    showDivision(subdivisionBean2);
    assertNull(subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem());
    runOnFxThreadAndWait(() -> setSearchText("Sheikah"));

    assertEquals(subdivisionBean1, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(2, subDivisionTabController.ratingTable.getItems().size());
    assertEquals("Sheikah", subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem().getUsername());
  }

  @Test
  public void testFilterByNamePlayerPartialMatch() {
    showDivision(subdivisionBean2);
    assertNull(subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem());
    runOnFxThreadAndWait(() -> setSearchText("z"));
    assertEquals(subdivisionBean2, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, subDivisionTabController.ratingTable.getItems().size());
    assertEquals("ZLO", subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem().getUsername());
  }

  @Test
  public void testAutoCompletionSuggestions() {
    settingAutoCompletionForTestEnvironment();
    setSearchText("o");
    assertSearchSuggestions("MarcSpector", "ZLO");
    setSearchText("ei");
    assertSearchSuggestions("Sheikah");
  }

  private void showDivision(SubdivisionBean subdivision) {
    runOnFxThreadAndWait(() -> {
      instance.majorDivisionPicker.getSelectionModel().select(subdivision);
      instance.onMajorDivisionPicked();
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
    runOnFxThreadAndWait(() -> {
      List<String> actualSuggestions = instance.usernamesAutoCompletion.getAutoCompletionPopup().getSuggestions();
      assertEquals(expectedSuggestions.length,  actualSuggestions.size());
      assertTrue(actualSuggestions.containsAll(Arrays.asList(expectedSuggestions)));
  });
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
  }

  @Test
  public void noLeagueEntry() {
    when(i18n.get("leaderboard.noEntry")).thenReturn("Play matchmaker games to get assigned to a division");
    waitForFxEvents();

    assertFalse(instance.playerDivisionNameLabel.isVisible());
    assertTrue(instance.placementLabel.isVisible());
    assertEquals("Play matchmaker games to get assigned to a division", instance.placementLabel.getText());
    assertEquals(subdivisionBean2, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
    assertEquals(1, subDivisionTabController.ratingTable.getItems().size());
  }

  @Test
  public void testNotPlaced() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().score(8).subdivision(null).get();
    when(leaderboardService.getLeagueEntryForPlayer(player, 1)).thenReturn(
        CompletableFuture.completedFuture(leagueEntryBean));
    when(i18n.get("leaderboard.placement", 100)).thenReturn("in placement");

    instance.updateDisplayedPlayerStats(player);
    waitForFxEvents();

    assertFalse(instance.playerDivisionNameLabel.isVisible());
    assertTrue(instance.placementLabel.isVisible());
    assertEquals("in placement", instance.placementLabel.getText());
    assertEquals(subdivisionBean2, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
    assertEquals(1, subDivisionTabController.ratingTable.getItems().size());
  }

  @Test
  public void testWithLeagueEntry() {
    LeagueEntryBean playerEntryBean = LeagueEntryBeanBuilder.create().defaultValues().score(8).subdivision(subdivisionBean1).get();
    when(leaderboardService.getLeagueEntryForPlayer(player, 1)).thenReturn(
        CompletableFuture.completedFuture(playerEntryBean));
    when(leaderboardService.getEntries(subdivisionBean1)).thenReturn(
        CompletableFuture.completedFuture(List.of(leagueEntryBean1, playerEntryBean, leagueEntryBean2)));
    when(i18n.number(8)).thenReturn("8");

    instance.updateDisplayedPlayerStats(player);
    waitForFxEvents();

    assertTrue(instance.playerDivisionNameLabel.isVisible());
    assertFalse(instance.placementLabel.isVisible());
    assertEquals(-360.0 * 0.8, instance.scoreArc.getLength());
    assertEquals("8", instance.playerScoreLabel.getText());
    assertEquals(subdivisionBean1, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
    assertEquals(3, subDivisionTabController.ratingTable.getItems().size());
    assertEquals("junit", subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem().getUsername());
  }
}
