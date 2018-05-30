package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.player.Player;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static com.faforever.client.game.PlayerStatus.HOSTING;
import static com.faforever.client.game.PlayerStatus.IDLE;
import static com.faforever.client.game.PlayerStatus.LOBBYING;
import static com.faforever.client.game.PlayerStatus.PLAYING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class UserFilterControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  ChatUserItemController chatUserItemController;
  @Mock
  Player player;
  @Mock
  ChannelTabController channelTabController;
  @Mock
  I18n i18n;

  private UserFilterController instance;

  @Before
  public void setUp() throws Exception {
    instance = new UserFilterController(i18n);
    // TODO convert to constructor parameter
    instance.channelTabController = channelTabController;

    when(chatUserItemController.getChatUser()).thenReturn(player);

    loadFxml("theme/chat/user_filter.fxml", clazz -> instance);
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

    assertTrue(instance.isBoundByRating(chatUserItemController));
  }

  @Test
  public void testIsBoundedByRatingNotWithinBounds() throws Exception {
    when(player.getGlobalRatingMean()).thenReturn((float) 500);
    when(player.getGlobalRatingDeviation()).thenReturn((float) 0);
    instance.minRatingFilterField.setText("600");
    instance.maxRatingFilterField.setText("300");

    assertFalse(instance.isBoundByRating(chatUserItemController));
  }

  @Test
  public void testIsGameStatusMatchPlaying() throws Exception {
    when(player.getStatus()).thenReturn(PLAYING);
    instance.playerStatusFilter = PLAYING;

    assertTrue(instance.isGameStatusMatch(chatUserItemController));
  }

  @Test
  public void testIsGameStatusMatchLobby() throws Exception {
    when(player.getStatus()).thenReturn(HOSTING);
    instance.playerStatusFilter = HOSTING;

    assertTrue(instance.isGameStatusMatch(chatUserItemController));

    when(player.getStatus()).thenReturn(LOBBYING);
    instance.playerStatusFilter = LOBBYING;

    assertTrue(instance.isGameStatusMatch(chatUserItemController));
  }

  @Test
  public void testOnGameStatusPlaying() throws Exception {
    when(channelTabController.getUserToChatUserControls()).thenReturn(new HashMap<>());
    when(i18n.get("game.gameStatus.playing")).thenReturn("playing");

    instance.onGameStatusPlaying(null);
    assertEquals(PLAYING, instance.playerStatusFilter);
    assertEquals(i18n.get("game.gameStatus.playing"), instance.gameStatusMenu.getText());
  }

  @Test
  public void testOnGameStatusLobby() throws Exception {
    when(channelTabController.getUserToChatUserControls()).thenReturn(new HashMap<>());
    when(i18n.get("game.gameStatus.lobby")).thenReturn("lobby");

    instance.onGameStatusLobby(null);
    assertEquals(LOBBYING, instance.playerStatusFilter);
    assertEquals(i18n.get("game.gameStatus.lobby"), instance.gameStatusMenu.getText());
  }

  @Test
  public void testOnGameStatusNone() throws Exception {
    when(channelTabController.getUserToChatUserControls()).thenReturn(new HashMap<>());
    when(i18n.get("game.gameStatus.none")).thenReturn("none");

    instance.onGameStatusNone(null);
    assertEquals(IDLE, instance.playerStatusFilter);
    assertEquals(i18n.get("game.gameStatus.none"), instance.gameStatusMenu.getText());
  }
}
