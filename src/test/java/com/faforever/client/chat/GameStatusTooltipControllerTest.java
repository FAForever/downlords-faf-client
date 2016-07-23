package com.faforever.client.chat;

import com.faforever.client.game.GameInfoBean;
import com.faforever.client.map.MapService;
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
    GameInfoBean gameInfoBean = new GameInfoBean();
    gameInfoBean.setMapTechnicalName("testMap");

    when(mapService.loadSmallPreview(gameInfoBean.getMapTechnicalName())).thenReturn(null);
    when(mapService.findMapByName(gameInfoBean.getMapTechnicalName())).thenReturn(null);

/*    boolean containsGameTooltipControllerInstance = false;
    for(Node node: ((Pane) instance.getRoot()).getChildren()) {
      if(node instanceof GameStatusTooltipController.class) {
        containsGameTooltipControllerInstance = true;
        break;
      }
    }*/
  }
}
