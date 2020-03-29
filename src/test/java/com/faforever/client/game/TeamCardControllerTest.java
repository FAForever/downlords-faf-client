package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.replay.Replay.PlayerStats;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.Rating;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Label;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TeamCardControllerTest extends AbstractPlainJavaFxTest {
  private TeamCardController instance;
  @Mock
  private Player player;
  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerCardTooltipController playerCardTooltipController;
  @Mock
  private RatingChangeLabelController ratingChangeLabelController;

  private ArrayList<Player> playerList;
  private ObservableMap<String, List<PlayerStats>> teams;
  private PlayerStats playerStats;

  @Before
  public void setUp() throws IOException {
    instance = new TeamCardController(uiService, i18n);
    playerList = new ArrayList<>();
    playerList.add(player);
    teams = FXCollections.observableHashMap();

    when(uiService.loadFxml("theme/player_card_tooltip.fxml")).thenReturn(playerCardTooltipController);
    when(uiService.loadFxml("theme/rating_change_label.fxml")).thenReturn(ratingChangeLabelController);
    when(playerCardTooltipController.getRoot()).thenReturn(new Label());
    when(ratingChangeLabelController.getRoot()).thenReturn(new Label());
    when(player.getId()).thenReturn(1);

    playerStats = new PlayerStats(1, 1000, 0, 1100d, 0d, 0, null);
    teams.put("2", Collections.singletonList(playerStats));

    loadFxml("theme/team_card.fxml", param -> instance);
  }

  @Test
  public void setPlayersInTeam() throws Exception {
    instance.setPlayersInTeam("2", playerList, player -> new Rating(1000, 0), null, RatingType.ROUNDED);
    verify(i18n).get("game.tooltip.teamTitle", 1, 1000);
  }

  @Test
  public void showRatingChange() throws Exception {
    instance.setPlayersInTeam("2", playerList, player -> new Rating(1000, 0), null, RatingType.EXACT);
    instance.showRatingChange(teams);
    verify(ratingChangeLabelController).setRatingChange(playerStats);
  }

}
