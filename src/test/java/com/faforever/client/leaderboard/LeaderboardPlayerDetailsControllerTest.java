package com.faforever.client.leaderboard;

import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.LeagueSeason;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import javafx.scene.image.Image;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public class LeaderboardPlayerDetailsControllerTest extends PlatformTest {

  @InjectMocks
  private LeaderboardPlayerDetailsController instance;

  @Mock
  private I18n i18n;
  @Mock
  private LeaderboardService leaderboardService;

  @BeforeEach
  public void setup() throws Exception {
    lenient().when(i18n.get(any())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(leaderboardService.loadDivisionImage(any())).thenReturn(new Image("http://localhost"));

    loadFxml("theme/leaderboard/leaderboard_player_details.fxml", clazz -> instance);
  }

  @Test
  public void testDetailsPlaced() {
    LeagueSeason leagueSeason = Instancio.create(LeagueSeason.class);
    LeagueEntry leagueEntry = Instancio.create(LeagueEntry.class);
    instance.setLeagueEntry(leagueEntry);
    instance.setLeagueSeason(leagueSeason);

    assertFalse(instance.placementLabel.isVisible());
    assertTrue(instance.playerDivisionImageView.isVisible());
  }

  @Test
  public void testDetailsNotPlaced() {
    LeagueSeason leagueSeason = Instancio.create(LeagueSeason.class);
    LeagueEntry leagueEntry = Instancio.of(LeagueEntry.class).ignore(field(LeagueEntry::subdivision)).create();
    instance.setLeagueEntry(leagueEntry);
    instance.setLeagueSeason(leagueSeason);

    assertTrue(instance.placementLabel.isVisible());
    assertFalse(instance.playerDivisionImageView.isVisible());
  }

}