package com.faforever.client.game;

import com.faforever.client.builders.GamePlayerStatsBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  @Mock
  private RatingChangeLabelController ratingChangeLabelController;

  private ArrayList<PlayerBean> playerList = new ArrayList<>();
  private ObservableMap<String, List<GamePlayerStatsBean>> teams;
  private GamePlayerStatsBean playerStats;

  @BeforeEach
  public void setUp() throws Exception {
    PlayerBean player = PlayerBeanBuilder.create().defaultValues().id(1).get();
    playerList.add(player);
    teams = FXCollections.observableHashMap();

    when(uiService.loadFxml("theme/player_card.fxml")).thenReturn(playerCardController);
    when(uiService.loadFxml("theme/rating_change_label.fxml")).thenReturn(ratingChangeLabelController);
    when(playerCardController.ratingProperty()).thenReturn(new SimpleObjectProperty<>());
    when(playerCardController.factionProperty()).thenReturn(new SimpleObjectProperty<>());
    when(playerCardController.playerProperty()).thenReturn(new SimpleObjectProperty<>());
    when(playerCardController.getRoot()).thenReturn(new Label());
    when(ratingChangeLabelController.getRoot()).thenReturn(new Label());
    playerStats = GamePlayerStatsBeanBuilder.create()
        .defaultValues()
        .player(PlayerBeanBuilder.create().defaultValues().get())
        .get();

    teams.put("2", Collections.singletonList(playerStats));

    loadFxml("theme/team_card.fxml", param -> instance);
  }

  @Test
  public void setPlayersInTeam() {
    when(i18n.get("game.tooltip.teamTitle", 1, 1000)).thenReturn("1 (1000)");

    instance.setRatingProvider(playerBean -> 1000);
    instance.setTeamId(2);
    instance.setPlayers(playerList);
    instance.setRatingPrecision(RatingPrecision.ROUNDED);
    assertEquals("1 (1000)", instance.teamNameLabel.getText());
  }

  @Test
  public void showRatingChange() {
    instance.setTeamId(2);
    instance.setPlayers(playerList);
    instance.setRatingProvider(player -> RatingUtil.getRating(1000, 0));
    instance.setRatingPrecision(RatingPrecision.EXACT);
    instance.setStats(teams.get("2"));
    verify(ratingChangeLabelController).setRatingChange(playerStats);
  }

}
