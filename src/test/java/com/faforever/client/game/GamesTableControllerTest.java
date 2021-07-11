package com.faforever.client.game;

import com.faforever.client.fx.Controller;
import com.faforever.client.fx.JavaFxUtil;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesBuilder;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
  @Mock
  private PlayerService playerService;
  @Mock
  private GameTooltipController gameTooltipController;
  @Mock
  private Controller<ImageView> imageViewController;
  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    instance = new GamesTableController(mapService, joinGameHelper, i18n, uiService, preferencesService, playerService);
    preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/play/game_tooltip.fxml")).thenReturn(gameTooltipController);
    when(uiService.loadFxml("theme/vault/map/map_preview_table_cell.fxml")).thenReturn(imageViewController);
    when(gameTooltipController.getRoot()).thenReturn(new Pane());
    when(imageViewController.getRoot()).thenReturn(new ImageView());
    when(i18n.get(any())).then(invocation -> invocation.getArguments()[0]);

    loadFxml("theme/play/games_table.fxml", param -> instance);

    JavaFxUtil.runLater(() -> getRoot().getChildren().addAll(instance.getRoot()));
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void test() throws Exception {
    JavaFxUtil.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBuilder.create().defaultValues().get(),
          GameBuilder.create().defaultValues().status(GameStatus.CLOSED).get()
      ));
    });
    WaitForAsyncUtils.waitForFxEvents();
  }

  @Test
  public void testPrivateGameColumnIsHidden() throws Exception {
    preferences.setShowPasswordProtectedGames(false);
    JavaFxUtil.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBuilder.create().defaultValues().get(),
          GameBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ));
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.passwordProtectionColumn.isVisible());
    preferences.setShowPasswordProtectedGames(true);
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.passwordProtectionColumn.isVisible());
  }

  @Test
  public void testModdedGameColumnIsHidden() throws Exception {
    preferences.setShowModdedGames(false);
    JavaFxUtil.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBuilder.create().defaultValues().get(),
          GameBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ));
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertFalse(instance.modsColumn.isVisible());
    preferences.setShowModdedGames(true);
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.modsColumn.isVisible());
  }

  @Test
  public void testPrivateGameColumnIsShownWithCoop() throws Exception {
    preferences.setShowPasswordProtectedGames(false);
    JavaFxUtil.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBuilder.create().defaultValues().get(),
          GameBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ), string -> string, false);
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.passwordProtectionColumn.isVisible());
  }

  @Test
  public void testModdedGameColumnIsShownWithCoop() throws Exception {
    preferences.setShowModdedGames(false);
    JavaFxUtil.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBuilder.create().defaultValues().get(),
          GameBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ), string -> string, false);
    });
    WaitForAsyncUtils.waitForFxEvents();
    assertTrue(instance.modsColumn.isVisible());
  }

  @Test
  public void testKeepsSorting() {
    preferences.getGameListSorting().setAll(new Pair<>("hostColumn", SortType.DESCENDING));

    JavaFxUtil.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBuilder.create().defaultValues().get(),
          GameBuilder.create().defaultValues().status(GameStatus.CLOSED).get()
      ));
    });
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(instance.gamesTable.getSortOrder(), hasSize(1));
    assertThat(instance.gamesTable.getSortOrder().get(0).getId(), is("hostColumn"));
    assertThat(instance.gamesTable.getSortOrder().get(0).getSortType(), is(SortType.DESCENDING));
  }

  @Test
  public void testSortingUpdatesPreferences() {
    assertThat(preferencesService.getPreferences().getGameListSorting(), hasSize(0));

    JavaFxUtil.runLater(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBuilder.create().defaultValues().get(),
          GameBuilder.create().defaultValues().status(GameStatus.CLOSED).get()
      ));
      TableColumn<Game, ?> column = instance.gamesTable.getColumns().get(0);
      column.setSortType(SortType.ASCENDING);
      instance.gamesTable.getSortOrder().add(column);
    });
    WaitForAsyncUtils.waitForFxEvents();

    assertThat(preferencesService.getPreferences().getGameListSorting(), hasSize(1));
    assertThat(
        preferencesService.getPreferences().getGameListSorting().get(0),
        equalTo(new Pair<>("passwordProtectionColumn", SortType.ASCENDING))
    );
  }
}
