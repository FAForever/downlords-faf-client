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
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.testfx.util.WaitForAsyncUtils.waitForFxEvents;

public class LeaderboardControllerTest extends PlatformTest {

  @InjectMocks
  private LeaderboardController instance;

  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;
  @Mock
  private NotificationService notificationService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TimeService timeService;
  @Mock
  private UiService uiService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

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
    lenient().when(i18n.get(anyString())).thenReturn("");
    lenient().when(i18n.getOrDefault("seasonName", "leaderboard.season.seasonName", 1)).thenReturn("seasonName 1");
    lenient().when(i18n.get("leagues.divisionName.test_name")).thenReturn("Bronze");
    lenient().when(i18n.get("leagues.divisionName.silver")).thenReturn("Silver");
    lenient().when(i18n.get("leaderboard.seasonDate", null, null)).thenReturn("-");
    lenient().when(i18n.get("leaderboard.noEntry")).thenReturn("Play matchmaker games to get assigned to a division");

    season = LeagueSeasonBeanBuilder.create().defaultValues().get();
    subdivisionBean1 = SubdivisionBeanBuilder.create().defaultValues().id(1).index(1).get();
    DivisionBean division = DivisionBeanBuilder.create().defaultValues().nameKey("silver").index(2).get();
    subdivisionBean2 = SubdivisionBeanBuilder.create().defaultValues().id(2).index(1).division(division).get();
    leagueEntryBean1 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).id(0).player(marcSpector).get();
    leagueEntryBean2 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean1).id(1).player(sheikah).get();
    LeagueEntryBean leagueEntryBean3 = LeagueEntryBeanBuilder.create().defaultValues().subdivision(subdivisionBean2).id(2).player(zlo).get();

    lenient().when(leaderboardService.getAllSubdivisions(season))
             .thenReturn(Flux.just(subdivisionBean1, subdivisionBean2));
    lenient().when(leaderboardService.getEntries(subdivisionBean1))
             .thenReturn(Flux.just(leagueEntryBean1, leagueEntryBean2));
    lenient().when(leaderboardService.getEntries(subdivisionBean2)).thenReturn(Flux.just(leagueEntryBean3));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
    lenient().when(playerService.getPlayerByName("Sheikah")).thenReturn(Mono.just(sheikah));
    lenient().when(leaderboardService.getSizeOfDivision(subdivisionBean1)).thenReturn(Mono.just(2));
    lenient().when(leaderboardService.getSizeOfDivision(subdivisionBean2)).thenReturn(Mono.just(1));
    lenient().when(leaderboardService.getPlayerNumberInHigherDivisions(subdivisionBean1)).thenReturn(Mono.just(1));
    lenient().when(leaderboardService.getPlayerNumberInHigherDivisions(subdivisionBean2)).thenReturn(Mono.just(0));
    lenient().when(leaderboardService.getTotalPlayers(season)).thenReturn(Mono.just(3));
    lenient().when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(Mono.empty());
    lenient().when(leaderboardService.getLeagueEntryForPlayer(sheikah, season)).thenReturn(Mono.just(leagueEntryBean2));
    doAnswer(invocation -> {
      Runnable runnable = invocation.getArgument(0);
      runnable.run();
      return null;
    }).when(fxApplicationThreadExecutor).execute(any());

    subDivisionTabController = new SubDivisionTabController(contextMenuBuilder, leaderboardService, notificationService, i18n, fxApplicationThreadExecutor);
    loadFxml("theme/leaderboard/sub_division_tab.fxml", clazz -> subDivisionTabController);
    subDivisionTabController.initialize();
    lenient().when(uiService.loadFxml("theme/leaderboard/sub_division_tab.fxml")).thenReturn(subDivisionTabController);

    loadFxml("theme/leaderboard/leaderboard.fxml", clazz -> instance);
    instance.setSeason(season);
  }

  @Test
  @Disabled("Need to refactor league tab")
  public void testSetSeason() {
    waitForFxEvents();

    assertEquals("SEASONNAME 1", instance.seasonLabel.getText());
    assertEquals(2, instance.majorDivisionPicker.getItems().size());
    assertEquals(subdivisionBean1, instance.majorDivisionPicker.getItems().get(1));
    assertTrue(instance.majorDivisionPicker.getSelectionModel().isSelected(0));
    assertTrue(instance.contentPane.isVisible());
    verifyNoInteractions(notificationService);
    assertNull(subDivisionTabController.ratingTable.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
  }

  @Test
  public void testInitializeWithSeasonError() {
    when(leaderboardService.getAllSubdivisions(season)).thenReturn(Flux.error(new FakeTestException()));

    instance.setSeason(season);

    verify(notificationService, times(2)).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadDivisions"));
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

  private void showDivision(SubdivisionBean subdivision) {
    runOnFxThreadAndWait(() -> {
      instance.majorDivisionPicker.getSelectionModel().select(subdivision);
      instance.onMajorDivisionPicked();
    });
  }

  private void setSearchText(String text) {
    runOnFxThreadAndWait(() -> instance.searchTextField.setText(text));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertEquals(instance.getRoot(), instance.leaderboardRoot);
  }

  @Test
  @Disabled("Need to refactor league tab")
  public void testNoLeagueEntry() {
    waitForFxEvents();

    assertFalse(instance.playerDivisionNameLabel.isVisible());
    assertTrue(instance.placementLabel.isVisible());
    assertEquals("Play matchmaker games to get assigned to a division", instance.placementLabel.getText());
    assertEquals(subdivisionBean2, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
    assertEquals(1, subDivisionTabController.ratingTable.getItems().size());
  }

  @Test
  @Disabled("Need to refactor league tab")
  public void testNotPlaced() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().score(8).subdivision(null).get();
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(Mono.just(leagueEntryBean));
    when(i18n.get("leaderboard.placement", 100, 10)).thenReturn("in placement (10)");

    instance.updateDisplayedPlayerStats();
    waitForFxEvents();

    assertFalse(instance.playerDivisionNameLabel.isVisible());
    assertTrue(instance.placementLabel.isVisible());
    assertEquals("in placement (10)", instance.placementLabel.getText());
    assertEquals(subdivisionBean2, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
    assertEquals(1, subDivisionTabController.ratingTable.getItems().size());
  }

  @Test
  @Disabled("Need to refactor league tab")
  public void testNotPlacedVeteran() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues()
        .score(8).subdivision(null).returningPlayer(true).get();
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(Mono.just(leagueEntryBean));
    when(i18n.get("leaderboard.placement", 100, 3)).thenReturn("in placement (3)");

    instance.updateDisplayedPlayerStats();
    waitForFxEvents();

    assertFalse(instance.playerDivisionNameLabel.isVisible());
    assertTrue(instance.placementLabel.isVisible());
    assertEquals("in placement (3)", instance.placementLabel.getText());
    assertEquals(subdivisionBean2, instance.majorDivisionPicker.getSelectionModel().getSelectedItem());
    assertEquals(1, instance.subDivisionTabPane.getTabs().size());
    assertEquals(1, subDivisionTabController.ratingTable.getItems().size());
  }

  @Test
  public void testWithLeagueEntry() {
    LeagueEntryBean playerEntryBean = LeagueEntryBeanBuilder.create().defaultValues().score(8).subdivision(subdivisionBean1).get();
    when(leaderboardService.getEntries(subdivisionBean1)).thenReturn(
        Flux.just(leagueEntryBean1, playerEntryBean, leagueEntryBean2));
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(Mono.just(playerEntryBean));
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
    when(leaderboardService.getAllSubdivisions(season)).thenReturn(Flux.error(new FakeTestException()));

    instance.updateDisplayedPlayerStats();

    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadDivisions"));
  }

  @Test
  public void testUpdateDisplayedPlayerStatsWithLeagueEntryError() {
    when(leaderboardService.getLeagueEntryForPlayer(player, season)).thenReturn(Mono.error(new FakeTestException()));

    instance.updateDisplayedPlayerStats();

    verify(notificationService).addImmediateErrorNotification(any(), eq("leaderboard.failedToLoadEntry"));
  }
}
