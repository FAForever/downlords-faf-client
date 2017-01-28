package com.faforever.client.game;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class GamesTilesContainerControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private GameTileController gameTileController;
  @Mock
  private UiService uiService;

  private GamesTilesContainerController instance;

  @Before
  public void setUp() throws Exception {
    instance = new GamesTilesContainerController(uiService);

    when(uiService.loadFxml("theme/play/game_card.fxml")).thenReturn(gameTileController);
    when(gameTileController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/games_tiles_container.fxml", clazz -> instance);
  }

  @Test
  public void testCreateTiledFlowPaneWithEmptyList() throws Exception {
    when(gameTileController.getRoot()).thenReturn(new Pane());
    ObservableList<Game> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList);

    assertThat(instance.tiledFlowPane.getChildren(), empty());
  }

  @Test
  public void testUpdate() throws Exception {
    Game game = GameBuilder.create().get();
    instance.updateTiles(FXCollections.observableArrayList(game));

    WaitForAsyncUtils.waitForFxEvents();
    assertThat(instance.selectedGameProperty().get(), CoreMatchers.equalTo(game));
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedList() throws Exception {
    when(gameTileController.getRoot()).thenReturn(new Pane());
    ObservableList<Game> observableList = FXCollections.observableArrayList();
    observableList.add(new Game());

    instance.createTiledFlowPane(observableList);

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPostInstantiatedGameInfoBean() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    instance.tiledFlowPane.getChildren().addListener((Observable observable) -> latch.countDown());

    doAnswer(invocation -> new Pane()).when(gameTileController).getRoot();

    ObservableList<Game> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList);
    observableList.add(new Game());

    latch.await();
    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedListAndPostInstantiatedGameInfoBean() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    ObservableList<Node> children = instance.tiledFlowPane.getChildren();
    children.addListener((Observable observable) -> latch.countDown());

    doAnswer(invocation -> new Pane()).when(gameTileController).getRoot();

    ObservableList<Game> observableList = FXCollections.observableArrayList();

    observableList.add(GameBuilder.create().defaultValues().get());
    instance.createTiledFlowPane(observableList);
    observableList.add(GameBuilder.create().defaultValues().get());

    latch.await();

    assertThat(children, hasSize(2));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), instanceOf(Node.class));
  }
}
