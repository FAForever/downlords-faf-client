package com.faforever.client.leaderboard;

import com.faforever.client.builders.PlayerInfoBuilder;
import com.faforever.client.domain.api.Division;
import com.faforever.client.domain.api.LeagueEntry;
import com.faforever.client.domain.api.Subdivision;
import com.faforever.client.domain.server.PlayerInfo;
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

  private final PlayerInfo player = PlayerInfoBuilder.create().defaultValues().get();

  @BeforeEach
  public void setup() throws Exception {
    lenient().when(playerService.currentPlayerProperty()).thenReturn(new SimpleObjectProperty<>(player));
    lenient().when(playerService.getCurrentPlayer()).thenReturn(player);
    lenient().when(i18n.get(any())).thenAnswer(invocation -> invocation.getArgument(0));

    loadFxml("theme/leaderboard/leaderboard_rankings.fxml", clazz -> instance);
  }

  @Test
  public void testSetRankings() {
    Division division1 = Instancio.of(Division.class).set(field(Division::index), 1).create();
    Division division2 = Instancio.of(Division.class).set(field(Division::index), 2).create();
    Subdivision subdivision1 = Instancio.of(Subdivision.class)
                                        .set(field(Subdivision::division), division1)
                                        .set(field(Subdivision::id), 1)
                                        .set(field(Subdivision::index), 1)
                                        .create();
    Subdivision subdivision2 = Instancio.of(Subdivision.class)
                                        .set(field(Subdivision::division), division1)
                                        .set(field(Subdivision::id), 2)
                                        .set(field(Subdivision::index), 2)
                                        .create();
    Subdivision subdivision3 = Instancio.of(Subdivision.class)
                                        .set(field(Subdivision::division), division2)
                                        .set(field(Subdivision::id), 3)
                                        .set(field(Subdivision::index), 1)
                                        .create();
    Subdivision subdivision4 = Instancio.of(Subdivision.class)
                                        .set(field(Subdivision::division), division2)
                                        .set(field(Subdivision::id), 4)
                                        .set(field(Subdivision::index), 2)
                                        .create();

    instance.setSubdivisions(List.of(subdivision1, subdivision2, subdivision3, subdivision4));

    assertThat(instance.divisionPicker.getItems(), contains(division2, division1));
    assertThat(instance.divisionPicker.getSelectionModel().getSelectedItem(), is(division2));
    assertThat(instance.subdivisionButtons.getChildren(), hasSize(2));
    assertThat(instance.subdivisionToggleGroup.getSelectedToggle(),
               is(instance.subdivisionButtons.getChildren().getLast()));

    LeagueEntry leagueEntry = Instancio.of(LeagueEntry.class)
                                       .set(field(LeagueEntry::id), 1)
                                       .set(field(LeagueEntry::subdivision), subdivision1)
                                       .set(field(LeagueEntry::player), player)
                                       .create();
    instance.setLeagueEntries(List.of(leagueEntry));

    assertThat(instance.divisionPicker.getItems(), contains(division2, division1));
    assertThat(instance.divisionPicker.getSelectionModel().getSelectedItem(), is(division1));
    assertThat(instance.subdivisionButtons.getChildren(), hasSize(2));
    assertThat(instance.subdivisionToggleGroup.getSelectedToggle(),
               is(instance.subdivisionButtons.getChildren().getFirst()));
    assertThat(instance.ratingTable.getSelectionModel().getSelectedItem(), is(leagueEntry));
  }

}