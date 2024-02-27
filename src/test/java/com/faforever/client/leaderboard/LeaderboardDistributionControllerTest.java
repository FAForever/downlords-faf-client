package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.chart.XYChart.Series;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public class LeaderboardDistributionControllerTest extends PlatformTest {

  @InjectMocks
  private LeaderboardDistributionController instance;

  @Mock
  private I18n i18n;
  @Mock
  private PlayerService playerService;

  @BeforeEach
  public void setup() throws Exception {
    PlayerBean playerBean = PlayerBeanBuilder.create().defaultValues().get();
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(playerBean));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(playerBean);
    lenient().when(i18n.get(any())).thenAnswer(invocation -> invocation.getArgument(0));

    loadFxml("theme/leaderboard/leaderboard_distribution.fxml", clazz -> instance);
  }

  @Test
  public void testSetData() {
    DivisionBean divisionBean1 = Instancio.of(DivisionBean.class).set(field(DivisionBean::index), 1).create();
    DivisionBean divisionBean2 = Instancio.of(DivisionBean.class).set(field(DivisionBean::index), 2).create();
    SubdivisionBean subdivision1 = SubdivisionBeanBuilder.create()
                                                         .defaultValues()
                                                         .id(1)
                                                         .division(divisionBean1)
                                                         .index(1)
                                                         .get();
    SubdivisionBean subdivision2 = SubdivisionBeanBuilder.create()
                                                         .defaultValues()
                                                         .id(2)
                                                         .division(divisionBean1)
                                                         .index(2)
                                                         .get();
    SubdivisionBean subdivision3 = SubdivisionBeanBuilder.create()
                                                         .defaultValues()
                                                         .id(3)
                                                         .division(divisionBean2)
                                                         .index(1)
                                                         .get();
    SubdivisionBean subdivision4 = SubdivisionBeanBuilder.create()
                                                         .defaultValues()
                                                         .id(4)
                                                         .division(divisionBean2)
                                                         .index(2)
                                                         .get();
    instance.setSubdivisions(List.of(subdivision1, subdivision2, subdivision3, subdivision4));

    ObservableList<Series<String, Integer>> series = instance.ratingDistributionChart.getData();

    assertThat(series, hasSize(2));
    series.forEach(serie -> assertThat(serie.getData(), hasSize(2)));

    PseudoClass highlighted = PseudoClass.getPseudoClass("highlighted-bar");

    instance.setLeagueEntries(
        List.of(LeagueEntryBeanBuilder.create().defaultValues().id(1).subdivision(subdivision1).get()));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(1));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), contains(highlighted));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));

    instance.setLeagueEntries(
        List.of(LeagueEntryBeanBuilder.create().defaultValues().id(2).subdivision(subdivision2).get()));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(1));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), contains(highlighted));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));

    instance.setLeagueEntries(
        List.of(LeagueEntryBeanBuilder.create().defaultValues().id(3).subdivision(subdivision3).get()));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(1));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), contains(highlighted));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));

    instance.setLeagueEntries(
        List.of(LeagueEntryBeanBuilder.create().defaultValues().id(4).subdivision(subdivision4).get()));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(1));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), contains(highlighted));
  }
}