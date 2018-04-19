package com.faforever.client.game;

import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import javafx.scene.input.MouseButton;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

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
  private UiService uiService;
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
    instance = new GameTileController(mapService, i18n, joinGameHelper, modService, uiService);

    game = GameBuilder.create().defaultValues().get();

    when(i18n.get(anyString())).thenReturn("test");
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(
        FeaturedModBeanBuilder.create().defaultValues().get()
    ));

    loadFxml("theme/play/game_card.fxml", clazz -> instance);

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
