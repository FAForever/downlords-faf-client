package com.faforever.client.game;

import com.faforever.client.fx.MouseEvents;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import javafx.collections.FXCollections;
import javafx.scene.input.MouseButton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private PlayerService playerService;

  private Game game;

  @Mock
  private Consumer<Game> onSelectedConsumer;

  @BeforeEach
  public void setUp() throws Exception {
    game = GameBuilder.create().defaultValues().get();

    when(i18n.get(anyString())).thenReturn("test");
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(
        FeaturedModBeanBuilder.create().defaultValues().get()
    ));

    instance = new GameTileController(mapService, i18n, joinGameHelper, modService, playerService);
    loadFxml("theme/play/game_card.fxml", clazz -> instance);
    instance.setOnSelectedListener(onSelectedConsumer);
  }

  @Test
  public void testOnLeftDoubleClick() {
    instance.setGame(game);
    instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 2));
    verify(joinGameHelper).join(any());
    verify(onSelectedConsumer).accept(game);
  }

  @Test
  public void testOnLeftSingleClick() {
    instance.setGame(game);
    instance.onClick(MouseEvents.generateClick(MouseButton.PRIMARY, 1));
    verify(joinGameHelper, never()).join(any());
    verify(onSelectedConsumer).accept(game);
  }

  @Test
  public void testSimModeLabel4Mods() {
    HashMap<String, String> simMods = new HashMap<>();
    simMods.put("test1", "test1");
    simMods.put("test2", "test2");
    simMods.put("test3", "test3");
    simMods.put("test4", "test4");
    game.setSimMods(FXCollections.observableMap(simMods));
    instance.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    verify(i18n).get(eq("game.mods.twoAndMore"), contains("test"), eq(3));
  }

  @Test
  public void testSimModeLabel2Mods() {
    HashMap<String, String> simMods = new HashMap<>();
    simMods.put("test1", "test1");
    simMods.put("test2", "test2");
    game.setSimMods(FXCollections.observableMap(simMods));
    instance.setGame(game);
    WaitForAsyncUtils.waitForFxEvents();

    verify(i18n).get("textSeparator");
  }

  @Test
  public void testFriendInGameHighlighting() {
    when(playerService.areFriendsInGame(game)).thenReturn(true);

    runOnFxThreadAndWait(() -> instance.setGame(game));

    assertTrue(instance.getRoot().getPseudoClassStates().contains(GameTileController.FRIEND_IN_GAME_PSEUDO_CLASS));
  }
}
