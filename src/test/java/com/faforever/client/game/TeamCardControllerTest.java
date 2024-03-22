package com.faforever.client.game;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.GameOutcome;
import com.faforever.client.domain.api.GamePlayerStats;
import com.faforever.client.domain.server.PlayerInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.replay.DisplayType;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Label;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.instancio.Select.field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamCardControllerTest extends PlatformTest {
  @InjectMocks
  private TeamCardController instance;

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerCardController playerCardController;

  private final ArrayList<PlayerInfo> playerList = new ArrayList<>();

  private ObservableMap<String, List<GamePlayerStats>> teams;
  private GamePlayerStats playerStats;

  @BeforeEach
  public void setUp() throws Exception {
    PlayerInfo player = PlayerInfoBuilder.create().defaultValues().id(1).get();
    playerList.add(player);
    teams = FXCollections.observableHashMap();

    lenient().when(uiService.loadFxml("theme/player_card.fxml")).thenReturn(playerCardController);
    lenient().when(playerCardController.ratingProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(playerCardController.divisionProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(playerCardController.factionProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(playerCardController.displayTypeProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(playerCardController.playerProperty()).thenReturn(new SimpleObjectProperty<>());
    lenient().when(playerCardController.getRoot()).thenReturn(new Label());
    playerStats = Instancio.of(GamePlayerStats.class).set(field(GamePlayerStats::player), player).create();

    teams.put("2", Collections.singletonList(playerStats));

    loadFxml("theme/team_card.fxml", param -> instance);
  }

  @Test
  public void setPlayersInTeam() {
    when(i18n.get("game.tooltip.teamTitle", 1)).thenReturn("Team 1");

    instance.setRatingProvider(player -> 1000);
    instance.setTeamId(2);
    instance.setPlayers(playerList);
    instance.setRatingPrecision(RatingPrecision.ROUNDED);
    assertEquals("Team 1", instance.teamNameLabel.getText());
  }

  @Test
  public void showRatingChange() {
    instance.setTeamId(2);
    instance.setPlayers(playerList);
    instance.setRatingProvider(player -> RatingUtil.getRating(1000, 0));
    instance.setRatingPrecision(RatingPrecision.EXACT);
    WaitForAsyncUtils.waitForFxEvents();
    instance.setStats(teams.get("2"));
    verify(playerCardController).setPlayerStats(playerStats);
    assertTrue(instance.teamRatingLabel.isVisible());
  }

  @Test
  public void showNoRating() {
    instance.setTeamId(2);
    instance.setDisplayType(DisplayType.DIVISION);
    instance.setPlayers(playerList);
    instance.setRatingProvider(player -> RatingUtil.getRating(1000, 0));
    instance.setRatingPrecision(RatingPrecision.EXACT);
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.teamRatingLabel.isVisible());
  }

  @Test
  public void showGameResult() {
    when(i18n.get("game.resultDraw")).thenReturn("Draw");

    instance.setTeamOutcome(GameOutcome.DRAW);
    instance.showGameResult();
    assertEquals("Draw", instance.gameResultLabel.getText());
    assertTrue(instance.gameResultLabel.isVisible());
  }

}
