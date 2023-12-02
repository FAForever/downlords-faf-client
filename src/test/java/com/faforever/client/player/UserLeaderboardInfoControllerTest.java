package com.faforever.client.player;

import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingBeanBuilder;
import com.faforever.client.builders.LeaderboardRatingMapBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.test.PlatformTest;
import javafx.scene.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

public class UserLeaderboardInfoControllerTest extends PlatformTest {

  @InjectMocks
  private UserLeaderboardInfoController instance;

  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;

  private LeaderboardBean leaderboard;
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    leaderboard = LeaderboardBeanBuilder.create().defaultValues().technicalName("1v1").get();
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();

    lenient().when(leaderboardService.loadDivisionImage(any()))
             .thenReturn(new Image("https://content.faforever.com/divisions/icons/unranked.png"));

    loadFxml("theme/user_leaderboard_info.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testSetLeaderboardInfo() {
    when(i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get("leaderboard.gameNumber", 47)).thenReturn("47 games");
    when(i18n.get("leaderboard.rating", 200)).thenReturn("200 rating");
    final LeaderboardRatingBean leaderboardRating = LeaderboardRatingBeanBuilder.create()
        .defaultValues()
        .numberOfGames(47)
        .mean(500)
        .deviation(100)
        .get();
    player.setLeaderboardRatings(LeaderboardRatingMapBuilder.create().put(leaderboard.getTechnicalName(), leaderboardRating).get());

    instance.setLeaderboardInfo(player, leaderboard);
    WaitForAsyncUtils.waitForFxEvents();

    assertFalse(instance.divisionImage.isVisible());
    assertFalse(instance.divisionImage.isManaged());
    assertFalse(instance.divisionLabel.isVisible());
    assertFalse(instance.divisionLabel.isManaged());
    assertEquals("1v1", instance.leaderboardNameLabel.getText());
    assertEquals("47 games", instance.gamesPlayedLabel.getText());
    assertEquals("200 rating", instance.ratingLabel.getText());
  }

  @Test
  public void testSetLeagueInfo() {
    LeagueEntryBean leagueEntry = LeagueEntryBeanBuilder.create().defaultValues().get();
    when(i18n.getOrDefault(leagueEntry.getSubdivision().getDivision().getNameKey(), leagueEntry.getSubdivision().getDivisionI18nKey())).thenReturn("bronze");
    when(i18n.get("leaderboard.divisionName", "bronze", leagueEntry.getSubdivision().getNameKey())).thenReturn("bronze II");

    instance.setLeagueInfo(leagueEntry);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.divisionImage.isVisible());
    assertTrue(instance.divisionImage.isManaged());
    assertTrue(instance.divisionLabel.isVisible());
    assertTrue(instance.divisionLabel.isManaged());
    assertEquals("BRONZE II", instance.divisionLabel.getText());
  }

  @Test
  public void testSetUnrankedLeague() {
    when(i18n.get("teammatchmaking.inPlacement")).thenReturn("unlisted");

    instance.setUnlistedLeague();
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.divisionImage.isVisible());
    assertTrue(instance.divisionImage.isManaged());
    assertTrue(instance.divisionLabel.isVisible());
    assertTrue(instance.divisionLabel.isManaged());
    assertEquals("UNLISTED", instance.divisionLabel.getText());
  }
}
