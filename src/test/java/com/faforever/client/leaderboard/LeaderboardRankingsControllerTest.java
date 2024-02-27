package com.faforever.client.leaderboard;

import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.domain.DivisionBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.i18n.I18n;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.PlatformTest;
import javafx.beans.property.SimpleObjectProperty;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

public class LeaderboardRankingsControllerTest extends PlatformTest {

  @InjectMocks
  private LeaderboardRankingsController instance;

  @Mock
  private I18n i18n;
  @Mock
  private PlayerService playerService;
  @Mock
  private ContextMenuBuilder contextMenuBuilder;

  private final PlayerBean player = PlayerBeanBuilder.create().defaultValues().get();

  @BeforeEach
  public void setup() throws Exception {
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
    lenient().when(i18n.get(any())).thenAnswer(invocation -> invocation.getArgument(0));

    loadFxml("theme/leaderboard/leaderboard_rankings.fxml", clazz -> instance);
  }

  @Test
  public void testSetRankings() {
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

    assertThat(instance.divisionPicker.getItems(), contains(divisionBean2, divisionBean1));
    assertThat(instance.divisionPicker.getSelectionModel().getSelectedItem(), is(divisionBean2));
    assertThat(instance.subdivisionButtons.getChildren(), hasSize(2));
    assertThat(instance.subdivisionToggleGroup.getSelectedToggle(),
               is(instance.subdivisionButtons.getChildren().getLast()));

    LeagueEntryBean leagueEntry = Instancio.of(LeagueEntryBean.class)
                                           .set(field(LeagueEntryBean::id), 1)
                                           .set(field(LeagueEntryBean::subdivision), subdivision1)
                                           .set(field(LeagueEntryBean::player), player)
                                           .create();
    instance.setLeagueEntries(List.of(leagueEntry));

    assertThat(instance.divisionPicker.getItems(), contains(divisionBean2, divisionBean1));
    assertThat(instance.divisionPicker.getSelectionModel().getSelectedItem(), is(divisionBean1));
    assertThat(instance.subdivisionButtons.getChildren(), hasSize(2));
    assertThat(instance.subdivisionToggleGroup.getSelectedToggle(),
               is(instance.subdivisionButtons.getChildren().getFirst()));
    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem(), is(leagueEntry));
  }

}