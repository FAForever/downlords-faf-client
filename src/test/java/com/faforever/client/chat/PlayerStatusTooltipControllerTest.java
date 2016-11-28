package com.faforever.client.chat;

import com.faforever.client.game.Game;
import com.faforever.client.game.GameTooltipController;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class PlayerStatusTooltipControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private MapService mapService;
  @Mock
  private UiService uiService;
  @Mock
  private GameTooltipController gameTooltipController;

  private GameStatusTooltipController instance;

  @Before
  public void setUp() throws Exception {
    instance = new GameStatusTooltipController();
    instance.uiService = uiService;

    when(uiService.loadFxml("theme/game_tooltip.fxml")).thenReturn(gameTooltipController);
    when(gameTooltipController.getRoot()).thenReturn(new Pane());

    loadFxml("theme/play/game_status_tooltip.fxml", clazz -> instance);
  }

  @Test
  public void testSetGameInfoBean() {
    Game game = new Game();
    game.setMapFolderName("testMap");

    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.SMALL)).thenReturn(null);
    when(mapService.findMapByName(game.getMapFolderName())).thenReturn(null);

/*    boolean containsGameTooltipControllerInstance = false;
    for(Node node: ((Pane) instance.getRoot()).getChildren()) {
      if(node instanceof GameStatusTooltipController.class) {
        containsGameTooltipControllerInstance = true;
        break;
      }
    }*/
  }
}
