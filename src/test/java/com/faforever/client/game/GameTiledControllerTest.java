package com.faforever.client.game;

import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class GameTiledControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  GameCardController gameCardController;
  @Mock
  ApplicationContext applicationContext;
  private GameTiledController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("games_tiled.fxml");
    instance.applicationContext = applicationContext;

    when(applicationContext.getBean(GameCardController.class)).thenReturn(gameCardController);

  }

  @Test
  public void testCreateTiledFlowPaneWithEmptyList() throws Exception {
    when(gameCardController.getRoot()).thenReturn(new Pane());
    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList);

    assertThat(instance.tiledFlowPane.getChildren(), empty());
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedList() throws Exception {
    when(gameCardController.getRoot()).thenReturn(new Pane());
    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();
    observableList.add(new GameInfoBean());

    instance.createTiledFlowPane(observableList);

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPostInstantiatedGameInfoBean() throws Exception {
    when(gameCardController.getRoot()).thenReturn(new Pane());
    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList);
    observableList.add(new GameInfoBean());

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedListAndPostInstantiatedGameInfoBean() throws Exception {
    when(gameCardController.getRoot())
        .thenReturn(new Pane())
        .thenReturn(new Pane());

    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();

    observableList.add(new GameInfoBean());
    instance.createTiledFlowPane(observableList);
    observableList.add(new GameInfoBean());

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(2));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), instanceOf(Node.class));
  }
}
