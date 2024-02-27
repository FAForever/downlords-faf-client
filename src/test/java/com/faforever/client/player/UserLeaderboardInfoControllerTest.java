package com.faforever.client.player;

import com.faforever.client.builders.LeaderboardRatingMapBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardRatingBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.leaderboard.LeaderboardService;
import com.faforever.client.test.PlatformTest;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    leaderboard = Instancio.of(LeaderboardBean.class).set(field(LeaderboardBean::technicalName), "1v1").create();
    player = PlayerBeanBuilder.create().defaultValues().username("junit").get();

    lenient().when(leaderboardService.loadDivisionImage(any()))
             .thenReturn(new Image("https://content.faforever.com/divisions/icons/unranked.png"));
    lenient().when(i18n.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

    loadFxml("theme/user_leaderboard_info.fxml", clazz -> instance);
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.root, instance.getRoot());
    assertNull(instance.getRoot().getParent());
  }

  @Test
  public void testSetLeaderboardInfo() {
    when(i18n.getOrDefault(leaderboard.technicalName(), leaderboard.nameKey())).thenReturn(leaderboard.technicalName());
    when(i18n.get("leaderboard.gameNumber", 47)).thenReturn("47 games");
    when(i18n.get("leaderboard.rating", 200)).thenReturn("200 rating");
    final LeaderboardRatingBean leaderboardRating = Instancio.of(LeaderboardRatingBean.class)
                                                             .set(field(LeaderboardRatingBean::mean), 500)
                                                             .set(field(LeaderboardRatingBean::deviation), 100)
                                                             .set(field(LeaderboardRatingBean::numberOfGames), 47)
                                                             .create();
    player.setLeaderboardRatings(
        LeaderboardRatingMapBuilder.create().put(leaderboard.technicalName(), leaderboardRating).get());

    instance.setLeaderboardInfo(player, leaderboard);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.divisionImage.isVisible());
    assertTrue(instance.divisionLabel.isVisible());
    assertEquals("1v1", instance.leaderboardNameLabel.getText());
    assertEquals("47 games", instance.gamesPlayedLabel.getText());
    assertEquals("200 rating", instance.ratingLabel.getText());
  }

  @Test
  public void testSetLeagueInfo() {
    LeagueEntryBean leagueEntry = Instancio.create(LeagueEntryBean.class);
    when(i18n.getOrDefault(leagueEntry.subdivision().division().nameKey(), "leagues.divisionName.%s".formatted(
        leagueEntry.subdivision().division().nameKey()))).thenReturn("bronze");
    when(i18n.get("leaderboard.divisionName", "bronze", leagueEntry.subdivision().nameKey())).thenReturn("bronze II");

    instance.setLeagueInfo(leagueEntry);
    WaitForAsyncUtils.waitForFxEvents();

    assertTrue(instance.divisionImage.isVisible());
    assertTrue(instance.divisionImage.isManaged());
    assertTrue(instance.divisionLabel.isVisible());
    assertTrue(instance.divisionLabel.isManaged());
    assertEquals("BRONZE II", instance.divisionLabel.getText());
  }

  @Test
  public void testUnrankedLeague() {
    assertTrue(instance.divisionImage.isVisible());
    assertTrue(instance.divisionImage.isManaged());
    assertTrue(instance.divisionLabel.isVisible());
    assertTrue(instance.divisionLabel.isManaged());
    assertEquals("TEAMMATCHMAKING.INPLACEMENT", instance.divisionLabel.getText());
  }
}
