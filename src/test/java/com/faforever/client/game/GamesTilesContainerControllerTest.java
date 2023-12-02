package com.faforever.client.game;

import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.GameBean;
import com.faforever.client.game.GamesTilesContainerController.TilesSortingOrder;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class GamesTilesContainerControllerTest extends PlatformTest {

  @Mock
  private GameTileController gameTileController;
  @Mock
  private UiService uiService;

  @Mock
  private GameTooltipController gameTooltipController;

  @InjectMocks
  private GamesTilesContainerController instance;

  @BeforeEach
  public void setUp() throws Exception {
    when(uiService.loadFxml("theme/play/game_card.fxml")).thenReturn(gameTileController);
    when(uiService.loadFxml("theme/play/game_tooltip.fxml")).thenReturn(gameTooltipController);
    when(gameTooltipController.getRoot()).thenReturn(new Pane());
    when(gameTileController.getRoot()).thenReturn(new Pane()).thenReturn(new FlowPane()).thenReturn(new StackPane());

    loadFxml("theme/play/games_tiles_container.fxml", clazz -> {
      if (clazz == GameTooltipController.class) {
        return gameTooltipController;
      }
      return instance;
    });
  }

  @Test
  public void testCreateTiledFlowPaneWithEmptyList() {
    ObservableList<GameBean> observableList = FXCollections.observableArrayList();

    runOnFxThreadAndWait(() -> instance.createTiledFlowPane(observableList));
    assertThat(instance.tiledFlowPane.getChildren(), empty());
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedList() {
    when(gameTileController.getRoot()).thenReturn(new Pane());
    ObservableList<GameBean> observableList = FXCollections.observableArrayList();
    observableList.add(new GameBean());

    runOnFxThreadAndWait(() -> instance.createTiledFlowPane(observableList));
    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPostInstantiatedGameInfoBean() {
    doAnswer(invocation -> new Pane()).when(gameTileController).getRoot();
    ObservableList<GameBean> observableList = FXCollections.observableArrayList();

    runOnFxThreadAndWait(() -> {
      instance.createTiledFlowPane(observableList);
      observableList.add(new GameBean());
    });
    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedListAndPostInstantiatedGameInfoBean() {
    ObservableList<Node> children = instance.tiledFlowPane.getChildren();
    doAnswer(invocation -> new Pane()).when(gameTileController).getRoot();

    ObservableList<GameBean> observableList = FXCollections.observableArrayList();
    runOnFxThreadAndWait(() -> {
      observableList.add(GameBeanBuilder.create().defaultValues().id(1).get());
      instance.createTiledFlowPane(observableList);
      observableList.add(GameBeanBuilder.create().defaultValues().id(2).get());
    });
    assertThat(children, hasSize(2));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), instanceOf(Node.class));
  }

  @Test
  public void testSorting() {
    ObservableList<GameBean> observableList = FXCollections.observableArrayList();
    GameBean game1 = GameBeanBuilder.create().defaultValues().get();
    GameBean game2 = GameBeanBuilder.create().defaultValues().title("xyz").get();

    game1.setTeams(Map.of(1, List.of(1, 2)));
    game1.setId(234);
    game2.setId(123);
    game2.setTeams(Map.of(1, List.of(3)));


    observableList.addAll(game1, game2);
    instance.setSortingOrder(TilesSortingOrder.PLAYER_ASC);

    runOnFxThreadAndWait(() -> instance.createTiledFlowPane(observableList));
    assertEquals(game2, instance.tiledFlowPane.getChildren().getFirst().getUserData());
  }
}
