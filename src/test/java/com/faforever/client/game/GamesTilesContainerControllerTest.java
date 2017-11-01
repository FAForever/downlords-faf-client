package com.faforever.client.game;

import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class GamesTilesContainerControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private GameTileController gameTileController;
  @Mock
  private UiService uiService;
  @Mock
  private PreferencesService preferencesService;

  private GamesTilesContainerController instance;
  private Preferences preferences;

  @Before
  public void setUp() throws Exception {
    instance = new GamesTilesContainerController(uiService, preferencesService);

    when(uiService.loadFxml("theme/play/game_card.fxml")).thenReturn(gameTileController);
    preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(preferences);
    when(gameTileController.getRoot()).thenReturn(new Pane()).thenReturn(new FlowPane()).thenReturn(new StackPane());

    loadFxml("theme/play/games_tiles_container.fxml", clazz -> instance);
  }

  @Test
  public void testCreateTiledFlowPaneWithEmptyList() throws Exception {
    ObservableList<Game> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList, new ChoiceBox<>());

    assertThat(instance.tiledFlowPane.getChildren(), empty());
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedList() throws Exception {
    when(gameTileController.getRoot()).thenReturn(new Pane());
    ObservableList<Game> observableList = FXCollections.observableArrayList();
    observableList.add(new Game());

    instance.createTiledFlowPane(observableList, new ChoiceBox<>());

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPostInstantiatedGameInfoBean() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    instance.tiledFlowPane.getChildren().addListener((Observable observable) -> {
      latch.countDown();
    });

    doAnswer(invocation -> new Pane()).when(gameTileController).getRoot();

    ObservableList<Game> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList, new ChoiceBox<>());
    observableList.add(new Game());

    latch.await();
    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedListAndPostInstantiatedGameInfoBean() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    ObservableList<Node> children = instance.tiledFlowPane.getChildren();
    children.addListener((Observable observable) -> {
      latch.countDown();
    });

    doAnswer(invocation -> new Pane()).when(gameTileController).getRoot();

    ObservableList<Game> observableList = FXCollections.observableArrayList();

    observableList.add(GameBuilder.create().defaultValues().get());
    instance.createTiledFlowPane(observableList, new ChoiceBox<>());
    observableList.add(GameBuilder.create().defaultValues().get());

    latch.await();

    WaitForAsyncUtils.waitForFxEvents();

    assertThat(children, hasSize(2));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), instanceOf(Node.class));
  }

  @Test
  public void testSorting() throws Exception {
    ObservableList<Game> observableList = FXCollections.observableArrayList();
    Game game1 = GameBuilder.create().defaultValues().get();
    Game game2 = GameBuilder.create().defaultValues().title("xyz").get();

    game1.setNumPlayers(12);
    game1.setId(234);
    game2.setId(123);
    game2.setNumPlayers(1);


    observableList.addAll(game1, game2);

    preferences.setGameTileSortingOrder(TilesSortingOrder.PLAYER_ASC);

    instance.createTiledFlowPane(observableList, new ChoiceBox<>());
    assertEquals(instance.uidToGameCard.get(game2.getId()), instance.tiledFlowPane.getChildren().get(0));
  }
}
