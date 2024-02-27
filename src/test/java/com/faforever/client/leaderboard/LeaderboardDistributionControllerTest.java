package com.faforever.client.leaderboard;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.LeagueEntryBean;
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

  private final PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();

  @BeforeEach
  public void setup() throws Exception {
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
    lenient().when(i18n.get(any())).thenAnswer(invocation -> invocation.getArgument(0));

    loadFxml("theme/leaderboard/leaderboard_distribution.fxml", clazz -> instance);
  }

  @Test
  public void testSetData() {
    DivisionBean divisionBean1 = Instancio.of(DivisionBean.class).set(field(DivisionBean::index), 1).create();
    DivisionBean divisionBean2 = Instancio.of(DivisionBean.class).set(field(DivisionBean::index), 2).create();
    SubdivisionBean subdivision1 = Instancio.of(SubdivisionBean.class)
                                            .set(field(SubdivisionBean::id), 1)
                                            .set(field(SubdivisionBean::division), divisionBean1)
                                            .set(field(SubdivisionBean::index), 1)
                                            .create();
    SubdivisionBean subdivision2 = Instancio.of(SubdivisionBean.class)
                                            .set(field(SubdivisionBean::id), 2)
                                            .set(field(SubdivisionBean::division), divisionBean1)
                                            .set(field(SubdivisionBean::index), 2)
                                            .create();
    SubdivisionBean subdivision3 = Instancio.of(SubdivisionBean.class)
                                            .set(field(SubdivisionBean::id), 3)
                                            .set(field(SubdivisionBean::division), divisionBean2)
                                            .set(field(SubdivisionBean::index), 1)
                                            .create();
    SubdivisionBean subdivision4 = Instancio.of(SubdivisionBean.class)
                                            .set(field(SubdivisionBean::id), 4)
                                            .set(field(SubdivisionBean::division), divisionBean2)
                                            .set(field(SubdivisionBean::index), 2)
                                            .create();
    instance.setSubdivisions(List.of(subdivision1, subdivision2, subdivision3, subdivision4));

    ObservableList<Series<String, Integer>> series = instance.ratingDistributionChart.getData();

    assertThat(series, hasSize(2));
    series.forEach(serie -> assertThat(serie.getData(), hasSize(2)));

    PseudoClass highlighted = PseudoClass.getPseudoClass("highlighted-bar");

    instance.setLeagueEntries(List.of(Instancio.of(LeagueEntryBean.class)
                                               .set(field(LeagueEntryBean::subdivision), subdivision1)
                                               .set(field(LeagueEntryBean::id), 1)
                                               .set(field(LeagueEntryBean::player), player)
                                               .create()));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(1));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), contains(highlighted));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));

    instance.setLeagueEntries(List.of(Instancio.of(LeagueEntryBean.class)
                                               .set(field(LeagueEntryBean::subdivision), subdivision2)
                                               .set(field(LeagueEntryBean::id), 2)
                                               .set(field(LeagueEntryBean::player), player)
                                               .create()));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(1));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), contains(highlighted));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));

    instance.setLeagueEntries(List.of(Instancio.of(LeagueEntryBean.class)
                                               .set(field(LeagueEntryBean::subdivision), subdivision3)
                                               .set(field(LeagueEntryBean::id), 3)
                                               .set(field(LeagueEntryBean::player), player)
                                               .create()));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(1));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), contains(highlighted));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(contains(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(contains(highlighted)));

    instance.setLeagueEntries(List.of(Instancio.of(LeagueEntryBean.class)
                                               .set(field(LeagueEntryBean::subdivision), subdivision4)
                                               .set(field(LeagueEntryBean::id), 4)
                                               .set(field(LeagueEntryBean::player), player)
                                               .create()));

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