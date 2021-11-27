package com.faforever.client.leaderboard;

import com.faforever.client.builders.DivisionBeanBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.LeagueSeasonBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.LeagueSeasonBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.remote.AssetService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
  private LeagueSeasonBean season;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBeanBuilder.create().defaultValues().id(3).username("junit").get();
    PlayerBean marcSpector = PlayerBeanBuilder.create().defaultValues().id(0).username("MarcSpector").get();
    PlayerBean sheikah = PlayerBeanBuilder.create().defaultValues().id(1).username("Sheikah").get();
    PlayerBean zlo = PlayerBeanBuilder.create().defaultValues().id(2).username("ZLO").get();
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.getOrDefault(anyString(), anyString())).thenReturn("seasonName");
    when(i18n.get("leagues.divisionName.test_name")).thenReturn("Bronze");
    when(i18n.get("leagues.divisionName.silver")).thenReturn("Silver");

    season = LeagueSeasonBeanBuilder.create().defaultValues().get();
    subdivisionBean1 = SubdivisionBeanBuilder.create().defaultValues().id(1).index(1).get();
    DivisionBean division = DivisionBeanBuilder.create().defaultValues().nameKey("silver").index(2).get();
    subdivisionBean2 = SubdivisionBeanBuilder.create().defaultValues().id(2).index(1).division(division).get();
    leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).id(0).player(marcSpector).get();
    leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).id(1).player(sheikah).get();
    LeagueEntryBean leagueEntryBean3 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean2).id(2).player(zlo).get();

    when(leaderboardService.getAllSubdivisions(season)).thenReturn(
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
    when(leaderboardService.getPlayerNumberInHigherDivisions(subdivisionBean1)).thenReturn(CompletableFuture.completedFuture(1));
    when(leaderboardService.getPlayerNumberInHigherDivisions(subdivisionBean2)).thenReturn(CompletableFuture.completedFuture(0));
    when(leaderboardService.getTotalPlayers(season)).thenReturn(CompletableFuture.completedFuture(3));
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(
        CompletableFuture.completedFuture(null));
    when(leaderboardService.getLeagueEntryForPlayer(sheikah, season)).thenReturn(
        CompletableFuture.completedFuture(leagueEntryBean2));

    subDivisionTabController = new SubDivisionTabController(leaderboardService, notificationService, i18n, uiService);
    loadFxml("theme/leaderboard/subDivisionTab.fxml", clazz -> subDivisionTabController);
    subDivisionTabController.initialize();
    when(uiService.loadFxml("theme/leaderboard/subDivisionTab.fxml")).thenReturn(subDivisionTabController);

    instance = new LeaderboardController(assetService, i18n, leaderboardService, notificationService, playerService, uiService);
    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> instance);
    instance.setSeason(season);
    // In a test environment this doesn't get called automatically anymore, so we have to do it manually
    runOnFxThreadAndWait(() -> instance.onMajorDivisionPicked());
  }

  @Test
  public void testSetSeason() {
    waitForFxEvents();

    assertEquals("SEASONNAME", instance.seasonLabel.getText());
    assertEquals(2, instance.majorDivisionPicker.getItems().size());
    assertEquals(subdivisionBean1, instance.majorDivisionPicker.getItems().get(0));
    assertTrue(instance.majorDivisionPicker.getSelectionModel().isSelected(1));
    assertTrue(instance.contentPane.isVisible());
    verifyNoInteractions(notificationService);
    assertNull(subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
  }

  @Test
  public void testInitializeWithSeasonError() {
    when(leaderboardService.getAllSubdivisions(season)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.setSeason(season);

    verify(notificationService, times(2)).addImmediateErrorNotification(any(CompletionException.class), eq("leaderboard.failedToLoadDivisions"));
  }

  @Test
  public void testSelectSearchedPlayer() {
    showDivision(subdivisionBean2);
    assertNull(subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem());
    setSearchText("Sheikah");
    instance.processSearchInput();
    runOnFxThreadAndWait(() -> instance.onMajorDivisionPicked());

    assertEquals(subdivisionBean1, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(2, subDivisionTabController.ratingTable.getItems().size());
    assertEquals("Sheikah", subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem().getPlayer().getUsername());
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
  public void testNoLeagueEntry() {
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
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(
        CompletableFuture.completedFuture(leagueEntryBean));
    when(i18n.get("leaderboard.placement", 100, 10)).thenReturn("in placement");

    instance.updateDisplayedPlayerStats();
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
    when(leaderboardService.getEntries(subdivisionBean1)).thenReturn(
        CompletableFuture.completedFuture(List.of(leagueEntryBean1, playerEntryBean, leagueEntryBean2)));
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(
        CompletableFuture.completedFuture(playerEntryBean));
    when(i18n.number(8)).thenReturn("8");
    when(i18n.get("leaderboard.divisionName", "Bronze", "I")).thenReturn("Bronze I");

    instance.updateDisplayedPlayerStats();
    runOnFxThreadAndWait(() -> instance.onMajorDivisionPicked());

    assertTrue(instance.playerDivisionNameLabel.isVisible());
    assertEquals("BRONZE I", instance.playerDivisionNameLabel.getText());
    assertFalse(instance.placementLabel.isVisible());
    assertEquals(-360.0 * 0.8, instance.scoreArc.getLength());
    assertEquals("8", instance.playerScoreLabel.getText());
    assertEquals(subdivisionBean1, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
    assertEquals(3, subDivisionTabController.ratingTable.getItems().size());
    assertEquals("junit", subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem().getPlayer().getUsername());
  }

  @Test
  public void testUpdateDisplayedPlayerStatsWithDivisionError() {
    when(leaderboardService.getAllSubdivisions(season)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.updateDisplayedPlayerStats();

    verify(notificationService).addImmediateErrorNotification(any(CompletionException.class), eq("leaderboard.failedToLoadDivisions"));
  }

  @Test
  public void testUpdateDisplayedPlayerStatsWithLeagueEntryError() {
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(CompletableFuture.failedFuture(new FakeTestException()));

    instance.updateDisplayedPlayerStats();

    verify(notificationService).addImmediateErrorNotification(any(CompletionException.class), eq("leaderboard.failedToLoadEntry"));
  }
}
