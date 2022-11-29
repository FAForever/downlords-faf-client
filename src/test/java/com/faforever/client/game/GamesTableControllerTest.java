package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.builders.PreferencesBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.commons.lobby.GameStatus;
import javafx.collections.FXCollections;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class GamesTableControllerTest extends UITest {

  @InjectMocks
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
  private ImageViewHelper imageViewHelper;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private GameTooltipController gameTooltipController;

  private Preferences preferences;

  @BeforeEach
  public void setUp() throws Exception {
    preferences = PreferencesBuilder.create().defaultValues().get();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(uiService.loadFxml("theme/play/game_tooltip.fxml")).thenReturn(gameTooltipController);
    when(gameTooltipController.getRoot()).thenReturn(new Pane());
    when(i18n.get(any())).then(invocation -> invocation.getArguments()[0]);

    loadFxml("theme/play/games_table.fxml", param -> instance);

    runOnFxThreadAndWait(() -> getRoot().getChildren().addAll(instance.getRoot()));
  }

  @Test
  public void testInitializeGameTable() {
    runOnFxThreadAndWait(() -> instance.initializeGameTable(FXCollections.observableArrayList(
        GameBeanBuilder.create().defaultValues().get(),
        GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).get()
    )));
    assertEquals(2, instance.gamesTable.getItems().size());
  }

  @Test
  public void testPrivateGameColumnIsHidden() {
    runOnFxThreadAndWait(() -> {
      preferences.setHidePrivateGames(true);
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBeanBuilder.create().defaultValues().get(),
          GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ));
    });
    assertFalse(instance.passwordProtectionColumn.isVisible());
    runOnFxThreadAndWait(() -> preferences.setHidePrivateGames(false));
    assertTrue(instance.passwordProtectionColumn.isVisible());
  }

  @Test
  public void testModdedGameColumnIsHidden() {
    runOnFxThreadAndWait(() -> {
      preferences.setHideModdedGames(true);
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBeanBuilder.create().defaultValues().get(),
          GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ));
    });
    assertFalse(instance.modsColumn.isVisible());
    runOnFxThreadAndWait(() -> preferences.setHideModdedGames(false));
    assertTrue(instance.modsColumn.isVisible());
  }

  @Test
  public void testPrivateGameColumnIsShownWithCoop() {
    runOnFxThreadAndWait(() -> {
      preferences.setHidePrivateGames(false);
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBeanBuilder.create().defaultValues().get(),
          GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ), string -> string, false);
    });
    assertTrue(instance.passwordProtectionColumn.isVisible());
  }

  @Test
  public void testModdedGameColumnIsShownWithCoop() {
    runOnFxThreadAndWait(() -> {
      preferences.setHideModdedGames(false);
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBeanBuilder.create().defaultValues().get(),
          GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).password("ABC").get()
      ), string -> string, false);
    });
    assertTrue(instance.modsColumn.isVisible());
  }

  @Test
  public void testKeepsSorting() {
    runOnFxThreadAndWait(() -> {
      preferences.getGameTableSorting().put("hostColumn", SortType.DESCENDING);
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBeanBuilder.create().defaultValues().get(),
          GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).get()
      ));
    });

    assertThat(instance.gamesTable.getSortOrder(), hasSize(1));
    assertThat(instance.gamesTable.getSortOrder().get(0).getId(), is("hostColumn"));
    assertThat(instance.gamesTable.getSortOrder().get(0).getSortType(), is(SortType.DESCENDING));
  }

  @Test
  public void testSortingUpdatesPreferences() {
    assertThat(preferencesService.getPreferences().getGameTableSorting().entrySet(), hasSize(0));

    runOnFxThreadAndWait(() -> {
      instance.initializeGameTable(FXCollections.observableArrayList(
          GameBeanBuilder.create().defaultValues().get(),
          GameBeanBuilder.create().defaultValues().status(GameStatus.CLOSED).get()
      ));
      TableColumn<GameBean, ?> column = instance.gamesTable.getColumns().get(0);
      column.setSortType(SortType.ASCENDING);
      instance.gamesTable.getSortOrder().add(column);
    });

    assertThat(preferencesService.getPreferences().getGameTableSorting().entrySet(), hasSize(1));
    assertThat(
        preferencesService.getPreferences().getGameTableSorting().get("passwordProtectionColumn"),
        equalTo(SortType.ASCENDING)
    );
  }
}
