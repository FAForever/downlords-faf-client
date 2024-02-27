package com.faforever.client.leaderboard;

import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
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

  @BeforeEach
  public void setup() throws Exception {
    PlayerBean playerBean = PlayerBeanBuilder.create().defaultValues().get();
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(playerBean));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(playerBean);
    lenient().when(i18n.get(any())).thenAnswer(invocation -> invocation.getArgument(0));

    loadFxml("theme/leaderboard/leaderboard_rankings.fxml", clazz -> instance);
  }

  @Test
  public void testSetRankings() {
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

    assertThat(instance.divisionPicker.getItems(), contains(divisionBean2, divisionBean1));
    assertThat(instance.divisionPicker.getSelectionModel().getSelectedItem(), is(divisionBean2));
    assertThat(instance.subdivisionButtons.getChildren(), hasSize(2));
    assertThat(instance.subdivisionToggleGroup.getSelectedToggle(),
               is(instance.subdivisionButtons.getChildren().getLast()));

    LeagueEntryBean leagueEntry = LeagueEntryBeanBuilder.create().defaultValues().id(1).subdivision(subdivision1).get();
    instance.setLeagueEntries(List.of(leagueEntry));

    assertThat(instance.divisionPicker.getItems(), contains(divisionBean2, divisionBean1));
    assertThat(instance.divisionPicker.getSelectionModel().getSelectedItem(), is(divisionBean1));
    assertThat(instance.subdivisionButtons.getChildren(), hasSize(2));
    assertThat(instance.subdivisionToggleGroup.getSelectedToggle(),
               is(instance.subdivisionButtons.getChildren().getFirst()));
    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem(), is(leagueEntry));
  }

}