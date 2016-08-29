package com.faforever.client.game;

import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameTileControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private ModService modService;
  private GameTileController instance;
  @Mock
  private JoinGameHelper joinGameHelper;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;

  @Mock
  private GameTooltipController gameTooltipController;
  private Game game;

  @Mock
  private Consumer<Game> onSelectedConsumer;

  @Before
  public void setUp() throws Exception {
    instance = loadController("game_tile.fxml");
    instance.modService = modService;
    instance.applicationContext = applicationContext;
    instance.i18n = i18n;
    instance.mapService = mapService;
    instance.joinGameHelper = joinGameHelper;

    game = GameInfoBeanBuilder.create().defaultValues().get();

    when(applicationContext.getBean(GameTooltipController.class)).thenReturn(gameTooltipController);
    when(gameTooltipController.getRoot()).thenReturn(new Pane());
    when(i18n.get(anyString())).thenReturn("test");
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(
        FeaturedModBeanBuilder.create().defaultValues().get()
    ));

    instance.initialize();
    instance.postConstruct();

    instance.setOnSelectedListener(onSelectedConsumer);
    instance.setGame(game);
  }

  @Test
  public void testOnLeftDoubleClick() {
    instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 2));
    verify(joinGameHelper).join(any());
    verify(onSelectedConsumer).accept(game);
  }

  @Test
  public void testOnLeftSingleClick() {
    instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    verify(joinGameHelper, never()).join(any());
    verify(onSelectedConsumer).accept(game);
  }
}
