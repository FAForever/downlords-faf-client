package com.faforever.client.leaderboard;

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
import javafx.scene.control.Tab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
  @Mock
  private SubDivisionTabController subDivisionTabController;

  private PlayerBean player;
  private SubdivisionBean subdivisionBean1;

  @BeforeEach
  public void setUp() throws Exception {
    player = PlayerBeanBuilder.create().defaultValues().id(1).username("junit").get();
    when(i18n.get(anyString())).thenReturn("");
    when(i18n.getOrDefault(anyString(), anyString())).thenReturn("seasonName");

    subdivisionBean1 = SubdivisionBeanBuilder.create().defaultValues().index(1).get();
    SubdivisionBean subdivisionBean2 = SubdivisionBeanBuilder.create().defaultValues().index(3).get();
    LeagueEntryBean leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).get();
    LeagueEntryBean leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean2).get();
    when(leaderboardService.getAllSubdivisions(1)).thenReturn(
        CompletableFuture.completedFuture(List.of(subdivisionBean1, subdivisionBean2)));
    when(leaderboardService.getEntries(subdivisionBean1)).thenReturn(
        CompletableFuture.completedFuture(List.of(leagueEntryBean1)));
    when(leaderboardService.getEntries(subdivisionBean2)).thenReturn(
        CompletableFuture.completedFuture(List.of(leagueEntryBean2)));
    when(playerService.getCurrentPlayer()).thenReturn(player);
    when(leaderboardService.getLeagueEntryForPlayer(player, 1)).thenReturn(
        CompletableFuture.completedFuture(leagueEntryBean1));
    when(leaderboardService.getSizeOfDivision(subdivisionBean1)).thenReturn(CompletableFuture.completedFuture(1));
    when(leaderboardService.getSizeOfDivision(subdivisionBean2)).thenReturn(CompletableFuture.completedFuture(1));
    when(leaderboardService.getTotalPlayers(1)).thenReturn(CompletableFuture.completedFuture(2));
    when(leaderboardService.getLeagueEntryForPlayer(player, 1)).thenReturn(
        CompletableFuture.completedFuture(null));
    when(subDivisionTabController.getRoot()).thenReturn(new Tab());
    when(uiService.loadFxml("theme/leaderboard/subDivisionTab.fxml")).thenReturn(subDivisionTabController);

    instance = new LeaderboardController(assetService, i18n, leaderboardService, notificationService, playerService, uiService);
    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> instance);

    instance.setSeason(LeagueSeasonBeanBuilder.create().defaultValues().id(1).get());
  }

  @Test
  public void testSetSeason() {

    assertEquals(instance.seasonLabel.getText(), "SEASONNAME");
    assertEquals(instance.majorDivisionPicker.getItems().size(), 1);
    assertEquals(instance.majorDivisionPicker.getItems().get(0), subdivisionBean1);
    assertTrue(instance.contentPane.isVisible());
  }

  @Test
  public void testFilterByNamePlayerExactMatch() {

  }

  @Test
  public void testFilterByNamePlayerPartialMatch() {

  }

  @Test
  public void testAutoCompletionSuggestions() {
    settingAutoCompletionForTestEnvironment();
    setSearchText("o");
    assertSearchSuggestions("MarcSpector", "ZLO");
    setSearchText("ei");
    assertSearchSuggestions("Sheikah");
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
  public void testMajorDivisionPicker() {

  }

  @Test
  public void noLeagueEntry() {
    when(i18n.get("leaderboard.noEntry")).thenReturn("Play matchmaker games to get assigned to a division");

    assertFalse(instance.playerDivisionNameLabel.isVisible());
    assertTrue(instance.placementLabel.isVisible());
    assertEquals(instance.placementLabel.getText(), "Play matchmaker games to get assigned to a division");
  }

  @Test
  public void testNotPlaced() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().score(8).subdivision(null).get();
    when(leaderboardService.getLeagueEntryForPlayer(player, 1)).thenReturn(
        CompletableFuture.completedFuture(leagueEntryBean));
    when(i18n.get("leaderboard.placement", 100)).thenReturn("in placement");

    instance.updateDisplayedPlayerStats(player);

    assertFalse(instance.playerDivisionNameLabel.isVisible());
    assertTrue(instance.placementLabel.isVisible());
    assertEquals(instance.placementLabel.getText(), "in placement");
  }

  @Test
  public void testWithLeagueEntry() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().score(8).subdivision(subdivisionBean).get();
    when(leaderboardService.getLeagueEntryForPlayer(player, 1)).thenReturn(
        CompletableFuture.completedFuture(leagueEntryBean));
    when(i18n.number(8)).thenReturn("8");

    instance.updateDisplayedPlayerStats(player);

    assertTrue(instance.playerDivisionNameLabel.isVisible());
    assertFalse(instance.placementLabel.isVisible());
    assertEquals(instance.scoreArc.getLength(), -360.0 * 0.8);
    assertEquals(instance.playerScoreLabel.getText(), "8");
  }

}
