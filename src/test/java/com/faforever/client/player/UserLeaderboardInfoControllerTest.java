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
import com.faforever.client.test.UITest;
import javafx.scene.image.Image;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class UserLeaderboardInfoControllerTest extends UITest {

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
    leaderboard = LeaderboardBeanBuilder.create().defaultValues().get();
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();

    when(i18n.getOrDefault(leaderboard.getTechnicalName(), leaderboard.getNameKey())).thenReturn(leaderboard.getTechnicalName());
    when(i18n.get("leaderboard.gameNumber", 10)).thenReturn("10 games");
    when(i18n.get("leaderboard.rating", 200)).thenReturn("200 rating");
    when(leaderboardService.loadDivisionImage(any())).thenReturn(new Image(""));

    loadFxml("theme/user_leaderboard_info.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testSetLeaderboardInfo() {
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
    assertEquals("test_name", instance.leaderboardNameLabel.getText());
    assertEquals("47 games", instance.gamesPlayedLabel.getText());
    assertEquals("200 rating", instance.ratingLabel.getText());
  }

  @Test
  public void testSetLeagueInfo() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().get();

    instance.setLeagueInfo(leagueEntryBean);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.divisionImage.isVisible());
    assertTrue(instance.divisionImage.isManaged());
    assertTrue(instance.divisionLabel.isVisible());
    assertTrue(instance.divisionLabel.isManaged());
    assertEquals("bronze", instance.divisionLabel.getText());
  }
}
