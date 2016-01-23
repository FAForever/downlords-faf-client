package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.legacy.domain.PlayersMessage;
import com.faforever.client.legacy.domain.SocialMessage;
import com.faforever.client.remote.FafService;
import com.faforever.client.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

import static com.faforever.client.chat.SocialStatus.FOE;
import static com.faforever.client.chat.SocialStatus.FRIEND;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PlayerServiceImplTest {

  @Mock
  GameService gameService;
  @Mock
  FafService fafService;
  @Mock
  UserService userService;

  private PlayerServiceImpl instance;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new PlayerServiceImpl();
    instance.fafService = fafService;
    instance.userService = userService;
    instance.gameService = gameService;
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInit() throws Exception {
    instance.init();

    verify(fafService).addOnMessageListener(eq(PlayersMessage.class), any(Consumer.class));
    verify(fafService).addOnMessageListener(eq(SocialMessage.class), any(Consumer.class));
  }

  @Test
  public void testGetPlayerForUsernameUsernameDoesNotExist() throws Exception {
    PlayerInfoBean playerInfoBean = instance.getPlayerForUsername("junit");
    assertNull(playerInfoBean);
  }

  @Test
  public void testGetPlayerForUsernameUsernameExists() throws Exception {
    instance.registerAndGetPlayerForUsername("junit");

    PlayerInfoBean playerInfoBean = instance.getPlayerForUsername("junit");

    assertNotNull(playerInfoBean);
    assertEquals("junit", playerInfoBean.getUsername());
  }

  @Test
  public void testGetPlayerForUsernameNull() throws Exception {
    PlayerInfoBean playerInfoBean = instance.getPlayerForUsername(null);
    assertNull(playerInfoBean);
  }

  @Test
  public void testRegisterAndGetPlayerForUsernameDoesNotExist() throws Exception {
    PlayerInfoBean playerInfoBean = instance.registerAndGetPlayerForUsername("junit");

    assertNotNull(playerInfoBean);
    assertEquals("junit", playerInfoBean.getUsername());
  }

  @Test(expected = NullPointerException.class)
  public void testRegisterAndGetPlayerForUsernameNull() throws Exception {
    instance.registerAndGetPlayerForUsername(null);
  }

  @Test
  public void testGetPlayerNamesEmpty() throws Exception {
    Set<String> playerNames = instance.getPlayerNames();
    assertThat(playerNames, empty());
  }

  @Test
  public void testGetPlayerNamesSomeInstances() throws Exception {
    instance.registerAndGetPlayerForUsername("player1");
    instance.registerAndGetPlayerForUsername("player2");

    Set<String> playerNames = instance.getPlayerNames();

    assertThat(playerNames, hasSize(2));
    assertThat(playerNames, containsInAnyOrder("player1", "player2"));
  }

  @Test
  public void testAddFriend() throws Exception {
    PlayerInfoBean lisa = instance.registerAndGetPlayerForUsername("lisa");
    PlayerInfoBean ashley = instance.registerAndGetPlayerForUsername("ashley");

    instance.addFriend("lisa");
    instance.addFriend("ashley");

    verify(fafService, times(2)).setFriends(eq(Arrays.asList("lisa", "ashley")));
    assertTrue("Property 'friend' was not set to true", lisa.getSocialStatus() == FRIEND);
    assertTrue("Property 'friend' was not set to true", ashley.getSocialStatus() == FRIEND);
  }

  @Test
  public void testAddFriendIsFoe() throws Exception {
    PlayerInfoBean playerInfoBean = instance.registerAndGetPlayerForUsername("player");
    playerInfoBean.setSocialStatus(FOE);

    instance.addFriend("player");

    assertFalse("Property 'foe' is still true", playerInfoBean.getSocialStatus() == FOE);
  }

  @Test
  public void testRemoveFriend() throws Exception {
    PlayerInfoBean player1 = instance.registerAndGetPlayerForUsername("player1");
    PlayerInfoBean player2 = instance.registerAndGetPlayerForUsername("player2");

    instance.addFriend("player1");
    verify(fafService).setFriends(eq(singletonList("player1")));

    instance.addFriend("player2");
    verify(fafService, times(2)).setFriends(eq(Arrays.asList("player1", "player2")));

    instance.removeFriend("player1");
    verify(fafService, times(3)).setFriends(eq(singletonList("player2")));

    assertFalse("Property 'friend' was not set to false", player1.getSocialStatus() == FRIEND);
    assertTrue("Property 'friend' was not set to true", player2.getSocialStatus() == FRIEND);
  }

  @Test
  public void testAddFoe() throws Exception {
    PlayerInfoBean player1 = instance.registerAndGetPlayerForUsername("player1");
    PlayerInfoBean player2 = instance.registerAndGetPlayerForUsername("player2");

    instance.addFoe("player1");
    instance.addFoe("player2");

    verify(fafService, times(2)).setFoes(Arrays.asList("player1", "player2"));
    assertTrue("Property 'foe' was not set to true", player1.getSocialStatus() == FOE);
    assertTrue("Property 'foe' was not set to true", player2.getSocialStatus() == FOE);
  }

  @Test
  public void testAddFoeIsFriend() throws Exception {
    PlayerInfoBean playerInfoBean = instance.registerAndGetPlayerForUsername("player");
    playerInfoBean.setSocialStatus(FRIEND);

    instance.addFoe("player");

    assertFalse("Property 'friend' is still true", playerInfoBean.getSocialStatus() == FRIEND);
  }

  @Test
  public void testRemoveFoe() throws Exception {
    PlayerInfoBean player = instance.registerAndGetPlayerForUsername("player");

    instance.addFriend("player");
    instance.removeFriend("player");

    assertFalse("Property 'friend' was not set to false", player.getSocialStatus() == FRIEND);
  }

  @Test
  public void testOnPlayerInfo() throws Exception {

  }

  @Test
  public void testOnFoeList() throws Exception {

  }

  @Test
  public void testOnFriendList() throws Exception {

  }

  @Test
  public void testGetCurrentPlayer() throws Exception {

  }
}
