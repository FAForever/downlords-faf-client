package com.faforever.client.leaderboard;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.instancio.Select.field;
import static org.instancio.Select.scope;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public class LeaderboardDistributionControllerTest extends PlatformTest {

  @InjectMocks
  private LeaderboardDistributionController instance;

  @Mock
  private I18n i18n;
  @Mock
  private PlayerService playerService;

  private final PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();

  @BeforeEach
  public void setup() throws Exception {
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
    lenient().when(i18n.get(any())).thenAnswer(invocation -> invocation.getArgument(0));

    loadFxml("theme/leaderboard/leaderboard_distribution.fxml", _ -> instance);
  }

  @Test
  public void testSetData() {
    Subdivision subdivision1 = Instancio.of(Subdivision.class)
                                        .set(field(Division::index).within(scope(Division.class)), 1)
                                        .set(field(Subdivision::id), 1)
                                        .set(field(Subdivision::index), 1)
                                        .create();
    Subdivision subdivision2 = Instancio.of(Subdivision.class)
                                        .set(field(Division::index).within(scope(Division.class)), 1)
                                        .set(field(Subdivision::id), 2)
                                        .set(field(Subdivision::index), 2)
                                        .create();
    Subdivision subdivision3 = Instancio.of(Subdivision.class)
                                        .set(field(Division::index).within(scope(Division.class)), 2)
                                        .set(field(Subdivision::id), 3)
                                        .set(field(Subdivision::index), 1)
                                        .create();
    Subdivision subdivision4 = Instancio.of(Subdivision.class)
                                        .set(field(Division::index).within(scope(Division.class)), 2)
                                        .set(field(Subdivision::id), 4)
                                        .set(field(Subdivision::index), 2)
                                        .create();
    runOnFxThreadAndWait(
        () -> instance.setSubdivisions(List.of(subdivision1, subdivision2, subdivision3, subdivision4)));

    ObservableList<Series<String, Integer>> series = instance.ratingDistributionChart.getData();

    assertThat(series, hasSize(2));
    series.forEach(serie -> assertThat(serie.getData(), hasSize(2)));

    PseudoClass highlighted = PseudoClass.getPseudoClass("highlighted-bar");

    runOnFxThreadAndWait(() -> instance.setLeagueEntries(List.of(Instancio.of(LeagueEntry.class)
                                               .set(field(LeagueEntry::subdivision), subdivision1)
                                               .set(field(LeagueEntry::id), 1)
                                               .set(field(LeagueEntry::player), player).create())));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(1));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), hasItem(highlighted));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(hasItem(highlighted)));

    runOnFxThreadAndWait(() -> instance.setLeagueEntries(List.of(Instancio.of(LeagueEntry.class)
                                               .set(field(LeagueEntry::subdivision), subdivision2)
                                               .set(field(LeagueEntry::id), 2)
                                               .set(field(LeagueEntry::player), player).create())));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(1));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), hasItem(highlighted));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(hasItem(highlighted)));

    runOnFxThreadAndWait(() -> instance.setLeagueEntries(List.of(Instancio.of(LeagueEntry.class)
                                               .set(field(LeagueEntry::subdivision), subdivision3)
                                               .set(field(LeagueEntry::id), 3)
                                               .set(field(LeagueEntry::player), player).create())));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(1));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(0));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), hasItem(highlighted));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), not(hasItem(highlighted)));

    runOnFxThreadAndWait(() -> instance.setLeagueEntries(List.of(Instancio.of(LeagueEntry.class)
                                               .set(field(LeagueEntry::subdivision), subdivision4)
                                               .set(field(LeagueEntry::id), 4)
                                               .set(field(LeagueEntry::player), player).create())));

    assertThat(series.getFirst().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getFirst().getData().getLast().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getFirst().getYValue(), equalTo(0));
    assertThat(series.getLast().getData().getLast().getYValue(), equalTo(1));

    assertThat(series.getFirst().getData().getFirst().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getFirst().getData().getLast().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getLast().getData().getFirst().getNode().getPseudoClassStates(), not(hasItem(highlighted)));
    assertThat(series.getLast().getData().getLast().getNode().getPseudoClassStates(), hasItem(highlighted));
  }
}