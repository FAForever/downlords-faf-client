package com.faforever.client.game;

import com.faforever.client.builders.GamePlayerStatsBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.GamePlayerStatsBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.RatingUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TeamCardControllerTest extends UITest {
  private TeamCardController instance;

  @Mock
  private I18n i18n;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerCardTooltipController playerCardTooltipController;
  @Mock
  private RatingChangeLabelController ratingChangeLabelController;

  private PlayerBean player;
  private ArrayList<PlayerBean> playerList;
  private ObservableMap<String, List<GamePlayerStatsBean>> teams;
  private GamePlayerStatsBean playerStats;

  //Create Panel to initialize the graphics
  @BeforeAll
  public static void initGraphics() {
    JFXPanel panel = new JFXPanel();
  }

  @BeforeEach
  public void setUp() throws IOException {
    player = PlayerBeanBuilder.create().defaultValues().id(1).get();
    instance = new TeamCardController(uiService, i18n);
    playerList = new ArrayList<>();
    playerList.add(player);
    teams = FXCollections.observableHashMap();

    when(uiService.loadFxml("theme/player_card_tooltip.fxml")).thenReturn(playerCardTooltipController);
    when(uiService.loadFxml("theme/rating_change_label.fxml")).thenReturn(ratingChangeLabelController);
    when(playerCardTooltipController.getRoot()).thenReturn(new Label());
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
    instance.setPlayersInTeam("2", playerList, player -> RatingUtil.getRating(1000, 0), null, RatingPrecision.ROUNDED);
    verify(i18n).get("game.tooltip.teamTitle", 1, 1000);
  }

  @Test
  public void showRatingChange() {
    instance.setPlayersInTeam("2", playerList, player -> RatingUtil.getRating(1000, 0), null, RatingPrecision.EXACT);
    instance.showRatingChange(teams);
    verify(ratingChangeLabelController).setRatingChange(playerStats);
  }

}
