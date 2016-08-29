package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static com.faforever.client.game.GameStatus.HOST;
import static com.faforever.client.game.GameStatus.LOBBY;
import static com.faforever.client.game.GameStatus.NONE;
import static com.faforever.client.game.GameStatus.PLAYING;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class FilterUserControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  ChatUserItemController chatUserItemController;

  @Mock
  Player player;

  @Mock
  ChannelTabController channelTabController;

  @Mock
  I18n i18n;

  private FilterUserController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("filter_user.fxml");
    instance.channelTabController = loadController("channel_tab.fxml");
    instance.i18n = i18n;

    when(chatUserItemController.getPlayer()).thenReturn(player);
  }

  @Test
  public void setChannelTabControllerTest() throws Exception {
    instance.setChannelController(channelTabController);
    assertEquals(channelTabController, instance.channelTabController);
  }

  @Test
  public void testIsInClan() throws Exception {
    String testClan = "testClan";
    when(player.getClan()).thenReturn(testClan);
    instance.clanFilterField.setText(testClan);

    assertTrue(instance.isInClan(chatUserItemController));
  }

  @Test
  public void testIsBoundedByRatingWithinBounds() throws Exception {
    when(player.getGlobalRatingMean()).thenReturn((float) 500);
    when(player.getGlobalRatingDeviation()).thenReturn((float) 0);
    instance.minRatingFilterField.setText("300");
    instance.maxRatingFilterField.setText("700");

    assertTrue(instance.isBoundedByRating(chatUserItemController));
  }

  @Test
  public void testIsBoundedByRatingNotWithinBounds() throws Exception {
    when(player.getGlobalRatingMean()).thenReturn((float) 500);
    when(player.getGlobalRatingDeviation()).thenReturn((float) 0);
    instance.minRatingFilterField.setText("600");
    instance.maxRatingFilterField.setText("300");

    assertFalse(instance.isBoundedByRating(chatUserItemController));
  }

  @Test
  public void testIsGameStatusMatchPlaying() throws Exception {
    when(player.getGameStatus()).thenReturn(PLAYING);
    instance.gameStatusFilter = PLAYING;

    assertTrue(instance.isGameStatusMatch(chatUserItemController));
  }

  @Test
  public void testIsGameStatusMatchLobby() throws Exception {
    when(player.getGameStatus()).thenReturn(HOST);
    instance.gameStatusFilter = HOST;

    assertTrue(instance.isGameStatusMatch(chatUserItemController));

    when(player.getGameStatus()).thenReturn(LOBBY);
    instance.gameStatusFilter = LOBBY;

    assertTrue(instance.isGameStatusMatch(chatUserItemController));
  }

  @Test
  public void testOnGameStatusPlaying() throws Exception {
    when(channelTabController.getUserToChatUserControls()).thenReturn(new HashMap<>());
    when(i18n.get("chat.filter.gameStatus.playing")).thenReturn("playing");

    instance.onGameStatusPlaying(null);
    assertEquals(PLAYING, instance.gameStatusFilter);
    assertEquals(i18n.get("chat.filter.gameStatus.playing"), instance.gameStatusMenu.getText());
  }

  @Test
  public void testOnGameStatusLobby() throws Exception {
    when(channelTabController.getUserToChatUserControls()).thenReturn(new HashMap<>());
    when(i18n.get("chat.filter.gameStatus.lobby")).thenReturn("lobby");

    instance.onGameStatusLobby(null);
    assertEquals(LOBBY, instance.gameStatusFilter);
    assertEquals(i18n.get("chat.filter.gameStatus.lobby"), instance.gameStatusMenu.getText());
  }

  @Test
  public void testOnGameStatusNone() throws Exception {
    when(channelTabController.getUserToChatUserControls()).thenReturn(new HashMap<>());
    when(i18n.get("chat.filter.gameStatus.none")).thenReturn("none");

    instance.onGameStatusNone(null);
    assertEquals(NONE, instance.gameStatusFilter);
    assertEquals(i18n.get("chat.filter.gameStatus.none"), instance.gameStatusMenu.getText());
  }

  @Test
  public void testOnClearGameStatus() throws Exception {
    when(channelTabController.getUserToChatUserControls()).thenReturn(new HashMap<>());
    when(i18n.get("chat.filter.gameStatus")).thenReturn("gameStatus");

    instance.onClearGameStatus(null);
    assertEquals(null, instance.gameStatusFilter);
    assertEquals(i18n.get("chat.filter.gameStatus"), instance.gameStatusMenu.getText());
  }
}
