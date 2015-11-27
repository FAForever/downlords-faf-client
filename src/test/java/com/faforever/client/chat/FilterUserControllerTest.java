package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static com.faforever.client.legacy.GameStatus.HOST;
import static com.faforever.client.legacy.GameStatus.LOBBY;
import static com.faforever.client.legacy.GameStatus.NONE;
import static com.faforever.client.legacy.GameStatus.PLAYING;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class FilterUserControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  ChatUserControl chatUserControl;

  @Mock
  PlayerInfoBean playerInfoBean;

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

    when(chatUserControl.getPlayerInfoBean()).thenReturn(playerInfoBean);
  }

  @Test
  public void setChannelTabControllerTest() throws Exception {
    instance.setChannelController(channelTabController);
    assertEquals(channelTabController, instance.channelTabController);
  }

  @Test
  public void testIsInClan() throws Exception {
    String testClan = "testClan";
    when(playerInfoBean.getClan()).thenReturn(testClan);
    instance.clanFilterField.setText(testClan);

    assertTrue(instance.isInClan(chatUserControl));
  }

  @Test
  public void testIsBoundedByRatingWithinBounds() throws Exception {
    when(playerInfoBean.getGlobalRatingMean()).thenReturn((float) 500);
    when(playerInfoBean.getGlobalRatingDeviation()).thenReturn((float) 0);
    instance.minRatingFilterField.setText("300");
    instance.maxRatingFilterField.setText("700");

    assertTrue(instance.isBoundedByRating(chatUserControl));
  }

  @Test
  public void testIsBoundedByRatingNotWithinBounds() throws Exception {
    when(playerInfoBean.getGlobalRatingMean()).thenReturn((float) 500);
    when(playerInfoBean.getGlobalRatingDeviation()).thenReturn((float) 0);
    instance.minRatingFilterField.setText("600");
    instance.maxRatingFilterField.setText("300");

    assertFalse(instance.isBoundedByRating(chatUserControl));
  }

  @Test
  public void testIsGameStatusMatchPlaying() throws Exception {
    when(playerInfoBean.getGameStatus()).thenReturn(PLAYING);
    instance.gameStatusFilter = PLAYING;

    assertTrue(instance.isGameStatusMatch(chatUserControl));
  }

  @Test
  public void testIsGameStatusMatchLobby() throws Exception {
    when(playerInfoBean.getGameStatus()).thenReturn(HOST);
    instance.gameStatusFilter = HOST;

    assertTrue(instance.isGameStatusMatch(chatUserControl));

    when(playerInfoBean.getGameStatus()).thenReturn(LOBBY);
    instance.gameStatusFilter = LOBBY;

    assertTrue(instance.isGameStatusMatch(chatUserControl));
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
