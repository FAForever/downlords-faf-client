package com.faforever.client.game;

import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class GamesTableControllerTest extends AbstractPlainJavaFxTest {

  private GamesTableController instance;
  @Mock
  private I18n i18n;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private UiService uiService;
  @Mock
  private MapService mapService;
  @Mock
  private PreferencesService preferencesService;

  @Before
  public void setUp() throws Exception {
    instance = new GamesTableController(mapService, joinGameHelper, i18n, uiService, preferencesService);
    when(preferencesService.getPreferences()).thenReturn(new Preferences());

    loadFxml("theme/play/games_table.fxml", param -> instance);

    Platform.runLater(() -> getRoot().getChildren().addAll(instance.getRoot()));
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void test() throws Exception {
    instance.initializeGameTable(FXCollections.observableArrayList(
        GameBuilder.create().defaultValues().get(),
        GameBuilder.create().defaultValues().state(GameStatus.CLOSED).get()
    ));
  }

  @Test
  public void testKeepsSorting() {
    preferencesService.getPreferences().getGameListSorting().setAll(new Pair<>("hostColumn", SortType.DESCENDING));

    instance.initializeGameTable(FXCollections.observableArrayList(
        GameBuilder.create().defaultValues().get(),
        GameBuilder.create().defaultValues().state(GameStatus.CLOSED).get()
    ));

    assertThat(instance.gamesTable.getSortOrder(), hasSize(1));
    assertThat(instance.gamesTable.getSortOrder().get(0).getId(), is("hostColumn"));
    assertThat(instance.gamesTable.getSortOrder().get(0).getSortType(), is(SortType.DESCENDING));
  }

  @Test
  public void testSortingUpdatesPreferences() {
    assertThat(preferencesService.getPreferences().getGameListSorting(), hasSize(0));

    instance.initializeGameTable(FXCollections.observableArrayList(
        GameBuilder.create().defaultValues().get(),
        GameBuilder.create().defaultValues().state(GameStatus.CLOSED).get()
    ));

    TableColumn<Game, ?> column = instance.gamesTable.getColumns().get(0);
    column.setSortType(SortType.ASCENDING);
    instance.gamesTable.getSortOrder().add(column);

    assertThat(preferencesService.getPreferences().getGameListSorting(), hasSize(1));
    assertThat(
        preferencesService.getPreferences().getGameListSorting().get(0),
        equalTo(new Pair<>("passwordProtectionColumn", SortType.ASCENDING))
    );
  }
}
