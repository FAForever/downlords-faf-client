package com.faforever.client.chat;

import com.faforever.client.game.Game;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapServiceImpl.PreviewSize;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class GameStatusTooltipControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  MapService mapService;

  private GameStatusTooltipController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("game_status_tooltip.fxml");

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
