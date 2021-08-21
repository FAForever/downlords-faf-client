package com.faforever.client.game;

import com.faforever.client.builders.FeaturedModBeanBuilder;
import com.faforever.client.builders.GameBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.GameBean;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.mod.ModService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.UITest;
import com.faforever.client.theme.UiService;
import com.faforever.client.vault.replay.WatchButtonController;
import com.faforever.commons.lobby.GameStatus;
import javafx.scene.control.Button;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameDetailControllerTest extends UITest {

  @Mock
  private I18n i18n;
  @Mock
  private MapService mapService;
  @Mock
  private ModService modService;
  @Mock
  private UiService uiService;
  @Mock
  private PlayerService playerService;
  @Mock
  private JoinGameHelper joinGameHelper;

  @Mock
  private WatchButtonController watchButtonController;

  private GameDetailController instance;
  private GameBean game;


  @BeforeEach
  public void setUp() throws Exception {
    game = GameBeanBuilder.create().defaultValues().get();
    when(watchButtonController.getRoot()).thenReturn(new Button());
    when(modService.getFeaturedMod(game.getFeaturedMod())).thenReturn(CompletableFuture.completedFuture(FeaturedModBeanBuilder.create().defaultValues().get()));
    when(mapService.loadPreview(game.getMapFolderName(), PreviewSize.LARGE)).thenReturn(null);
    when(i18n.get("game.detail.players.format", game.getNumPlayers(), game.getMaxPlayers())).thenReturn(String.format("%d/%d", game.getNumPlayers(), game.getMaxPlayers()));

    instance = new GameDetailController(i18n, mapService, modService, playerService, uiService, joinGameHelper);
    loadFxml("theme/play/game_detail.fxml", clazz -> {
      if (clazz == WatchButtonController.class) {
        return watchButtonController;
      }
      return instance;
    });
    runOnFxThreadAndWait(() -> instance.setGame(game));
  }

  @Test
  public void testSetGame() {
    assertTrue(instance.gameTitleLabel.isVisible());
    assertTrue(instance.hostLabel.isVisible());
    assertTrue(instance.numberOfPlayersLabel.isVisible());
    assertTrue(instance.mapImageView.isVisible());
    assertTrue(instance.gameTypeLabel.isVisible());
    assertTrue(instance.joinButton.isVisible());
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().size());

    runOnFxThreadAndWait(() -> instance.setGame(null));
    assertFalse(instance.gameTitleLabel.isVisible());
    assertFalse(instance.hostLabel.isVisible());
    assertFalse(instance.numberOfPlayersLabel.isVisible());
    assertFalse(instance.mapImageView.isVisible());
    assertFalse(instance.gameTypeLabel.isVisible());
    assertFalse(instance.joinButton.isVisible());
    assertEquals(0, instance.teamListPane.getChildren().size());
  }

  @Test
  public void testGameStatusListener() {
    assertFalse(instance.watchButton.isVisible());
    assertTrue(instance.joinButton.isVisible());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.PLAYING));
    assertTrue(instance.watchButton.isVisible());
    assertFalse(instance.joinButton.isVisible());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.CLOSED));
    assertFalse(instance.watchButton.isVisible());
    assertFalse(instance.joinButton.isVisible());
    runOnFxThreadAndWait(() -> game.setStatus(GameStatus.UNKNOWN));
    assertFalse(instance.watchButton.isVisible());
    assertFalse(instance.joinButton.isVisible());
  }

  @Test
  public void testGamePropertyListener() {
    assertEquals(game.getTitle(), instance.gameTitleLabel.getText());
    assertEquals(game.getHost(), instance.hostLabel.getText());
    assertEquals(game.getMapFolderName(), instance.mapLabel.getText());
    game.setTitle("blah");
    game.setHost("me");
    game.setMapFolderName("lala");
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(game.getTitle(), instance.gameTitleLabel.getText());
    assertEquals(game.getHost(), instance.hostLabel.getText());
    assertEquals(game.getMapFolderName(), instance.mapLabel.getText());
  }

  @Test
  public void testNumPlayersListener() {
    assertEquals(String.format("%d/%d", game.getNumPlayers(), game.getMaxPlayers()), instance.numberOfPlayersLabel.getText());
    when(i18n.get("game.detail.players.format", 12, 16)).thenReturn("12/16");
    game.setNumPlayers(12);
    game.setMaxPlayers(16);
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(String.format("%d/%d", game.getNumPlayers(), game.getMaxPlayers()), instance.numberOfPlayersLabel.getText());
  }

  @Test
  public void testModListener() {
    assertEquals("Forged Alliance Forever", instance.gameTypeLabel.getText());
    FeaturedModBean mod = FeaturedModBeanBuilder.create().defaultValues().technicalName("ladder").displayName("LADDER").get();
    when(modService.getFeaturedMod(mod.getTechnicalName())).thenReturn(CompletableFuture.completedFuture(mod));
    runOnFxThreadAndWait(() -> game.setFeaturedMod(mod.getTechnicalName()));
    assertEquals(mod.getDisplayName(), instance.gameTypeLabel.getText());
  }

  @Test
  public void testTeamListener() {
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().size());
    runOnFxThreadAndWait(() -> game.getTeams().putAll(Map.of("1", List.of("Me"), "2", List.of("You"))));
    assertEquals(game.getTeams().size(), instance.teamListPane.getChildren().size());
  }

  @Test
  public void testGetRoot() {
    assertEquals(instance.gameDetailRoot, instance.getRoot());
  }

  @Test
  public void testJoinGame() {
    instance.onJoinButtonClicked(null);
    verify(joinGameHelper).join(game);
  }
}
