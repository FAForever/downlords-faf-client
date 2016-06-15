package com.faforever.client.game;

import com.faforever.client.fx.MouseEvents;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.input.MouseButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class GameTileControllerTest extends AbstractPlainJavaFxTest {

  private GameTileController instance;

  @Mock
  private JoinGameHelper joinGameHelper;

  @Before
  public void setUp() throws Exception {
    instance = loadController("game_tile.fxml");
  }

  @Test
  public void testOnLeftDoubleClick() {
    instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 2));
    verify(joinGameHelper).join(any());
  }

  @Test
  public void testOnLeftSingleClick() {
    instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    verify(joinGameHelper, never()).join(any());
  }
}
